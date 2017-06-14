package norm.dao.poc.jaxrs.error_handling;

import norm.dao.poc.jaxrs.Utils;
import norm.dao.poc.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@PropertySource("classpath:jaxrs.properties")
@Provider
public class ServiceExceptionMapper implements ExceptionMapper<ServiceException> {
    private final Logger log = LoggerFactory.getLogger(ServiceExceptionMapper.class);

    @Value("${documentation.url}")
    private String documentationUrl;

    @Override
    public Response toResponse(final ServiceException exception)
    {
        log.info("ServiceExceptionMapper: " + Utils.getRootCause(exception).toString());

        if (exception.getCode() >= 400 && exception.getCode() < 500) {
            return Response.status(exception.getCode()).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(new ValidationErrors() {{
                        add(
                                new ValidationError(
                                        exception.getMessage(),
                                        exception.getMessageCode(),
                                        exception.getPath(),
                                        exception.getInvalidValue(),
                                        documentationUrl
                                )
                        );
                    }}).build();
        }
        else {
            return Response.status(exception.getCode()).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(new GeneralError(
                            exception.getMessage(),
                            exception.getMessageCode()
                    )).build();
        }
    }
}
