package apps;

public class UnauthException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public UnauthException(String msg) {
        super(msg);
    }
}
