package norm.dao.poc.jaxrs.error_handling;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import norm.dao.exceptions.NotSetException;
import norm.dao.poc.jaxrs.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@PropertySource("classpath:jaxrs.properties")
@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    private final Logger log = LoggerFactory.getLogger(JsonMappingExceptionMapper.class);

    @Value("${documentation.url}")
    private String documentationUrl;
    @Value("${err.400.property.required}")
    private String propertyRequired;
    @Value("${err.400.property.unknown}")
    private String propertyUnknown;

    @Context
    private Request request;
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(final JsonMappingException exception) {
        Throwable rootException = Utils.getRootCause(exception);

        log.info("JsonMappingException: " + rootException.toString());

        if (rootException instanceof NotSetException)
        {
            return Response.status(Response.Status.BAD_REQUEST).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(new ValidationErrors() {{
                        add(
                                new ValidationError(
                                        propertyRequired,
                                        "{err.400.property.required}",
                                        getPath(exception),
                                        "<NOT SET>",
                                        documentationUrl
                                )
                        );
                    }}).build();
        }
        else if (rootException instanceof UnrecognizedPropertyException)
        {
            final String path = getPath(exception);
            return Response.status(Response.Status.BAD_REQUEST).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(new ValidationErrors() {{
                        add(
                                new ValidationError(
                                        propertyUnknown,
                                        "{err.400.property.unknown}",
                                        path,
                                        path,
                                        documentationUrl
                                )
                        );
                    }}).build();
        }

        return (new UnhandledExceptionMapper()).toResponse(exception);
    }

    private String getPath(final JsonMappingException exception) {
        String path = "";
        for (JsonMappingException.Reference reference : exception.getPath()) {
            path = path + ((path.length() > 0) ? "." : "") + reference.getFieldName();
        }
        return path;
    }
}
