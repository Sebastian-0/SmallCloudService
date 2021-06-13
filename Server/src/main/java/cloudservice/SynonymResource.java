package cloudservice;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.List;
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
			throw new BadRequestException("Missing word argument");
		}
		database.addSynonyms(word, synonyms);
	}

	@GET
	public List<String> list(@QueryParam("word") String word) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing word argument");
		}
		return database.getSynonyms(word);
	}
}
