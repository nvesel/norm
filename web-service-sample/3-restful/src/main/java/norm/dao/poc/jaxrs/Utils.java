package norm.dao.poc.jaxrs;

public class Utils {
    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause = null;
        Throwable result = throwable;

        while(null != (cause = result.getCause())  && (result != cause) ) {
            result = cause;
        }
        return result;
    }
}
