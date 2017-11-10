package com.mmo.recordreplay.rr;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mmo.recordreplay.messages.Message;

/**
 * @author Morten Meiling Olsen
 */
public class PoCMessage extends Message<String> {

    public PoCMessage(@JsonProperty("messageObject") String messageObject) {
        super(messageObject);
    }

    @Override
    public String getShortDescription() {
        return "PoC message: " + messageObject;
    }
}
