package io.github.mortenmolsen.recordreplay.calls;

import io.github.mortenmolsen.recordreplay.messages.Message;
import io.github.mortenmolsen.recordreplay.messages.WaitingInput;

/**
 * @author Morten Meiling Olsen
 */
public class VoidCallWithMessages<T extends Message> extends CallWithMessages<Void, T> {
    public VoidCallWithMessages(String methodName, WaitingInput<T> waitingInput) {
        super(methodName, Void.TYPE, waitingInput);
    }
}
