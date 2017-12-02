package io.github.mortenmolsen.recordreplay.testsystem.rr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mortenmolsen.recordreplay.messages.Message;

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
