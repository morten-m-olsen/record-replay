package io.github.mortenmolsen.recordreplay.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.junit.Assert;

/**
 * A message sender is capable of sending a message out into the system in some way.
 * It also maintains a queue of messages that are ready to be sent.
 *
 * @author Morten Meiling Olsen
 */
@SuppressFBWarnings("UC_USELESS_OBJECT")
public abstract class MessageSender<T extends Message> {

    private final List<T> queuedMessages = new CopyOnWriteArrayList<>();

    private final ExpectedCallsFromMessages<T> expectedCallsFromMessages;

    protected MessageSender(ExpectedCallsFromMessages<T> expectedCallsFromMessages) {
        this.expectedCallsFromMessages = expectedCallsFromMessages;
    }

    public final void addToQueue(T message) {
        queuedMessages.add(message);
    }

    /**
     * Asserts that the first message matches the given predicate, and sends and returns it, if it does (otherwise fails).
     */
    public final T assertAndSendFirstMessage(Predicate<T> matcher) {
        Assert.assertTrue("Message did not match the predicate: " + queuedMessages.get(0).getMessageObject(), matcher.test(queuedMessages.get(0)));
        return sendFirstMessage();
    }

    /**
     * Retrieves the first message that matches the given predicate, and returns it.
     * NOTE: this does NOT send the message. The message can be send afterwards using {@link #sendMessage(Message)} or any of the other methods (it remains in the queue).
     */
    public final MessageInQueue getMessageForSending(Predicate<T> matcher) {
        return new MessageInQueue(queuedMessages.stream().filter(message -> matcher.test(message)).findFirst().get());
    }

    public final T sendFirstMessage() {
        T toSend = queuedMessages.remove(0);
        sendMessage(toSend);
        return toSend;
    }

    public final void sendAllMessages() {
        // since each message might instigate a call that adds more messages we must clear the existing ones before sending
        List<T> toSend = new ArrayList<>(queuedMessages);
        queuedMessages.clear();
        toSend.forEach(message -> sendMessage(message));
    }

    public final boolean isEmpty() {
        return queuedMessages.isEmpty();
    }

    /**
     * Sends a message out into the system.
     * This method can also be called with a non-queued message simply to access the sending logic of this class.
     *
     * @param message the message to send.
     */
    public final void sendMessage(T message) {
        expectedCallsFromMessages.setUpExpectations(message);
        doSendMessage(message);
    }

    /**
     * Does the actual sending - must be implemented by subclasses.
     *
     * @param message the message to send.
     */
    protected abstract void doSendMessage(T message);

    public class MessageInQueue {

        @Getter
        private final T message;

        public MessageInQueue(T message) {
            this.message = message;
        }

        public void send() {
            if (queuedMessages.contains(message)) {
                queuedMessages.remove(message);
                sendMessage(message);
            }
            else {
                throw new IllegalStateException("Message already sent.");
            }
        }

    }

}
