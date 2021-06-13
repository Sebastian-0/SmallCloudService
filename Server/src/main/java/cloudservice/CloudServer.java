package cloudservice;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(CloudServer.class);

	public void run() throws Exception {
		Server server = new Server(8080);

		ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		servletContextHandler.addServlet(new ServletHolder("root-servlet", new ServletContainer(new ApiResourceConfig())), "/api/*");
		servletContextHandler.addFilter(CrossOriginFilter.class, "/*", null);

		HandlerList handlerList = new HandlerList();
		handlerList.addHandler(servletContextHandler);
		handlerList.addHandler(new DefaultHandler());

		server.setHandler(handlerList);

		server.start();
		LOGGER.info("Server started...");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOGGER.info("Shutting down...");
			try {
				server.stop();
			} catch (Exception e) {
				LOGGER.error("Failed to stop the Jetty server");
			}
		}));
	}

	public static void main(String[] args) throws Exception {
		new CloudServer().run();
	}
}
