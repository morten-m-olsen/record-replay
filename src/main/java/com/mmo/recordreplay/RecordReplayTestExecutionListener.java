package com.mmo.recordreplay;

import com.mmo.recordreplay.testdata.TestData;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Execution listener that ensures that the {@link TestData} and the {@link TestCallManager} get initialized and finalized.
 *
 * @author Morten Meiling Olsen
 */
public class RecordReplayTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        testContext.getApplicationContext().getBean(TestData.class).init(testContext.getTestClass().getSimpleName(), testContext.getTestMethod().getName());
        TestCallManager testCallManager = testContext.getApplicationContext().getBean(TestCallManager.class);
        testCallManager.init(testContext.getTestMethod());
        testContext.getApplicationContext().getBean(TimeOffsetter.class).init(testCallManager);
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        testContext.getApplicationContext().getBean(TestData.class).finish();
    }
}
