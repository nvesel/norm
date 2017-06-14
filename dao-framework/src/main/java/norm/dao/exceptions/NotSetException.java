package norm.dao.exceptions;

// Consider as user error
public class NotSetException extends UnsupportedOperationException {
    public NotSetException(String msg) {
        super(msg);
    }
}
