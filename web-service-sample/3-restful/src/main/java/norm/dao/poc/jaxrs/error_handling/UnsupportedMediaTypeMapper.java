package norm.dao.poc.jaxrs.error_handling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@PropertySource("classpath:jaxrs.properties")
@Provider
public class UnsupportedMediaTypeMapper implements ExceptionMapper<NotSupportedException> {
    private final Logger log = LoggerFactory.getLogger(UnsupportedMediaTypeMapper.class);

    @Value("${documentation.url}")
    private String documentationUrl;
    @Value("${err.400.mediaType.unsupported")
    private String mediaTypeUnsupported;

    @Override
    public Response toResponse(final NotSupportedException exception) {

        log.info("JsonMappingException: " + exception.toString());

        if ("HTTP 415 Unsupported Media Type".equals(exception.getMessage())) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(new ValidationErrors() {{
                        add(
                                new ValidationError(
                                        mediaTypeUnsupported,
                                        "{err.400.mediaType.unsupported}",
                                        "Content-Type",
                                        null,
                                        documentationUrl
                                )
                        );
                    }}).build();
        }

        return (new UnhandledExceptionMapper()).toResponse(exception);
    }
}
