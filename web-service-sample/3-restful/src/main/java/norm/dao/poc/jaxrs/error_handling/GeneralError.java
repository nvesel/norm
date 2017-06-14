package norm.dao.poc.jaxrs.error_handling;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

import java.io.Serializable;
import java.util.List;

public class GeneralError extends ResourceSupport implements Serializable
{
    private String message;
    private String messageCode;

    public GeneralError(String message, String messageCode) {
        this.message = message;
        this.messageCode = messageCode;
    }

    public GeneralError(String message, String messageCode, List<Link> links) {
        this.message = message;
        this.messageCode = messageCode;
        if (links != null)
            add(links);
    }

    public String getMessage() {
        return message;
    }

    public String getMessageCode() {
        return messageCode;
    }
}