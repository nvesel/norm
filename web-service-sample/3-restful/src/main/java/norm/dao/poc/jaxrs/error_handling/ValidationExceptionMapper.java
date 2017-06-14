package norm.dao.poc.jaxrs.error_handling;

import norm.dao.poc.jaxrs.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.PropertyResolver;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.*;

//@Component //TODO: jersey ignores this class if enabled. Why?
@PropertySource("classpath:jaxrs.properties")
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException>
{
    private final Logger log = LoggerFactory.getLogger(ValidationExceptionMapper.class);

    @Autowired
    private PropertyResolver propertyResolver;

    @Override
    public Response toResponse(final ValidationException exception)
    {
        Throwable rootException = Utils.getRootCause(exception);

        log.info("ValidationException: "+rootException.toString());

        if (exception instanceof ConstraintViolationException)
        {
            final ConstraintViolationException cve = (ConstraintViolationException) exception;
            final Response.ResponseBuilder response = Response.status(Response.Status.BAD_REQUEST);
            response.type(MediaType.APPLICATION_JSON_TYPE);
            response.entity(
                    getMessageEntity(cve.getConstraintViolations())
            );
            return response.build();
        }

        return (new UnhandledExceptionMapper()).toResponse(exception);
    }

    private List<ValidationError> getMessageEntity(final Set<ConstraintViolation<?>> violations)
    {
        final List<ValidationError> errors = new ArrayList<>();

        for (final ConstraintViolation<?> violation : violations)
        {
            errors.add(
                    new ValidationError(
                            violation.getMessage(),
                            violation.getMessageTemplate(),
                            getPath(violation),
                            getInvalidValue(violation.getInvalidValue()),
                            propertyResolver.getProperty("documentation.url")
                    )
            );
        }

        return errors;
    }

    private String getInvalidValue(final Object invalidValue)
    {
        if (invalidValue == null)
        {
            return null;
        }

        if (invalidValue.getClass().isArray())
        {
            return Arrays.toString((Object[]) invalidValue);
        }

        return invalidValue.toString();
    }

    private String getPath(final ConstraintViolation<?> violation)
    {
        int count = 0;

        String path_orig = violation.getPropertyPath().toString();

        for (Iterator<Path.Node> iterator = violation.getPropertyPath().iterator(); iterator.hasNext(); ) {
            Path.Node currentNode = iterator.next();
            if (currentNode != null && !currentNode.toString().isEmpty()) {
                count++;
                if (count <= 2) iterator.remove();
            }
        }

        String path_fixed = violation.getPropertyPath().toString();

        return (path_fixed.length() == 0)?path_orig:path_fixed;
    }
}
