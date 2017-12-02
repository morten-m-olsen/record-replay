package io.github.mortenmolsen.recordreplay.messages;

/**
 * @author Morten Meiling Olsen
 */
public class NoOpMessageSender<T extends Message> extends MessageSender<T> {

    public NoOpMessageSender() {
        super(new ExpectedCallsFromMessages.NoOpExpectedCallsFromMessages<T>());
    }

    @Override
    protected void doSendMessage(T message) {
        // do nothing
    }
}
