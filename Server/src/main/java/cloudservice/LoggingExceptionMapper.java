package cloudservice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingExceptionMapper.class);

	@Context
	private HttpServletRequest sourceRequest;

	@Override
	public Response toResponse(Exception exception) {
		if (sourceRequest != null) { // Will be null in tests
			LOGGER.error("Encountered exception when executing: {}?{}", sourceRequest.getRequestURI(), sourceRequest.getQueryString(), exception);
		}

		if (exception instanceof WebApplicationException) {
			WebApplicationException webApplicationException = (WebApplicationException) exception;
			return Response.status(webApplicationException.getResponse().getStatus(),
					webApplicationException.getResponse().getStatusInfo().getReasonPhrase() + " - " + webApplicationException.getMessage()).build();
		}

		return Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getMessage()).build();
	}
}
