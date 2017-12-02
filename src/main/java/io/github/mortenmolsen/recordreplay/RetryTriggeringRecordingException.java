package io.github.mortenmolsen.recordreplay;

/**
 * Exception signalling that recording has failed in a way that warrants retrying.
 *
 * @author Morten Meiling Olsen
 */
public class RetryTriggeringRecordingException extends RuntimeException {

    private static final long serialVersionUID = -7239644440886392366L;

    public RetryTriggeringRecordingException(String errorMessage) {
        super(errorMessage);
    }
}
