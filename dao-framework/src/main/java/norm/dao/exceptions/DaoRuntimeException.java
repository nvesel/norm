package norm.dao.exceptions;

//Unexpected internal error
public class DaoRuntimeException extends RuntimeException {
    public DaoRuntimeException(String message) {
        super(message);
    }
    public DaoRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
