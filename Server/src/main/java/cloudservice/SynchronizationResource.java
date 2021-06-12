package cloudservice;

import cloudservice.Database.Entry;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Path("synchronization")
public class SynchronizationResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(SynonymResource.class);

	private final Database database;

	@Inject
	public SynchronizationResource(Database database) {
		this.database = database;
	}

	@POST
	public void importAll(List<Entry> entries) {
		LOGGER.info("Incoming synchronization import of {} entries", entries.size());
		for (Entry entry : entries) {
			database.addSynonyms(entry.word, entry.synonyms);
		}
	}
}
