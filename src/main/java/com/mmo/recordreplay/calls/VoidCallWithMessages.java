package com.mmo.recordreplay.calls;

import com.mmo.recordreplay.messages.Message;
import com.mmo.recordreplay.messages.WaitingInput;

/**
 * @author Morten Meiling Olsen
 */
public class VoidCallWithMessages<T extends Message> extends CallWithMessages<Void, T> {
    public VoidCallWithMessages(String methodName, WaitingInput<T> waitingInput) {
        super(methodName, Void.TYPE, waitingInput);
    }
}
