package norm.dao.poc.service;

public class ServiceException extends RuntimeException {

    private StatusCode statusCode = StatusCode.INTERNAL_ERROR;
    private String messageCode;
    private String message;
    private String path = null;
    private Object invalidValue = null;

    public ServiceException(String messageCode, String message)
    {
        super(message);
        this.messageCode = messageCode;
    }

    public ServiceException(String messageCode, String message, String path, Object invalidValue)
    {
        super(message);
        this.messageCode = messageCode;
        this.path = path;
        this.invalidValue = invalidValue;
        this.statusCode = StatusCode.BAD_REQUEST;
    }

    public ServiceException(StatusCode status, String messageCode, String message)
    {
        super(message);
        this.messageCode = messageCode;
        this.statusCode = status;
    }

    public ServiceException(StatusCode status, String messageCode, String message, Throwable cause)
    {
        super(message, cause);
        this.messageCode = messageCode;
        this.statusCode = status;
    }

    public int getCode() {
        return statusCode.getCode();
    }

    public String getMessageCode()
    {
        return messageCode;
    }

    public String getPath() {
        return path;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    public enum StatusCode {
        BAD_REQUEST(400),
        INTERNAL_ERROR(500);

        private int code;

        StatusCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }
}
