package cloudservice;

import cloudservice.Database.SynonymPage;
import cloudservice.util.JUnit5JerseyTest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SynonymApiTest extends JUnit5JerseyTest {
	@Override
	protected Application configure() {
		return new ApiResourceConfig();
	}

	@Override
	protected void configureClient(ClientConfig config) {
		config.register(JsonIO.class);
	}

	@Test
	void addMissingWordArgument() {
		defineCluster();

		Response response = addSynonyms("", ImmutableSet.of());
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getStatusInfo().getReasonPhrase().contains("Missing 'word' argument"),
				"Was: " + response.getStatusInfo().getReasonPhrase());
	}

	@Test
	void addMissingBody() {
		defineCluster();

		Response response = addSynonyms("a", null);
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getStatusInfo().getReasonPhrase().contains("Missing synonym list body"),
				"Was: " + response.getStatusInfo().getReasonPhrase());

		response = addSynonyms("a", ImmutableSet.of());
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getStatusInfo().getReasonPhrase().contains("Missing synonym list body"),
				"Was: " + response.getStatusInfo().getReasonPhrase());
	}

	@ParameterizedTest
	@ValueSource(strings = {" ", "", "\t", "\n"})
	void addBadBody(String badSynonym) {
		defineCluster();

		Response response = addSynonyms("a", ImmutableSet.of("a", "b", badSynonym));
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getStatusInfo().getReasonPhrase().contains("Body contains synonym which is null or empty"),
				"Was: " + response.getStatusInfo().getReasonPhrase());
	}

	@Test
	void addAndGet() {
		defineCluster();

		addSynonyms("a", ImmutableSet.of("b", "c", "d"));
		SynonymPage synonymPage = getSynonyms("a", 10);
		assertEquals(3, synonymPage.total);
		assertEquals(ImmutableList.of("b", "c", "d"), synonymPage.synonyms);
	}

	@Test
	void addAndGetTransitive() {
		defineCluster();

		addSynonyms("a", ImmutableSet.of("b"));
		addSynonyms("b", ImmutableSet.of("c"));

		SynonymPage synonymPage = getSynonyms("a", 10);
		assertEquals(2, synonymPage.total);
		assertEquals(ImmutableList.of("b", "c"), synonymPage.synonyms);

		synonymPage = getSynonyms("b", 10);
		assertEquals(2, synonymPage.total);
		assertEquals(ImmutableList.of("a", "c"), synonymPage.synonyms);

		synonymPage = getSynonyms("c", 10);
		assertEquals(2, synonymPage.total);
		assertEquals(ImmutableList.of("a", "b"), synonymPage.synonyms);
	}

	@Test
	void getMissingWordArgument() {
		defineCluster();

		BadRequestException exception = assertThrows(BadRequestException.class, () -> getSynonyms("", 10));
		assertTrue(exception.getMessage().contains("Missing 'word' argument"), "Was: " + exception.getMessage());
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0})
	void getWithInvalidLimit(int badLimit) {
		defineCluster();

		BadRequestException exception = assertThrows(BadRequestException.class, () -> getSynonyms("a", badLimit));
		assertTrue(exception.getMessage().contains("The 'limit' must be larger than 0 but was " + badLimit),
				"Was: " + exception.getMessage());
	}

	@Test
	void getWithPagination() {
		defineCluster();

		addSynonyms("a", ImmutableSet.of("b", "c", "d"));

		SynonymPage synonymPage = getSynonyms("a", 1);
		assertEquals(3, synonymPage.total);
		assertEquals(ImmutableList.of("b"), synonymPage.synonyms);

		synonymPage = getSynonyms("a", 2);
		assertEquals(3, synonymPage.total);
		assertEquals(ImmutableList.of("b", "c"), synonymPage.synonyms);

		synonymPage = getSynonyms("a", 3);
		assertEquals(3, synonymPage.total);
		assertEquals(ImmutableList.of("b", "c", "d"), synonymPage.synonyms);
	}

	private void defineCluster() {
		target().path("cluster")
				.queryParam("thisInstance", "localhost:" + getPort())
				.request()
				.post(Entity.json(ImmutableList.of("localhost:" + getPort())));
	}

	private Response addSynonyms(String word, Set<String> synonyms) {
		Entity<Set<String>> body = null;
		if (synonyms != null) {
			body = Entity.json(synonyms);
		}
		return target().path("synonyms")
						.queryParam("word", word)
						.request()
						.post(body);
	}

	private SynonymPage getSynonyms(String word, int limit) {
		return target().path("synonyms")
				.queryParam("word", word)
				.queryParam("limit", limit)
				.request()
				.get(new GenericType<>() {});
	}
}
