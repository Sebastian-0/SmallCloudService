package cloudservice;

import cloudservice.Database.Synonyms;
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
	void addAndGetMissingWordArgument() {
		Response response = addSynonyms("", ImmutableSet.of());
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

		BadRequestException exception = assertThrows(BadRequestException.class, () -> getSynonyms("", 10));
		assertTrue(exception.getMessage().contains("Missing 'word' argument"), "Was: " + exception.getMessage());
	}

	@Test
	void addAndGet() {
		addSynonyms("a", ImmutableSet.of("b", "c", "d"));
		Synonyms synonyms = getSynonyms("a", 10);
		assertEquals(3, synonyms.total);
		assertEquals(ImmutableList.of("b", "c", "d"), synonyms.synonyms);
	}

	@Test
	void addAndGetTransitive() {
		addSynonyms("a", ImmutableSet.of("b"));
		addSynonyms("b", ImmutableSet.of("c"));

		Synonyms synonyms = getSynonyms("a", 10);
		assertEquals(2, synonyms.total);
		assertEquals(ImmutableList.of("b", "c"), synonyms.synonyms);

		synonyms = getSynonyms("b", 10);
		assertEquals(2, synonyms.total);
		assertEquals(ImmutableList.of("a", "c"), synonyms.synonyms);

		synonyms = getSynonyms("c", 10);
		assertEquals(2, synonyms.total);
		assertEquals(ImmutableList.of("a", "b"), synonyms.synonyms);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0})
	void getWithInvalidLimit(int badLimit) {
		BadRequestException exception = assertThrows(BadRequestException.class, () -> getSynonyms("a", badLimit));
		assertTrue(exception.getMessage().contains("The 'limit' must be larger than 0 but was " + badLimit),
				"Was: " + exception.getMessage());
	}

	@Test
	void getWithPagination() {
		addSynonyms("a", ImmutableSet.of("b", "c", "d"));

		Synonyms synonyms = getSynonyms("a", 1);
		assertEquals(3, synonyms.total);
		assertEquals(ImmutableList.of("b"), synonyms.synonyms);

		synonyms = getSynonyms("a", 2);
		assertEquals(3, synonyms.total);
		assertEquals(ImmutableList.of("b", "c"), synonyms.synonyms);

		synonyms = getSynonyms("a", 3);
		assertEquals(3, synonyms.total);
		assertEquals(ImmutableList.of("b", "c", "d"), synonyms.synonyms);
	}

	private Response addSynonyms(String word, Set<String> synonyms) {
		return target().path("synonyms")
						.queryParam("word", word)
						.request()
						.post(Entity.json(synonyms));
	}

	private Synonyms getSynonyms(String word, int limit) {
		return target().path("synonyms")
				.queryParam("word", word)
				.queryParam("limit", limit)
				.request()
				.get(new GenericType<>() {});
	}
}
