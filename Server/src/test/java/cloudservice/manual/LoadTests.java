package cloudservice.manual;

import cloudservice.JsonIO;
import com.google.common.collect.ImmutableList;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.LongSummaryStatistics;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * These tests are utilities, not normal unit tests.
 *
 * They assume there is a server running already on 8080 (default port).
 */
@Disabled("Comment out this when running load test locally")
public class LoadTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoadTests.class);

	private static final String HOST = "http://localhost:8080";

	private static Client client;

	@BeforeAll
	static void beforeAll() {
		client = ClientBuilder.newBuilder().register(JsonIO.class).build();
	}

	@AfterAll
	static void afterAll() {
		client.close();
	}

	/**
	 * Adds synonyms for the words 1...100000. Each number <code>x</code> will have a synonym to the numbers
	 * <ul>
	 *     <li><code>x + 1</code></li>
	 *     <li><code>100000 + x*2</code></li>
	 *     <li><code>100000 + x*2 + 1</code></li>
	 * </ul>
	 *
	 * Note that this test does not make much use of concurrency since writing is single threaded server-side.
	 */
	@Test
	void addManySynonyms() throws InterruptedException {
		final int nWorkers = 10;
		final int nSynonyms = 100_000;

		Consumer<Integer> workTask = idx -> {
			importSynonym(Integer.toString(idx),
					Integer.toString(idx + 1),
					Integer.toString(nSynonyms + idx * 2),
					Integer.toString(nSynonyms + idx * 2 + 1));
		};

		setupAndRun(nWorkers, nSynonyms, workTask);
	}

	private void importSynonym(String a, String... b) {
		try (Response response = client.target(HOST)
				.path("/api/synonyms")
				.queryParam("word", a)
				.request()
				.post(Entity.json(ImmutableList.copyOf(b)))) {
			if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
				LOGGER.error("Failed request: {} - {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
			}
		}
	}

	/**
	 * Generates queries for the words 1...100000.
	 *
	 * <b>Note:</b> This test assumes that the words exist, so run {@link #addManySynonyms()} first.
	 */
	@Test
	void getManySynonyms() throws InterruptedException {
		final int nWorkers = 10;
		final int nSynonyms = 100_000;

		Consumer<Integer> workTask = idx -> {
			getSynonyms(Integer.toString(idx));
		};

		setupAndRun(nWorkers, nSynonyms, workTask);
	}

	private void setupAndRun(int nWorkers, int nSynonyms, Consumer<Integer> workTask) throws InterruptedException {
		final int nPerWorker = nSynonyms / nWorkers;

		Thread[] threads = new Thread[nWorkers];
		for (int i = 0; i < nWorkers; i++) {
			int start = i * nPerWorker;
			int end = (i+1) * nPerWorker;
			if (i == nWorkers - 1) {
				end = nSynonyms;
			}
			threads[i] = new Thread(new Worker(start, end, workTask, i == 0));
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join();
		}
	}

	private void getSynonyms(String a) {
		try (Response response = client.target(HOST)
				.path("/api/synonyms")
				.queryParam("word", a)
				.request()
				.get()) {
			if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
				LOGGER.error("Failed request: {} - {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
			}
		}
	}


	private static class Worker implements Runnable {
		private final int startIndex;
		private final int endIndex;
		private final Consumer<Integer> workTask;

		private final boolean reportStatistics;
		private final Queue<Long> responseTimes = new ArrayDeque<>();

		private Worker(int startIndex, int endIndex, Consumer<Integer> workTask, boolean reportStatistics) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.workTask = workTask;
			this.reportStatistics = reportStatistics;
		}

		@Override
		public void run() {
			for (int i = startIndex; i < endIndex; i++) {
				long start = System.nanoTime();
				workTask.accept(i);
				long time = System.nanoTime() - start;
				responseTimes.offer(time);
				if (responseTimes.size() > 100) {
					responseTimes.poll();
				}

				if (reportStatistics && i > 0 && i % 1000 == 0) {
					LongSummaryStatistics statistics = responseTimes.stream().mapToLong(Long::longValue).summaryStatistics();
					LOGGER.warn("Avg. response time: {}ms", statistics.getAverage() / 1_000_000);
				}
			}
		}
	}
}
