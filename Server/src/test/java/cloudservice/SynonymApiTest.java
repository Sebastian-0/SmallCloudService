package cloudservice;

import cloudservice.util.JUnit5JerseyTest;
import com.google.common.collect.ImmutableSet;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
	void errorWhenMissingArgument() {
		Response response = addSynonyms("", ImmutableSet.of());
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

		assertThrows(BadRequestException.class, () -> getSynonyms(""));
	}

	@Test
	void addAndGet() {
		addSynonyms("a", ImmutableSet.of("b", "c", "d"));
		assertEquals(ImmutableSet.of("b", "c", "d"), getSynonyms("a"));
	}

	@Test
	void addAndGetTransitive() {
		addSynonyms("a", ImmutableSet.of("b"));
		addSynonyms("b", ImmutableSet.of("c"));

		assertEquals(ImmutableSet.of("b", "c"), getSynonyms("a"));
		assertEquals(ImmutableSet.of("a", "c"), getSynonyms("b"));
		assertEquals(ImmutableSet.of("a", "b"), getSynonyms("c"));
	}

	private Response addSynonyms(String word, Set<String> synonyms) {
		return target().path("synonyms")
						.queryParam("word", word)
						.request()
						.post(Entity.json(synonyms));
	}

	private Set<String> getSynonyms(String word) {
		return target().path("synonyms")
				.queryParam("word", word)
				.request()
				.get(new GenericType<>() {});
	}
}
