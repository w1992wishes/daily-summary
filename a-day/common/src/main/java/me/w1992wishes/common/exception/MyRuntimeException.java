package me.w1992wishes.common.exception;

/**
 * @author w1992wishes 2019/3/27 13:51
 */
public class MyRuntimeException extends RuntimeException {

    public MyRuntimeException() {
        super();
    }

    public MyRuntimeException(String message) {
        super(message);
    }

    public MyRuntimeException(Throwable cause) {
        super(cause);
    }

    public MyRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
