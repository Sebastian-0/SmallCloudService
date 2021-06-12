package cloudservice;

import cloudservice.Database.Entry;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is responsible for keeping the cluster in sync. When our data is modified we distribute the changes to
 * other nodes. If that fails we put the changes in a list to retry later.
 *
 * Every ten seconds we do retries on previously failed requests, as well as sending over all data to new nodes (marked
 * as needing a synchronization).
 *
 * There are some limitations with the current implementation:
 * <ul>
 *     <li>This class is only created & started when it's first referenced (not really a problem but I imagine it can
 *     cause confusion)</li>
 *     <li>Request timeouts might cause delays in incoming requests (not verified)</li>
 * </ul>
 */
public class Synchronizer {
	// TODO Problems with the current solution:
	//  - Missing tests
	//  - No security around the define cluster endpoint, you can easily hack the system
	//  - Multi-node systems don't really make sense when we don't even persist the data
	//
	// Request to define cluster:
	// curl -X POST -d "[\"localhost:8080\", \"localhost:8081\"]" -H "Content-type: application/json" "localhost:8080/api/cluster?thisInstance=localhost:8080"

	private final Logger LOGGER = LoggerFactory.getLogger(Synchronizer.class);

	private final Cluster cluster;
	private final Database database;

	private final LinkedBlockingQueue<FailedRequest> failedRequests;
	private final Client client;

	@Inject
	public Synchronizer(Cluster cluster, Database database) {
		this.cluster = cluster;
		this.database = database;
		failedRequests = new LinkedBlockingQueue<>();
		client = ClientBuilder.newBuilder().register(JsonIO.class).build();
		new Thread(new Worker()).start();
	}

	/**
	 * Distribute incoming requests to the cluster.
	 */
	public void sendToOthers(String word, Set<String> synonyms) {
		Set<String> others = cluster.getOtherInstances();
		others.removeIf(host -> sendTo(host, word, synonyms));
		if (!others.isEmpty()) {
			failedRequests.add(new FailedRequest(others, word, synonyms));
		}
	}

	private boolean sendTo(String host, String word, Set<String> synonyms) {
		try (Response response = client.target("http://" + host)
				.path("/api/synonyms")
				.queryParam("word", word)
				.queryParam("distribute", false)
				.request()
				.post(Entity.json(synonyms))) {
			LOGGER.info("status " + response.getStatus());
			return response.getStatusInfo().getFamily() == Family.SUCCESSFUL;
		} catch (ProcessingException e) {
			LOGGER.warn("Failed to synchronize synonyms for {} to {}", word, host, e);
			return false;
		}
	}

	private class Worker implements Runnable {
		private final Logger LOGGER = LoggerFactory.getLogger(Worker.class);

		@Override
		public void run() {
			while (true) {
				synchronizeNewInstances();
				retryFailedRequests();

				try {
					Thread.sleep(10_000);
				} catch (InterruptedException ignored) {
					LOGGER.info("Terminating...");
					break;
				}
			}
		}

		private void synchronizeNewInstances() {
			Set<String> instances = cluster.getInstancesNeedingSynchronization();
			if (!instances.isEmpty()) {
				List<Entry> allEntries = database.getAllEntries();
				for (String instance : instances) {
					if (sendAllTo(instance, allEntries)) {
						cluster.markSynchronized(instance);
					}
				}
			}
		}

		private boolean sendAllTo(String host, List<Entry> entries) {
			try (Response response = client.target("http://" + host)
					.path("/api/synchronization")
					.request()
					.post(Entity.json(entries))) {
				return response.getStatusInfo().getFamily() == Family.SUCCESSFUL;
			} catch (ProcessingException e) {
				LOGGER.warn("Failed to import all synonyms to {}", host, e);
				return false;
			}
		}

		private void retryFailedRequests() {
			List<FailedRequest> queue = new ArrayList<>();
			failedRequests.drainTo(queue);
			Set<String> others = cluster.getOtherInstances();

			queue.removeIf(failedRequest -> {
				Set<String> targets = new LinkedHashSet<>(others);
				targets.retainAll(failedRequest.targets);
				targets.removeIf(host -> sendTo(host, failedRequest.word, failedRequest.synonyms));
				return targets.isEmpty();
			});
			failedRequests.addAll(queue); // Re-add all requests we still haven't managed to sync
		}
	}


	private static class FailedRequest {
		public Set<String> targets;
		public String word;
		public Set<String> synonyms;

		public FailedRequest(Set<String> targets, String word, Set<String> synonyms) {
			this.targets = targets;
			this.word = word;
			this.synonyms = synonyms;
		}
	}
}
