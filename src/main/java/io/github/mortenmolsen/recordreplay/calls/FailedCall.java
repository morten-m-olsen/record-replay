package io.github.mortenmolsen.recordreplay.calls;

/**
 * A call that fails - resulting in an exception being thrown.
 *
 * @author Morten Meiling Olsen
 */
public abstract class FailedCall<T extends Throwable> extends ExpectedCall<T> {

    /**
     * The return value class of the call that will be attempted (but is expected to fail).
     */
    private final Class<?> attemptedCallClass;

    public FailedCall(String methodName, Class<T> expectedResponseClass, Class<?> attemptedCallClass) {
        super(methodName, expectedResponseClass);
        this.attemptedCallClass = attemptedCallClass;
    }

    @Override
    public void validatePendingCall(String methodName, Class<?> returnClass) throws CallValidationException {
        // construct another dummy call with the attempted class and make that validate
        new ExpectedCall<>(this.methodName, attemptedCallClass).validatePendingCall(methodName, returnClass);
    }
}
