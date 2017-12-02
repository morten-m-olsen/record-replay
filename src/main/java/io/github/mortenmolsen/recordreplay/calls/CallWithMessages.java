package io.github.mortenmolsen.recordreplay.calls;

import io.github.mortenmolsen.recordreplay.messages.Message;
import io.github.mortenmolsen.recordreplay.messages.WaitingInput;

/**
 * A call that is expected to, on top of returning some result, result in some messages being received.
 *
 * @param <T> the type of response returned from the call.
 * @param <M> the type of possible messages following the call.
 * @author Morten Meiling Olsen
 */
public class CallWithMessages<T, M extends Message> extends ExpectedCall<T> {

    public WaitingInput<M> waitingInput;

    public CallWithMessages(String methodName, Class<T> expectedResponseClass, WaitingInput<M> waitingInput) {
        super(methodName, expectedResponseClass);
        this.waitingInput = waitingInput;
    }
}
