package cloudservice;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Path("synonyms")
public class SynonymResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(SynonymResource.class);

	@Inject
	private Database database;

	@POST
	public void add(@QueryParam("word") String word, Set<String> synonyms) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing word argument");
		}
		LOGGER.info("Synonyms to add: {} -> {}", word, synonyms);
		database.addSynonyms(word, synonyms);
	}

	@GET
	public Set<String> list(@QueryParam("word") String word) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing word argument");
		}
		Set<String> result = database.getSynonyms(word);
		LOGGER.info("Synonyms read: {} -> {}", word, result);
		return result;
	}
}
