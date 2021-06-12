package cloudservice;

import cloudservice.Database.SynonymPage;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ServiceUnavailableException;

import java.util.Set;

@Path("synonyms")
public class SynonymResource {
	private final Database database;
	private final Cluster cluster;
	private final Synchronizer synchronizer;

	@Inject
	public SynonymResource(Database database, Cluster cluster, Synchronizer synchronizer) {
		this.database = database;
		this.cluster = cluster;
		this.synchronizer = synchronizer;
	}

	@POST
	public void add(@QueryParam("word") String word,
					@DefaultValue("true") @QueryParam("distribute") boolean distribute,
					Set<String> synonyms) {
		requireClusterReady();

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

		if (distribute) {
			synchronizer.sendToOthers(word, synonyms);
		}
	}

	@GET
	public SynonymPage list(@QueryParam("word") String word, @QueryParam("limit") int limit) {
		requireClusterReady();

		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing 'word' argument");
		}
		if (limit <= 0) {
			throw new BadRequestException("The 'limit' must be larger than 0 but was " + limit);
		}
		return database.getSynonyms(word, limit);
	}

	private void requireClusterReady() {
		if (!cluster.isReadyForQueries()) {
			throw new ServiceUnavailableException("Cluster is not defined");
		}
	}
}
