package cloudservice;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Sets;

import java.util.LinkedHashMap;
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

	public Set<String> getSynonyms(String word) {
		readLock.lock();
		try {
			Node node = wordToNode.get(word);
			if (node == null) {
				return ImmutableSet.of();
			}
			return Sets.difference(node.find().members, ImmutableSet.of(word));
		} finally {
			readLock.unlock();
		}
	}

	private static class Node {
		private Node parent;
		private int rank;

		private Set<String> members;

		public Node(String text) {
			parent = this;
			rank = 1;
			members = ImmutableSet.of(text);
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
			Builder<String> builder = new Builder<>();
			builder.addAll(target.members);
			builder.addAll(source.members);
			target.members = builder.build();
			source.members = ImmutableSet.of();
		}
	}
}
