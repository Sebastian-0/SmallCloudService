package cloudservice;

import cloudservice.Database.SynonymPage;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.Set;

@Path("synonyms")
public class SynonymResource {
	private final Database database;

	@Inject
	public SynonymResource(Database database) {
		this.database = database;
	}

	@POST
	public void add(@QueryParam("word") String word, Set<String> synonyms) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing 'word' argument");
		}
		if (synonyms == null || synonyms.isEmpty()) {
			throw new BadRequestException("Missing synonym list body");
		}
		for (String synonym : synonyms) {
			if (synonym == null || synonym.isBlank()) {
				throw new BadRequestException("Body contains synonym which is null or empty");
			}
		}

		database.addSynonyms(word, synonyms);
	}

	@GET
	public SynonymPage list(@QueryParam("word") String word, @QueryParam("limit") int limit) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing 'word' argument");
		}
		if (limit <= 0) {
			throw new BadRequestException("The 'limit' must be larger than 0 but was " + limit);
		}
		return database.getSynonyms(word, limit);
	}
}
