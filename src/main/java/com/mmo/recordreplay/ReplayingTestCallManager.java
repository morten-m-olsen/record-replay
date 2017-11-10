package com.mmo.recordreplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.recordreplay.messages.Message;
import com.mmo.recordreplay.messages.MessageIdentifier;
import com.mmo.recordreplay.messages.MessageSender;
import com.mmo.recordreplay.messages.WaitingInput;
import com.mmo.recordreplay.testdata.ArgumentStringGenerator;
import com.mmo.recordreplay.testdata.Record;
import com.mmo.recordreplay.testdata.TestDataForReplaying;
import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Test call manager that replays recorded calls from {@link Record}s, ensuring no communication with the actual target of the calls.
 *
 * @author Morten Meiling Olsen
 */
public final class ReplayingTestCallManager<M extends Message> extends TestCallManager<M> {

    protected final ObjectMapper objectMapper;
    private final TestDataForReplaying testData;

    public ReplayingTestCallManager(Class<? extends Record<M>> recordClass, TestDataForReplaying testData, ObjectMapper objectMapper,
                                    ArgumentStringGenerator argumentStringGenerator, MessageSender<M> messageSender) {
        super(recordClass, argumentStringGenerator, messageSender);
        this.testData = testData;
        this.objectMapper = objectMapper;
    }

    @Override
    protected <Q> Q doSimpleCall(Callable<Q> callable, Class<Q> expectedReturnValueClass, Record<M> record) {
        return testData.getTestDataHandler().convertValue(record.getResponse(), expectedReturnValueClass);
    }

    @Override
    protected Record<M> getRecord(String argumentsString) {
        if (!testData.hasData()) {
            Assert.fail("Call made, but none is recorded (perhaps you need to record test data).");
        }
        //noinspection unchecked
        Record<M> record = testData.consumeFirstRecord(recordClass);
        if (!argumentsString.equals(record.getRequest())) {
            Assert.fail("Call was made to:\n" + argumentsString + "\n, but the recorded call was:\n" + record.getRequest() + ".\n" +
                    " This is not necessarily an error, but it does require the data for this test to be re-recorded.");
        }
        return record;
    }

    @Override
    protected Object doCall(Class invokedOn, Method method, Object[] args, Record<M> record) throws Throwable {
        if (isExcludedMethod(method)) {
            // do nothing, call is "excluded" which means it is ignored in replay mode (nothing will have been recorded)
            return null;
        }
        else {
            return currentCall.replay(record, testData.getTestDataHandler());
        }
    }

    @Override
    protected List<M> collectMessages(WaitingInput<M> waitingInput, Record<M> record) {
        // verify messages and return them
        if (waitingInput == null) {
            if (record.getMessages().size() > 0) {
                Assert.fail("Were not expecting any message but got: " + record.getMessages()
                        .stream()
                        .map(Message::getShortDescription)
                        .collect(Collectors.joining(",")));
            }
            return Collections.emptyList();
        }
        List<M> received = new ArrayList<>();
        MessageIdentifier.MessageCheckResult currentResult = MessageIdentifier.MessageCheckResult.NOT_YET;
        for (M message : record.getMessages()) {
            if (waitingInput.getAcceptPredicate().test(message)) {
                received.add(message);
                currentResult = waitingInput.checkReceivedMessages(received);
                if (currentResult == MessageIdentifier.MessageCheckResult.TOO_MANY) {
                    Assert.fail("Got too many messages of type " + message);
                }
            }
            else {
                Assert.fail("Message not accepted by waiting input: " + message);
            }

        }
        if (currentResult != MessageIdentifier.MessageCheckResult.OK) {
            Assert.fail("Not all expected message were present in recording.");
        }
        return record.getMessages();
    }

    @Override
    protected List<M> getMessagesByWaitingFor(WaitingInput<M> input) throws Exception {
        return collectMessages(input, testData.consumeFirstRecord(recordClass));
    }

    @Override
    public void init(Method testMethod) {
        // nothing to initialize here.
    }
}
