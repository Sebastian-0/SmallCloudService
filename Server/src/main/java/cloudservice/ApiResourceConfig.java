package cloudservice;

import jakarta.inject.Singleton;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

public class ApiResourceConfig extends ResourceConfig {
	public ApiResourceConfig() {
		register(LoggingExceptionMapper.class);
		register(JsonIO.class);

		register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(Database.class).to(Database.class).in(Singleton.class);
			}
		});

		register(SynonymResource.class);
	}
}
