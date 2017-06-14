package norm.dao.poc.jaxrs.error_handling;

import norm.dao.poc.jaxrs.Utils;
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
public class UnhandledExceptionMapper implements ExceptionMapper<Throwable> {
    private final Logger log = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

    @Value("${err.500.unexpected}")
    private String msgErrUnexpected;

    @Override
    public Response toResponse(final Throwable exception)
    {
        Throwable rootException = Utils.getRootCause(exception);

        log.error("Unhandled Exception", rootException);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                type(MediaType.APPLICATION_JSON_TYPE).
                entity(new GeneralError(
                        msgErrUnexpected,
                        "{err.500.unexpected}"
                )).build();
    }
}
