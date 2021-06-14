package cloudservice;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class stores all the synonyms in memory. Updates are implemented using Union-Find to make them go faster since
 * we are only interested in adding synonyms. To support removals as well we would need to redesign.
 * <br/>
 * <br/>The main priority is to make queries fast, so it's important that we pre-compute as much as possible, and that we use
 * a good locking mechanism.
 * <br/>
 * <br/>To do some benchmarking I added 300k synonyms using 10 workers (see <code>LoadTest</code>}, and then ran 100k
 * modifications/queries as quickly as possible in three separate tests. In this test all synonyms are connected by
 * transitivity since that is the worst case we need to handle.
 * <ul>
 *     <li>When testing modifications, response time for adding synonyms went from 40ms to 330ms during the test (as
 *     more synonyms were added). Part of this time is merging the synonym lists in the database and part of it is
 *     contention for the lock since only 1 of 10 workers can have access at a time.</li>
 *     <li>When testing queries, response time for querying synonyms was consistently sub 0.3ms during the test (with
 *     300k synonyms in the database)</li>
 *     <li>When testing both modifications and queries at the same time, response time for querying synonyms was above
 *     100ms throughout the test, which isn't surprising since the modifications lock the database a lot of the time.</li>
 * </ul>
 * <br/>My conclusion is that this is good enough performance if this is what the final system should look like. The
 * worst case is when there is a heavy load with both queries and modifications at the same time but realistically I
 * think that scenario is unlikely.
 * <br/>
 * <br/>We could improve performance in the last test by having two versions of the database,
 * one live and one that is being modified, and then just swapping them when we are done, then I think we would have
 * vastly better response times for queries. We could also use binary search when merging the synonym lists to speed up
 * the modification queries as well.
 */
public class Database {
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	private final Map<String, Node> wordToNode = new LinkedHashMap<>();

	/**
	 * Add new synonyms, this method will lock the database from reads during the operation.
	 */
	public void addSynonyms(String word, Set<String> synonyms) {
		writeLock.lock();
		try {
			Node wordNode = getNode(word);
			for (String synonym : synonyms) {
				Node synonymNode = getNode(synonym);
				wordNode.union(synonymNode);
			}
		} finally {
			writeLock.unlock();
		}
	}

	private Node getNode(String word) {
		return wordToNode.computeIfAbsent(word, k -> new Node(word));
	}

	/**
	 * Returns the synonyms of the specified word, limited to at most the specified limit amount of results. The total
	 * count is also returned as part of the result.
	 */
	public SynonymPage getSynonyms(String word, int limit) {
		readLock.lock();
		try {
			Node node = wordToNode.get(word);
			if (node == null) {
				return new SynonymPage(0, ImmutableList.of());
			}
			List<String> allSynonyms = node.find().members;
			List<String> result = new ArrayList<>();
			for (String synonym : allSynonyms) {
				if (result.size() == limit) {
					break;
				}

				if (!synonym.equals(word)) {
					result.add(synonym);
				}
			}
			return new SynonymPage(allSynonyms.size() - 1, result); // Remove one since 'word' is included
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * This data structure uses Union-Find to merge groups of synonyms. The twist is that we also want to keep track
	 * of the complete set of synonyms included in the group to speed up queries.
	 */
	private static class Node {
		private static final Collator COLLATOR;

		private Node parent;
		private int rank;

		private List<String> members;

		static {
			COLLATOR = Collator.getInstance();
			COLLATOR.setStrength(Collator.PRIMARY);
		}

		public Node(String text) {
			parent = this;
			rank = 1;
			members = ImmutableList.of(text);
		}

		public Node find() {
			if (parent != this) {
				parent = parent.find();
			}
			return parent;
		}

		public void union(Node other) {
			Node n1 = find();
			Node n2 = other.find();

			if (n1 == n2) {
				return;
			}

			if (n1.rank > n2.rank) {
				merge(n2, n1);
			} else if (n2.rank > n1.rank) {
				merge(n1, n2);
			} else {
				merge(n2, n1);
				n1.rank += 1;
			}
		}

		private void merge(Node source, Node target) {
			source.parent = target;
			target.members = mergeLists(target.members, source.members);
			source.members = ImmutableList.of();
		}

		/**
		 * This is relatively fast but can take 200ms if you have 300k words that are synonyms (and you are unlucky with
		 * the input). Binary searching would most likely help for the most cases since we seldom merge two large lists.
		 */
		private ImmutableList<String> mergeLists(List<String> l1, List<String> l2) {
			Builder<String> builder = new Builder<>();

			int idx1 = 0;
			int idx2 = 0;
			while (true) {
				if (idx1 == l1.size()) {
					builder.addAll(l2.subList(idx2, l2.size()));
					break;
				}
				if (idx2 == l2.size()) {
					builder.addAll(l1.subList(idx1, l1.size()));
					break;
				}

				String head1 = l1.get(idx1);
				String head2 = l2.get(idx2);

				int c = COLLATOR.compare(head1, head2);
				if (c < 0) {
					builder.add(head1);
					idx1++;
				} else if (c > 0) {
					builder.add(head2);
					idx2++;
				} else {
					if (head1.equals(head2)) {
						throw new IllegalStateException("Multiple sets contain the same element!");
					}
					// Differed in capitalization, e.g. a vs. A
					builder.add(head1);
					builder.add(head2);
					idx1++;
					idx2++;
				}
			}
			return builder.build();
		}
	}

	public static class SynonymPage {
		public final int total;
		public final List<String> synonyms;

		public SynonymPage(int total, List<String> synonyms) {
			this.total = total;
			this.synonyms = synonyms;
		}
	}
}
