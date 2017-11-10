package com.mmo.recordreplay.messages;

import java.util.UUID;

/**
 * This is basically the "lazy man's" message identifier. Add this to a waiting input to simply have it accept everything.
 * This can be useful to quickly write tests where the purpose of the test isn't to anally assert that certain messages come in.
 *
 * @author Morten Meiling Olsen
 */
public class AllIncomingMessagesIdentifier<T extends Message> extends MessageIdentifier<T> {

    public AllIncomingMessagesIdentifier() {
        super(0, Mode.AT_LEAST, message -> true);
    }

    @Override
    protected String getIdentifier() {
        return "Any message";
    }

    @Override
    protected String distinctIdentifier(T message) {
        return UUID.randomUUID().toString();
    }
}
