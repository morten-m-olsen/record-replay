package com.mmo.recordreplay.messages;

import com.mmo.recordreplay.TestCallManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * This class is responsible for setting up the calls expected to result from the processing of messages.
 * This has been separated out into it's own class simply because having to specify this in the tests gets too cumbersome.
 *
 * @author Morten Meiling Olsen
 */
public abstract class ExpectedCallsFromMessages<M extends Message> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    @Lazy
    protected TestCallManager<M> testCallManager;

    /**
     * Sets up which calls will result from processing the given message.
     */
    public abstract void setUpExpectations(M message);

    /**
     * To be used for implementations where messages never result in calls being made.
     */
    public static class NoOpExpectedCallsFromMessages<M extends Message> extends ExpectedCallsFromMessages<M> {

        @Override
        public void setUpExpectations(M message) {
            // do nothing
        }
    }
}
