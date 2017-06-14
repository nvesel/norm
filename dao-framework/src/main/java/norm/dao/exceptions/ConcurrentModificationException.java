package norm.dao.exceptions;

// Consider as user error. MVCC violation.
public class ConcurrentModificationException extends java.util.ConcurrentModificationException {
    public ConcurrentModificationException(String message) {
        super(message);
    }
}
