package com.mmo.recordreplay.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A "message" is a (very fluffy) concept of "something received as a result of a request". NOT, however, as a response, but as a message
 * received later through some channel.
 * The interface does not define much, as a message is very implementation-specific.
 *
 * @author Morten Meiling Olsen
 */
public abstract class Message<T> {

    protected T messageObject;

    public Message(T messageObject) {
        this.messageObject = messageObject;
    }

    /**
     * @return a short description of this message.
     */
    @JsonIgnore
    public abstract String getShortDescription();

    /**
     * @return the actual message object.
     */
    public T getMessageObject() {
        return messageObject;
    }

    /**
     * Sets the message object, will be called by jackson.
     */
    public void setMessageObject(T messageObject) {
        this.messageObject = messageObject;
    }

    @Override
    public String toString() {
        return getShortDescription();
    }
}
