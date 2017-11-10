package com.mmo.recordreplay;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Rule that allows the {@link TestCallManager} a chance to report anything that might be wrong, thus potentially helping in identifying the error.
 * Additionally, the rule implements a retrying strategy on certain types of test failures.
 *
 * @author Morten Meiling Olsen
 */
public class TestCallManagerAwareTestRule implements TestRule {

    private final TestCallManager testCallManager;

    private int remainingRetries = 10;

    public TestCallManagerAwareTestRule(TestCallManager testCallManager) {
        this.testCallManager = testCallManager;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // NOTE: while loop is stopped "manually" on conditions inside
                while (true) {
                    try {
                        base.evaluate();
                    } catch (Throwable t) {
                        if (t instanceof RetryTriggeringRecordingException) {
                            if (remainingRetries > 0) {
                                remainingRetries--;
                                System.out.println("Got error: '" + t.getMessage() + "'. Retrying...(" + remainingRetries + " retries remaining).");
                                continue;
                            }
                        }
                        // give the test call manager a chance of throwing any exception it might have caught
                        try {
                            testCallManager.validateAfterTest();
                        } catch (IllegalStateException e) {
                            throw new IllegalStateException(e.getMessage(), t);
                        }
                        throw t;
                    }
                    // give the test call manager a chance of throwing any exception it might have caught
                    testCallManager.validateAfterTest();

                    // the normal case - the while loop is broken here as we only retry when the above is the case
                    break;
                }
            }
        };
    }
}
