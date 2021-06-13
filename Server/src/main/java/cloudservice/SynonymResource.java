package cloudservice;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Path("synonyms")
public class SynonymResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(SynonymResource.class);

	private final Database database;
	private final Collator collator;

	@Inject
	public SynonymResource(Database database) {
		this.database = database;
		this.collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
	}

	@POST
	public void add(@QueryParam("word") String word, Set<String> synonyms) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing word argument");
		}
		LOGGER.info("Synonyms to add: {} -> {}", word, synonyms);
		database.addSynonyms(word, synonyms);
	}

	@GET
	public List<String> list(@QueryParam("word") String word) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing word argument");
		}
		// Ideally we don't want to duplicate the data by creating a list but it's hard to sort it otherwise
		List<String> result = new ArrayList<>(database.getSynonyms(word));
		result.sort(collator);
		LOGGER.info("Synonyms read: {} -> {}", word, result);
		return result;
	}
}
