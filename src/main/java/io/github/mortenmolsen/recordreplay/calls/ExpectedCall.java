package io.github.mortenmolsen.recordreplay.calls;

import io.github.mortenmolsen.recordreplay.TestCallManager;
import io.github.mortenmolsen.recordreplay.testdata.Record;
import io.github.mortenmolsen.recordreplay.testdata.TestDataHandler;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * An expected call that will be made through the {@link TestCallManager}.
 *
 * @param <T> the type of response returned from the call.
 * @author Morten Meiling Olsen
 */
public class ExpectedCall<T> {

    /**
     * Defines which method is expected to be called on the interface that is designated as the point where external communication with the broker begins.
     */
    public final String methodName;

    /**
     * The expected class of the response.
     */
    public final Class<T> expectedResponseClass;

    public ExpectedCall(String methodName, Class<T> expectedResponseClass) {
        this.methodName = methodName;
        this.expectedResponseClass = expectedResponseClass;
    }

    /**
     * Called from the {@link TestCallManager} to verify that an expected call corresponds with what is actually called.
     *
     * @param methodName the name of the method that is ACTUALLY called.
     * @param returnClass the actual return value class of the method that was called.
     */
    public void validatePendingCall(String methodName, Class<?> returnClass) throws CallValidationException {
        if (!this.methodName.equals(methodName)) {
            throw new CallValidationException("Call made to: " + methodName + ", but a call to: " + this.methodName + " was expected.");
        }
        if (this.expectedResponseClass != returnClass) {
            throw new CallValidationException("Call made with return value class: " + returnClass + ", but a call with return value class " +
                    this.expectedResponseClass + " was expected.");
        }
    }

    public void validateResult(@Nullable Object result) throws CallValidationException {
        if (result == null) {
            if (expectedResponseClass != Void.TYPE) {
                throw new CallValidationException("Unexpected 'null' response, expected a response of class " + expectedResponseClass);
            }
        }
        else if (result.getClass() != expectedResponseClass) {
            throw new CallValidationException("Unexpected response of class " + result.getClass() + ". Expected response of class " + expectedResponseClass);
        }
    }

    public void record(@Nullable Object result, Record<?> record) {
        record.setResponse(result);
        System.out.println("Recorded response of class: " + expectedResponseClass);
    }


    public T replay(Record<?> record, TestDataHandler testDataHandler) {
        return testDataHandler.convertValue(record.getResponse(), expectedResponseClass);
    }

    /**
     * @param result the actual result of making this call.
     * @return a possible other Expected call that the execution of this call results in.
     */
    public Optional<ExpectedCall> resultsIn(Object result) {
        // by default, the call does not result in another call
        return Optional.empty();
    }

    public static class CallValidationException extends Exception {

        private static final long serialVersionUID = -1183887006165149801L;

        public CallValidationException(String message) {
            super(message);
        }
    }
}
