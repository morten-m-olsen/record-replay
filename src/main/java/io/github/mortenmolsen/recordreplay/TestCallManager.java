package io.github.mortenmolsen.recordreplay;

import io.github.mortenmolsen.recordreplay.calls.CallWithMessages;
import io.github.mortenmolsen.recordreplay.calls.CompositeExpectedCall;
import io.github.mortenmolsen.recordreplay.calls.ExpectedCall;
import io.github.mortenmolsen.recordreplay.calls.WildcardCall;
import io.github.mortenmolsen.recordreplay.messages.Message;
import io.github.mortenmolsen.recordreplay.messages.MessageSender;
import io.github.mortenmolsen.recordreplay.messages.WaitingInput;
import io.github.mortenmolsen.recordreplay.testdata.ArgumentStringGenerator;
import io.github.mortenmolsen.recordreplay.testdata.Record;
import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * A test call manager ensures that tests receive the data that they would get from making a call to some object.
 * It may do this by actually forwarding to the real object or by replaying recorded data.
 * The "object" will likely be something that retrieves remote data - the test call manager ensures that, during replaying, no remote calls are made.
 *
 * @param <M> The type of message that the manager uses.
 *
 * @author Morten Meiling Olsen
 */
public abstract class TestCallManager<M extends Message> {

    /**
     * Contains the arguments of the last call made. Tests might want to examine this.
     */
    @Getter
    private List<Object> argsOfLastCall;

    /**
     * A stack of the currently expected calls.
     */
    protected final Stack<ExpectedCall<?>> expectedCalls = new Stack<>();

    /**
     * The call currently being processed.
     */
    protected ExpectedCall<?> currentCall;

    /**
     * This specifies which methods should be ignored as far as the RR framework goes.
     * This is useful when wrapping objects that have certain methods that are not an actual proxy to an external call but rather configuration of the object necessary to make
     * it able to talk to the external world.
     */
    private final Set<Method> excludedMethods = new HashSet<>();

    @Getter
    private final List<ExpectedCall> completedCalls = new ArrayList<>();
    protected final Class<? extends Record<M>> recordClass;
    private final ArgumentStringGenerator argumentStringGenerator;
    protected final MessageSender<M> messageSender;

    @Setter
    private Runnable optionalPostCallProcessing;

    public TestCallManager(Class<? extends Record<M>> recordClass, ArgumentStringGenerator argumentStringGenerator, MessageSender<M> messageSender) {
        this.recordClass = recordClass;
        this.argumentStringGenerator = argumentStringGenerator;
        this.messageSender = messageSender;
    }

    /**
     *
     * @param testMethod the test method about to be executed.
     */
    public abstract void init(Method testMethod);

    /**
     * Puts a {@link ExpectedCall} on top of the call stack. This essentially tells the manager which call must be coming in next.
     */
    public void expectCall(ExpectedCall<?> expectedCall) {
        if (expectedCall instanceof CompositeExpectedCall) {
            // special call that is really just a container for multiple actual calls
            ((CompositeExpectedCall) expectedCall).getCalls()
                    .forEach(call -> expectCall(call));
        }
        else {
            expectedCalls.push(expectedCall);
        }
    }

    /**
     * This method makes it possible to simply get the result of a callable, in recording mode, it will record and return the result, in replay it will replay the result.
     * The callable is assumed to have no "side-effect", meaning any messages to also record, as this is not handled.
     */
    public final <Q> Q simpleCall(Callable<Q> callable, Class<Q> expectedReturnValueClass, String callIdentifier) {
        if (currentCall != null) {
            Assert.fail("A call is already in progress.");
        }
        argsOfLastCall = null;
        return doSimpleCall(callable, expectedReturnValueClass, getRecord(callIdentifier));
    }

    protected abstract  <Q> Q doSimpleCall(Callable<Q> callable, Class<Q> expectedReturnValueClass, Record<M> record);

    /**
     * Does preliminary and final work around the actual call ({@link #doCall(Class, Method, Object[], Record)} ).
     */
    public final <Q> Q call(Class invokedOn, Method method, Object[] args, Class<Q> returnValueClass) throws Throwable {
        if (isExcludedMethod(method)) {
            // excluded method - none of the logic below applies, but the subclass must be allowed to handle (it must be forwarded to the real instance in recording mode)
            //noinspection unchecked
            return (Q) doCall(invokedOn, method, args, null);
        }

        argsOfLastCall = args != null ? Collections.unmodifiableList(Arrays.asList(args)) : null;
        String methodName = method.getName();

        if (expectedCalls.empty()) {
            // we might have a wildcard call...
            if (getPreviousCall() instanceof WildcardCall) {
                try {
                    getPreviousCall().validatePendingCall(methodName, returnValueClass);

                    // ... indeed we have a wildcard call so add it and re-invoke method
                    expectCall(getPreviousCall());
                    return call(invokedOn, method, args, returnValueClass);
                } catch (ExpectedCall.CallValidationException e) {
                    //... not a wildcard call, so do nothing, it will fail below
                }
            }
            Assert.fail("Call made to: " + methodName + ", but no call is expected.");
        }
        if (currentCall != null) {
            Assert.fail("A call is already in progress.");
        }
        currentCall = expectedCalls.pop();

        try {
            currentCall.validatePendingCall(methodName, returnValueClass);
        } catch (ExpectedCall.CallValidationException e) {
            if (currentCall instanceof WildcardCall) {
                // the current call is a wildcard call - so we must assume that it was not called anyway, just recall method
                currentCall = null;
                return call(invokedOn, method, args, returnValueClass);
            }
            else if (getPreviousCall() instanceof WildcardCall) {
                // the previous call was a wildcard call, so test if this is another occurrence of that call
                try {
                    getPreviousCall().validatePendingCall(methodName, returnValueClass);

                    // this is another call to the wildcard call, put the just popped call back on the stack and add the wildcard call again
                    expectedCalls.push(currentCall);
                    expectedCalls.push(getPreviousCall());
                    currentCall = null;
                    return call(invokedOn, method, args, returnValueClass);
                } catch (ExpectedCall.CallValidationException e2) {
                    // not a wildcard call, it will fail below
                }
            }
            Assert.fail(e.getMessage());
        }
        Record<M> record = getRecord(args != null && args.length > 0 ? method.getName() + ": " + argumentStringGenerator.generateString(args) : method.getName());
        boolean callValidationFailed = false;
        try {
            Object result = doCall(invokedOn, method, args, record);
            currentCall.validateResult(result);

            //NOTE: it looks weird that we have to extract the optional and cannot inline it, but for some extremely strange reason, the compiler does not like that
            Optional<ExpectedCall> optional = currentCall.resultsIn(result);
            optional.ifPresent(this::expectCall);

            if (result instanceof Throwable) {
                throw (Throwable) result;
            } else {
                //noinspection unchecked
                return (Q) result;
            }
        } catch (ExpectedCall.CallValidationException e) {
            callValidationFailed = true;
            Assert.fail(e.getMessage());
            return null;

        } finally {
            completedCalls.add(currentCall);
            currentCall = null;
            if (!callValidationFailed) {
                if (getPreviousCall() instanceof CallWithMessages) {
                    //noinspection unchecked
                    collectMessages(((CallWithMessages<Q,M>) getPreviousCall()).waitingInput, record).forEach(message -> messageSender.addToQueue(message));
                }
                if (optionalPostCallProcessing != null) {
                    optionalPostCallProcessing.run();
                }
            }
        }
    }

    private ExpectedCall<?> getPreviousCall() {
        return completedCalls.size() > 0 ? completedCalls.get(completedCalls.size() - 1) : null;
    }

    /**
     * Collects and returns the messages requested by the input.
     */
    protected abstract List<M> collectMessages(WaitingInput<M> waitingInput, Record<M> record);

    /**
     * @param argumentsString the generated string that identifies the call arguments.
     * @return the record that is to be used by the next call.
     */
    protected abstract Record<M> getRecord(String argumentsString);

    /**
     * Performs the actual call of the method with the given arguments.
     */
    protected abstract Object doCall(Class invokedOn, Method method, Object[] args, Record<M> record) throws Throwable;

    /**
     * Waits for some messages based on the input and forwards the messages to the sender queue.
     */
    public final void waitFor(WaitingInput<M> input) throws Exception {
        getMessagesByWaitingFor(input).forEach(message -> messageSender.addToQueue(message));
    }

    protected abstract List<M> getMessagesByWaitingFor(WaitingInput<M> input) throws Exception;

    /**
     * Creates a mock of the given class that forwards calls into this test call manager for recording/replaying.
     * For this to work in recording, a real instance of the given class must be registered with the {@link RecordingTestCallManager#registerRealInstance(Object)} method.
     * In replaying, this is not necessary since it will get its result directly from the recorded data.
     */
    public final <Q> Q createRRMock(Class<Q> clazz) {
        return Mockito.mock(clazz, (Answer) invocation -> {
            if (invocation.getMethod().getName().equals("hashCode") || invocation.getMethod().getName().equals("equals")) {
                throw new RuntimeException("Cannot call: " + invocation.getMethod().getName() + " on mock.");
            } else if (invocation.getMethod().getName().equals("toString")) {
                return clazz.getName() + " mock";
            }
            return call(clazz, invocation.getMethod(), invocation.getArguments(), invocation.getMethod().getReturnType());
        });
    }

    /**
     * Excludes a method from being handled by all the RR logic. See {@link #excludedMethods}.
     */
    public void excludeMethod(Method method) {
        if (!method.getReturnType().getName().equals("void")) {
            // since excluded methods are ignored in replaying mode, we cannot allow them to return anything other than void as that result would otherwise be unavailable when
            // replaying
            throw new IllegalStateException("Cannot exclude non-void methods.");
        }
        excludedMethods.add(method);
    }

    /**
     * @return whether the given method should be excluded by the normal RR logic.
     */
    protected boolean isExcludedMethod(Method method) {
        return excludedMethods.contains(method);
    }

    /**
     * Allows the manager to do some validation when a run is complete.
     */
    public void validateAfterTest() {
        // non-wildcard calls are not allowed to be left after the test. Wildcard calls are by their nature allowed, since they represent a "possible" call
        List<ExpectedCall<?>> callThatShouldHaveBeenMade = expectedCalls
                .stream()
                .filter(expectedCall -> !(expectedCall instanceof WildcardCall))
                .collect(Collectors.toList());
        if (callThatShouldHaveBeenMade.size() > 0) {
            throw new IllegalStateException("Some expected calls were not made: "
                    + callThatShouldHaveBeenMade
                    .stream()
                    .map(expectedCall -> expectedCall.methodName)
                    .collect(Collectors.joining(", ")));
        }
    }
}
