package cloudservice;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Path("synonyms")
public class SynonymResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(SynonymResource.class);

	@POST
	public void add(@QueryParam("word") String word, List<String> synonyms) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing word argument");
		}

		LOGGER.info("Synonyms to add: {} -> {}", word, synonyms);
	}

	@GET
	public List<String> list(@QueryParam("word") String word) {
		if (word == null || word.isBlank()) {
			throw new BadRequestException("Missing word argument");
		}

		List<String> result = List.of("empty result");
		LOGGER.info("Synonyms read: {} -> {}", word, result);
		return result;
	}
}
