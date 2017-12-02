package io.github.mortenmolsen.recordreplay.conversion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;

import io.github.mortenmolsen.recordreplay.ReplayingTestCallManager;
import io.github.mortenmolsen.recordreplay.testdata.TestData;

/**
 * This class is used to do conversions of the recorded data files. It is simply too cumbersome to manually edit them, and re-recording all tests is also very annoying
 * (they have external dependencies, timing etc). Therefore, this class exists and can be hooked up to the {@link ReplayingTestCallManager},
 * thus making it convert the messages in the way defined in the {@link #doConvert(Object, Class)} method of this class.
 *
 * @author Morten Meiling Olsen
 */
public abstract class OldFormatConverter {

    protected final LinkedList<Object> convertedRecords = new LinkedList<>();
    private final TestData testData;

    public OldFormatConverter(TestData testData) {
        this.testData = testData;
    }

    /**
     * Converts the given object into a new representation, if applicable. Also stores the new object so that it can be saved after the test run is complete.
     *
     * @return the potentially converted object.
     */
    public Object convert(Object old, Class expectedClassOfObject) {
        Object convertedObject = doConvert(old, expectedClassOfObject);
        convertedRecords.add(convertedObject);
        return convertedObject;
    }

    /**
     * Implemented by subclasses to do the actual conversion.
     */
    protected abstract Object doConvert(Object old, Class expectedClassOfObject);

    public void record(String fileName) {
        try {
            Path pathToFile = Paths.get(testData.getPathToTestData(), fileName);
            if (Files.exists(pathToFile)) {
                Files.delete(pathToFile);
            }

            // first write NUL char to make git recognise file as binary
            Files.write(pathToFile, new byte[]{0});
            Files.write(pathToFile, testData.getTestDataHandler().serialize(convertedRecords), StandardOpenOption.APPEND);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
