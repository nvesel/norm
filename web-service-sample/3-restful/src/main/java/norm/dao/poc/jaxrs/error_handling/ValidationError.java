package norm.dao.poc.jaxrs.error_handling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public class ValidationError extends ResourceSupport implements Serializable
{
    private final Logger log = LoggerFactory.getLogger(ValidationError.class);

    private String message;
    private String messageCode;
    private String path;
    private Object invalidValue;

    protected ValidationError(final String message, final String messageCode, final String path, final Object invalidValue, String docsUrl) {
        this.message = message;
        this.messageCode = messageCode;
        this.path = path;
        this.invalidValue = invalidValue;
        try {
            add(new Link((new URI(docsUrl)).toString(), Link.REL_NEXT));
        } catch (URISyntaxException e) {
            log.info(e.toString());
        }
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    public String getMessageCode() {
        return messageCode;
    }
}