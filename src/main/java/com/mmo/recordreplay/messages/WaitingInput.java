package com.mmo.recordreplay.messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.mmo.recordreplay.TestCallManagerAwareTestRule;
import com.mmo.recordreplay.RetryTriggeringRecordingException;

/**
 * Input needed to determine what to wait for/how long etc.
 *
 * @author Morten Meiling Olsen
 */
public final class WaitingInput<M extends Message> {

    /**
     * The default time that is waited when recording.
     */
    public static final Integer DEFAULT_WAIT_TIME_SECONDS = 10;

    /**
     * The time that can be used for waiting when recording.
     */
    public int waitingTime;

    /**
     * Whether the waiting that is to be done might fail, in a way that warrants a retry. Used to control which exception is thrown during recording to be able to retry from the
     * {@link TestCallManagerAwareTestRule}.
     */
    public boolean retryTriggeringFailPossible = false;

    public final List<MessageIdentifier<M>> messageIdentifiers = new ArrayList<>();

    @SafeVarargs
    public WaitingInput(MessageIdentifier<M>... messageIdentifiers) {
        this(DEFAULT_WAIT_TIME_SECONDS, messageIdentifiers);
    }

    @SafeVarargs
    public WaitingInput(int waitingTime, MessageIdentifier<M>... messageIdentifiers) {
        this.messageIdentifiers.addAll(Arrays.asList(messageIdentifiers));
        this.waitingTime = waitingTime;
    }

    public WaitingInput<M> expect(MessageIdentifier<M> messageIdentifiers) {
        this.messageIdentifiers.add(messageIdentifiers);
        return this;
    }

    @SafeVarargs
    public final WaitingInput<M> expect(MessageIdentifier<M>... messageIdentifiers) {
        this.messageIdentifiers.addAll(Arrays.asList(messageIdentifiers));
        return this;
    }

    /**
     * If, during recording, we do not know exactly how many messages will actually be coming in.
     */
    public final boolean unknownNumberOfMessagesExpected() {
        // if just ONE of the message identifiers has the "AT_LEAST" or the "DISTINCT" mode, we must wait for the "null" message (no more messages)
        // this is because in this case we do not know the exact number of messages, but know when the action is complete on the IB side. For instance, when having received
        // an open order message we know that the order is opened (but then we might receive several of these that we also want to capture, therefore we wait until there are
        // no more messages.
        return messageIdentifiers
                .stream()
                .filter(messageIdentifier -> messageIdentifier.mode == MessageIdentifier.Mode.AT_LEAST ||
                        messageIdentifier.mode == MessageIdentifier.Mode.DISTINCT)
                .count() > 0;
    }

    /**
     * @return a predicate that matches any of the expected messages.
     */
    public Predicate<M> getAcceptPredicate() {
        return message -> {
            for (MessageIdentifier<M> identifier : messageIdentifiers) {
                if (identifier.matches(message)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * @return a {@link MessageIdentifier.MessageCheckResult} representing the state of the waiting.
     */
    public final MessageIdentifier.MessageCheckResult checkReceivedMessages(List<M> messages) {
        boolean allOk = true;
        for (MessageIdentifier<M> messageIdentifier : messageIdentifiers) {
            MessageIdentifier.MessageCheckResult result = messageIdentifier.checkReceivedMessages(messages);
            if (result == MessageIdentifier.MessageCheckResult.TOO_MANY) {
                // the identifier has received too many messages - we must return as this is not an acceptable state (and the test must fail)
                return result;
            }
            else if (result == MessageIdentifier.MessageCheckResult.NOT_YET) {
                // this identifier has not yet received the messages it needs - so we cannot overall return OK. We need to keep checking the rest of the identifiers, though,
                // since some might have received too many messages
                allOk = false;
            }
        }
        return allOk ? MessageIdentifier.MessageCheckResult.OK : MessageIdentifier.MessageCheckResult.NOT_YET;
    }

    public WaitingInput<M> failShouldTriggerRetry() {
        retryTriggeringFailPossible = true;
        return this;
    }

    public MessageCollector.Acceptor<M> getAcceptor() {
        return new WaitingInputAcceptor<>(this);
    }

    private static class WaitingInputAcceptor<T extends Message> extends MessageCollector.Acceptor<T> {

        private static int WAITING_THRESHOLD = 2000;

        private final WaitingInput<T> waitingInput;
        private volatile long lastMessageAcceptedAt;

        public WaitingInputAcceptor(WaitingInput<T> waitingInput) {
            super(waitingInput.waitingTime * 1000, waitingInput.getAcceptPredicate());
            this.waitingInput = waitingInput;
            System.out.println("Will wait for at most " + waitingInput.waitingTime + " seconds for each of the following messages: " +
                    waitingInput.messageIdentifiers
                            .stream()
                            .map(MessageIdentifier::getInfo)
                            .collect(Collectors.joining(",")));
        }

        @Override
        public boolean doAccept(T message) {
            if (super.doAccept(message)) {
                System.out.println("Accepted: " + message);
                lastMessageAcceptedAt = System.currentTimeMillis();
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        protected MessageIdentifier.MessageCheckResult checkMessages() {
            MessageIdentifier.MessageCheckResult result = waitingInput.checkReceivedMessages(acceptedMessages);
            if (result == MessageIdentifier.MessageCheckResult.TOO_MANY) {
                return MessageIdentifier.MessageCheckResult.TOO_MANY;
            }
            //noinspection SimplifiableIfStatement
            if (waitingInput.unknownNumberOfMessagesExpected()
                    && result == MessageIdentifier.MessageCheckResult.OK
                    && System.currentTimeMillis() - lastMessageAcceptedAt < WAITING_THRESHOLD) {
                return MessageIdentifier.MessageCheckResult.NOT_YET;
            }
            else {
                return result;
            }
        }

        @Override
        protected String getFailErrorMessage() {
            return super.getFailErrorMessage() +
                    "\nExpected: " + waitingInput.messageIdentifiers.stream()
                    .map(message -> message.getInfo())
                    .collect(Collectors.toList()) +

                    "\nDid NOT receive (or received too many): " + waitingInput.messageIdentifiers
                    .stream()
                    .filter(messageIdentifier -> messageIdentifier.checkReceivedMessages(acceptedMessages) != MessageIdentifier.MessageCheckResult.OK)
                    .map(identifier -> identifier.getInfo())
                    .collect(Collectors.toList());
        }

        @Override
        protected void fail() {
            if (waitingInput.retryTriggeringFailPossible) {
                // we throw an exception instead of failing directly, allowing the test runner to decide what to do
                throw new RetryTriggeringRecordingException(getFailErrorMessage());

            } else {
                super.fail();
            }
        }
    }

}
