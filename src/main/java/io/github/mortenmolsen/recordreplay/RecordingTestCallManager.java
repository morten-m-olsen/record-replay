package io.github.mortenmolsen.recordreplay;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import io.github.mortenmolsen.recordreplay.calls.CallWithMessages;
import io.github.mortenmolsen.recordreplay.messages.Message;
import io.github.mortenmolsen.recordreplay.messages.MessageSender;
import io.github.mortenmolsen.recordreplay.messages.WaitingInput;
import io.github.mortenmolsen.recordreplay.testdata.ArgumentStringGenerator;
import io.github.mortenmolsen.recordreplay.testdata.Record;
import io.github.mortenmolsen.recordreplay.testdata.TestDataForRecording;
import io.github.mortenmolsen.recordreplay.messages.MessageCollector;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Assume;

/**
 * A test call manager that records the responses received as well as any messages coming in as results of the calls.
 *
 * @author Morten Meiling Olsen
 */
@SuppressFBWarnings("MS_CANNOT_BE_FINAL")
public final class RecordingTestCallManager<M extends Message> extends TestCallManager<M> {

    public static final RunMode RUN_MODE = RunMode.ALL;

    private final TestDataForRecording testData;

    /**
     * These are the instances that have been registered as "real" objects that invocations can be made on. See {@link #registerRealInstance(Object)}.
     */
    private final Map<Class, Object> realInstances = new HashMap<>();

    private final MessageCollector<M> messageCollector;

    public RecordingTestCallManager(Class<? extends Record<M>> recordClass, MessageCollector<M> messageCollector, TestDataForRecording testData,
                                    ArgumentStringGenerator argumentStringGenerator, MessageSender<M> messageSender) {
        super(recordClass, argumentStringGenerator, messageSender);
        this.messageCollector = messageCollector;
        this.testData = testData;
    }

    @Override
    protected <Q> Q doSimpleCall(Callable<Q> callable, Class<Q> expectedReturnValueClass, Record<M> record) {
        try {
            Q result = callable.call();
            record.setResponse(result);
            testData.addRecord(record);
            return result;
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    @SneakyThrows({InstantiationException.class, IllegalAccessException.class})
    @Override
    protected Record<M> getRecord(String argumentsString) {
        Record<M> resultingTransaction = recordClass.newInstance();
        resultingTransaction.setRequest(argumentsString);
        return resultingTransaction;
    }

    @Override
    protected final Object doCall(Class invokedOn, Method method, Object[] args, Record<M> record) throws Throwable {
        if (isExcludedMethod(method)) {
            // just forward to real instance, ignore for the purpose of the framework
            return method.invoke(getRealInstance(invokedOn), args);
        }
        @Nullable Object result;
        try {
            if (currentCall instanceof CallWithMessages) {
                // the call is one that also must collect messages afterwards so set up the collector
                //noinspection unchecked
                messageCollector.startAccepting(((CallWithMessages)currentCall).waitingInput.getAcceptor());
            }
            result = method.invoke(getRealInstance(invokedOn), args);
        }
        catch (InvocationTargetException e) {
            result = e.getCause();
        }
        currentCall.record(result, record);
        testData.addRecord(record);
        return result;
    }

    //NOTE: this method requires that the collection of messages has been instigated from somewhere else.
    @Override
    protected List<M> collectMessages(WaitingInput<M> waitingInput, Record<M> record) {
        List<M> collectedMessages = messageCollector.getCollectedMessages();
        record.getMessages().addAll(collectedMessages);
        return collectedMessages;
    }

    @Override
    public List<M> getMessagesByWaitingFor(WaitingInput<M> input) throws Exception {
        if (currentCall != null) {
            Assert.fail("Cannot wait, a call is already in progress.");
        }
        Record<M> record = recordClass.newInstance();
        record.setRequest("--wait for external action--");
        testData.addRecord(record);
        messageCollector.startAccepting(input.getAcceptor());
        return collectMessages(input, record);
    }

    @Override
    public void init(Method testMethod) {
        verifyTestRun(testMethod);
    }

    private void verifyTestRun(Method testMethod) {
        switch (RUN_MODE) {
            case ALL:
                break;
            case FAST:
                if (testMethod.getAnnotation(SlowRecording.class) != null ||
                        testMethod.getAnnotation(DependsOnExternalAction.class) != null) {
                    Assume.assumeFalse("Test ignored since this is a fast run.", true);
                }
                break;
            case SLOW:
                if (testMethod.getAnnotation(SlowRecording.class) == null) {
                    Assume.assumeFalse("Test ignored, since this is an 'only-slow' run.", true);
                }
                break;
            case EXTERNAL:
                if (testMethod.getAnnotation(DependsOnExternalAction.class) == null) {
                    Assume.assumeFalse("Test ignored, since this is an 'only-external' run.", true);
                }
                break;
            case NON_EXTERNAL:
                if (testMethod.getAnnotation(DependsOnExternalAction.class) != null) {
                    Assume.assumeFalse("Test ignored, since this is a 'NO depends-on-external-action' run.", true);
                }
                break;
            case ONLY_MISSING:
                if (testData.recordFileExists()) {
                    Assume.assumeFalse("Test ignored, since this is a run recording only for tests with no test data yet.", true);
                }
                break;
            default:
                Assert.fail("Unknown mode: " + RUN_MODE);
        }
    }

    /**
     * Retrieves a "real" instance of the given class. A real instance means a non-wrapped, non-proxied object that
     * actually talks to the external world in some way.
     */
    public final <T> T getRealInstance(Class<T> clazz) {
        if (!realInstances.containsKey(clazz)) {
            Assert.fail("No real instance registered for class: " + clazz.getName());
        }
        return clazz.cast(realInstances.get(clazz));
    }

    /**
     * Registers a "real" (non-mocked and actually doing external communication when invoked) instance. Methods will be invoked on this.
     */
    public final void registerRealInstance(Object instance) {
        realInstances.put(instance.getClass(), instance);
        // also register the instance under any interfaces it implements
        for (Class<?> implementedInterface : instance.getClass().getInterfaces()) {
            realInstances.put(implementedInterface, instance);
        }
    }

    @SneakyThrows(InterruptedException.class)
    @Override
    public void validateAfterTest() {
        super.validateAfterTest();
        Thread.sleep(1000);
        if (messageCollector.getErrorMessage() != null) {
            throw new IllegalStateException("Message collector recorded error: " + messageCollector.getErrorMessage());
        }
    }

    /**
     * Marks a test that is "slow" during recording. This will likely be a test that wait for protection to trigger or something similar. It is nice to be able to ignore
     * these and make a "fast" run.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface SlowRecording {
    }

    /**
     * Marks a test that cannot be run (in recording) just as-is, but will require some external action (setting the account in a certain state prior to running or performing
     * some action during the run).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DependsOnExternalAction {
        /**
         * @return the action that must take place.
         */
        String action();
    }

    private enum RunMode {
        /**
         * Run all tests
         */
        ALL,
        /**
         * Run only "fast" tests.
         */
        FAST,
        /**
         * Run only "slow" tests.
         */
        SLOW,
        /**
         * Run only tests depending on external input.
         */
        EXTERNAL,

        /**
         * Run only tests that do NOT depend on external actions being made during the test.
         */
        NON_EXTERNAL,

        /**
         * Run only tests that have no test data yet.
         */
        ONLY_MISSING
    }
}
