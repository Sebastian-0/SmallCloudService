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

public class Database {
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	private final Map<String, Node> wordToNode = new LinkedHashMap<>();

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

	public List<String> getSynonyms(String word) {
		readLock.lock();
		try {
			List<String> synonyms = ImmutableList.of();
			Node node = wordToNode.get(word);
			if (node != null) {
				synonyms = node.find().members;
			}

			List<String> result = new ArrayList<>(synonyms);
			result.remove(word);
			return result;
		} finally {
			readLock.unlock();
		}
	}

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
}
