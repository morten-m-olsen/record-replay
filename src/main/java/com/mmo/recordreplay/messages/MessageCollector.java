package com.mmo.recordreplay.messages;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import lombok.Synchronized;
import org.junit.Assert;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Responsible for collecting messages. Will only actually collect messages if a {@link MessageCollector.Acceptor} is set in which case the messages
 * will be collected into that.
 * It is possible to set a "non-expected" acceptor on the collector, which will determine how messages arriving when no actual acceptor is set are treated.
 *
 * @author Morten Meiling Olsen
 */
public final class MessageCollector<T extends Message> implements SmartLifecycle {

    private static final ThreadFactory THREAD_FACTORY = new CustomizableThreadFactory("Message-Collector-Heartbeat-Thread-");

    private Acceptor<T> acceptor;
    private Acceptor<T> nonExpectedAcceptor;

    private volatile String errorMessage;
    private final Object mutex = new Object();
    private volatile boolean running = false;
    private ScheduledExecutorService executorService;

    @Synchronized("mutex")
    public final void startAccepting(Acceptor<T> acceptor) {
        if (this.acceptor != null) {
            Assert.fail("Already in the process of collecting messages.");
        }
        this.acceptor = acceptor;
    }

    public final List<T> getCollectedMessages() {
        synchronized(mutex) {
            if (errorMessage != null) {
                // an error must have occurred while we were busy elsewhere, just fail now
                Assert.fail(errorMessage);
            }
            if (acceptor == null) {
                return Collections.emptyList();
            }
        }
        try {
            List<T> collectedMessages = acceptor.getCollectedMessages();
            if (errorMessage != null) {
                // an error occurred while waiting...
                Assert.fail(errorMessage);
            }
            return collectedMessages;
        }
        finally {
            synchronized (mutex) {
                acceptor = null;
            }
        }
    }

    @Synchronized("mutex")
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * This method ensures that acceptors that waits for an unknown number of messages get a chance to release the collection semaphore if they have waited for the designated
     * amount of time (and are satisfied with the received messages).
     * <p>
     * This is necessary because otherwise the acceptors only have a chance of reacting when messages are actually received.
     */
    @Synchronized("mutex")
    private void heartbeat() {
        if (acceptor != null && !acceptor.completed) {
            acceptor.releaseSemaphoreIfDone();
        }
    }

    @Synchronized("mutex")
    public void setNonExpectedAcceptor(Acceptor<T> nonExpectedAcceptor) {
        this.nonExpectedAcceptor = nonExpectedAcceptor;
    }

    /**
     * This method must be invoked when messages are received.
     * NOTE: this method will most likely be called by another thread than the main one.
     */
    @Synchronized("mutex")
    public final void messageReceived(T message) {
        if (acceptor == null || !acceptor.accept(message)) {
            // no current acceptor, or message not expected by acceptor
            if (nonExpectedAcceptor == null || !nonExpectedAcceptor.accept(message)) {
                errorMessage = "Received unacceptable unexpected message: " + message;
                // IF there actually is an acceptor - release the semaphore - we should fail now...
                if (acceptor != null) {
                    acceptor.forceRelease();
                }
            }
        }
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            running = false;
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public void start() {
        executorService = Executors.newScheduledThreadPool(1, THREAD_FACTORY);
        executorService.scheduleAtFixedRate(() -> heartbeat(), 0, 2000, TimeUnit.MILLISECONDS);
        running = true;
    }

    @Override
    public void stop() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

    /**
     * An acceptor is responsible for dealing with incoming messages in order to ensure that a certain type/number of messages are received.
     *
     * @param <T> the type of messages that the acceptor works with.
     */
    public static abstract class Acceptor<T extends Message> {

        private final Semaphore collectionSemaphore = new Semaphore(0);
        private final int waitingTimeMs;

        private volatile boolean completed = false;

        protected final List<T> acceptedMessages = new CopyOnWriteArrayList<>();
        private final Predicate<T> acceptPredicate;

        public Acceptor(int waitingTimeMs, Predicate<T> acceptPredicate) {
            this.waitingTimeMs = waitingTimeMs;
            this.acceptPredicate = acceptPredicate;
        }

        /**
         * Attempts to accept the given message.
         *
         * @return true if message is accepted (it is an expected message), false otherwise.
         */
        public final boolean accept(T message) {
            if (completed) {
                return false;
            }
            if (doAccept(message)) {
                acceptedMessages.add(message);
                releaseSemaphoreIfDone();
                return true;
            }
            else {
                System.out.println("Acceptor rejected message: " + message);
                return false;
            }
        }

        /**
         * Method called each time a method is accepted. It is also called at a scheduled, fixed, rate to release the collection semaphore for acceptors that do not know the
         * exact number of messages that they receive and therefore wait a while to determine that no more messages are coming in.
         */
        public void releaseSemaphoreIfDone() {
            if (checkMessages() != MessageIdentifier.MessageCheckResult.NOT_YET) {
                completed = true;
                collectionSemaphore.release();
            }
        }

        /**
         * Performs a release of the semaphore, no matter whether all messages have been received - this is for use when errors occurr.
         */
        public void forceRelease() {
            completed = true;
            collectionSemaphore.release();
        }

        @SneakyThrows(InterruptedException.class)
        public final List<T> getCollectedMessages() {
            if (collectionSemaphore.tryAcquire(waitingTimeMs, TimeUnit.MILLISECONDS)) {
                if (checkMessages() == MessageIdentifier.MessageCheckResult.OK) {
                    return acceptedMessages;
                }
                else {
                    fail();
                    return null;
                }
            }
            else {
                fail();
                return null;
            }
        }

        protected String getFailErrorMessage() {
            return "Error receiving expected messages: \nReceived:" + acceptedMessages
                    .stream()
                    .map(message -> message.getShortDescription())
                    .collect(Collectors.toList());
        }

        protected void fail() {
            Assert.fail(getFailErrorMessage());
        }

        protected boolean doAccept(T message) {
            return acceptPredicate.test(message);
        }

        /**
         * Checks the state of the received messages.
         */
        protected abstract MessageIdentifier.MessageCheckResult checkMessages();
    }

    /**
     * This class is provided as convenience for cases where some messages need to be accepted always. Most likely this only makes sense to use for the acceptor set to "accept"
     * any "unexpected" messages coming in (See {@link MessageCollector}.
     */
    public static class SimpleAcceptor<M extends Message> extends MessageCollector.Acceptor<M> {

        public SimpleAcceptor(Predicate<M> acceptPredicate) {
            super(0, acceptPredicate);
        }

        @Override
        protected MessageIdentifier.MessageCheckResult checkMessages() {
            return MessageIdentifier.MessageCheckResult.NOT_YET;
        }
    }
}
