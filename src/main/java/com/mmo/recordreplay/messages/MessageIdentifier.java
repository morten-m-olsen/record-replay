package com.mmo.recordreplay.messages;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Identifies a number of (a certain kind of) messages that come in. This is used to "expect" what messages that comes in as response to sending some request.
 *
 * @author Morten Meiling Olsen
 */
public abstract class MessageIdentifier<T extends Message> {

    /**
     * The required number of the identified message to receive (used along with {@link #mode} to determine if the identifier
     * has received all expected messages.
     */
    public final int requiredMessages;

    /**
     * The mode that the identifier works in - see javadoc on {@link MessageIdentifier.Mode}.
     */
    public final Mode mode;

    /**
     * The comparator used to test a message.
     */
    private final Predicate<T> messageComparator;

    protected MessageIdentifier(int requiredMessages, Mode mode, Predicate<T> messageComparator) {
        this.requiredMessages = requiredMessages;
        this.mode = mode;
        this.messageComparator = messageComparator;
    }

    /**
     * @return true if the given message is matched - that is is an expected message.
     */
    public final boolean matches(T message) {
        return messageComparator.test(message);
    }

    /**
     * @return a string identifying what type of message is identified.
     */
    protected abstract String getIdentifier();

    /**
     * @return a string describing what is identified.
     */
    public String getInfo() {
        return getIdentifier() + "(" + mode + ":" + requiredMessages + ")";
    }

    /**
     * Determines if the given list of messages contains the expected number of message for this identifier.
     */
    public MessageCheckResult checkReceivedMessages(List<T> alreadyReceivedMessages) {
        List<T> matching = alreadyReceivedMessages
                .stream()
                .filter(receivedMessage -> matches(receivedMessage))
                .collect(Collectors.toList());

        long totalNumMatches = matching.size();

        switch (mode) {
            case EXACT:
                if (totalNumMatches < requiredMessages) {
                    return MessageCheckResult.NOT_YET;
                }
                else if (totalNumMatches == requiredMessages) {
                    return MessageCheckResult.OK;
                }
                else {
                    return MessageCheckResult.TOO_MANY;
                }
            case AT_LEAST:
                if (totalNumMatches >= requiredMessages) {
                    return MessageCheckResult.OK;
                }
                else {
                    return MessageCheckResult.NOT_YET;
                }
            case DISTINCT:
                long numDistinct = matching
                        .stream()
                        .filter(distinctMessages()).count();

                if (numDistinct < requiredMessages) {
                    return MessageCheckResult.NOT_YET;
                }
                else if (numDistinct == requiredMessages) {
                    return MessageCheckResult.OK;
                }
                else {
                    return MessageCheckResult.TOO_MANY;
                }

            default:
                throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }

    /**
     * @return a predicate that identifies distinct messages. Their "distinctness" is determined by the function {@link #distinctIdentifier(Message)} which must
     * be implemented by subclasses.
     */
    private Predicate<T> distinctMessages() {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(distinctIdentifier(t), Boolean.TRUE) == null;
    }

    /**
     * @return a string distinctly identifying the message (for instance in IB, we might get many "openOrder" messages and want to distinguish between openOrder messages
     * for distinct orders).
     */
    protected abstract String distinctIdentifier(T message);

    /**
     * The mode determines how many messages the identifier expects to receive.
     */
    public enum Mode {

        /**
         * Exactly some number of messages is expected.
         */
        EXACT,

        /**
         * At least some number is expected (may be 0!) - but more is acceptable.
         * This mode will mean that, during waiting, we must wait until we have not received messages for a certain time (since we don't know the exact number we are waiting for).
         */
        AT_LEAST,

        /**
         * In this mode a certain number of distinct messages must be received. The distinctness is defined by the function {@link MessageIdentifier#distinctIdentifier(Message)}.
         * In this mode, the total number of messages does not matter, only the number of distinct messages. However, to ensure that we have received all messages, also in this
         * mode we must wait until we no longer receive messages, for a certain time, before we can stop.
         */
        DISTINCT
    }

    /**
     * The result of an evaluation of received messages.
     */
    public enum MessageCheckResult {
        /**
         * Means that a list of messages satisfies the expectations of the identifier.
         */
        OK,
        /**
         * Means that a list of messages does NOT satisfy the expectations, but still might.
         */
        NOT_YET,

        /**
         * Means that too many messages of the expected kind has been received.
         */
        TOO_MANY
    }
}
