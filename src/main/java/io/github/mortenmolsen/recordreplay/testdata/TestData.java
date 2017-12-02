package io.github.mortenmolsen.recordreplay.testdata;

import lombok.Getter;
import lombok.Setter;

/**
 * Base class for test data.
 *
 * @author Morten Meiling Olsen
 */
public abstract class TestData {

    @Getter
    @Setter
    protected TestDataHandler testDataHandler;

    protected String fileName;

    @Getter
    protected final String pathToTestData;

    protected TestData(String pathToTestData) {
        this.pathToTestData = pathToTestData;
    }

    public void init(String testClassName, String testMethodName) {
        this.fileName = testClassName + "_" + testMethodName + testDataHandler.getExtension();
    }

    /**
     * Called after a test to allow final stuff to be done with the data (record).
     */
    public abstract void finish();
}
