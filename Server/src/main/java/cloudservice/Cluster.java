package cloudservice;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Keeps track of which hosts are in the cluster, and which of them needs to get a full synchronization (necessary
 * when new nodes are included).
 */
public class Cluster {
	private final Map<String, Instance> instances = new LinkedHashMap<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	public void setAllInstances(String thisInstance, Set<String> allInstances) {
		writeLock.lock();
		try {
			boolean isInitialDefinition = instances.isEmpty();
			instances.keySet().retainAll(allInstances);
			for (String host : allInstances) {
				Instance instance = instances.computeIfAbsent(host, k -> new Instance(host));
				instance.isThisInstance = thisInstance.equals(host);
				if (isInitialDefinition) {
					// We don't want to sync our empty state, especially since others might import to us first, which would
					// then result in us sending full imports to everyone
					instance.needsFullSync = false;
				}
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Returns all hosts in the cluster.
	 */
	public Set<String> getAllInstances() {
		readLock.lock();
		try {
			return new LinkedHashSet<>(instances.keySet());
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Returns all other hosts in the cluster (not including us).
	 */
	public Set<String> getOtherInstances() {
		readLock.lock();
		try {
			Set<String> others = new LinkedHashSet<>();
			for (Instance instance : instances.values()) {
				if (!instance.isThisInstance) {
					others.add(instance.instance);
				}
			}
			return others;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Returns all instances in the cluster that needs to get a full synchronization, this will always exclude us.
	 */
	public Set<String> getInstancesNeedingSynchronization() {
		readLock.lock();
		try {
			Set<String> needsSync = new LinkedHashSet<>();
			for (Instance instance : instances.values()) {
				if (!instance.isThisInstance && instance.needsFullSync) {
					needsSync.add(instance.instance);
				}
			}
			return needsSync;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Mark the target as synchronized.
	 */
	public void markSynchronized(String target) {
		writeLock.lock();
		try {
			Instance instance = instances.get(target);
			if (instance != null) {
				instance.needsFullSync = false;
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * A cluster must be defined before we start accepting queries.
	 */
	public boolean isReadyForQueries() {
		readLock.lock();
		try {
			return !instances.isEmpty();
		} finally {
			readLock.unlock();
		}
	}

	private static class Instance {
		public String instance;
		public boolean needsFullSync;
		public boolean isThisInstance;

		public Instance(String instance) {
			this.instance = instance;
			this.needsFullSync = true;
		}
	}
}
