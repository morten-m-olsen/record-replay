package io.github.mortenmolsen.recordreplay.testdata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

import javax.annotation.Nullable;

import io.github.mortenmolsen.recordreplay.conversion.OldFormatConverter;
import lombok.Setter;

/**
 * The data replayed during a test.
 *
 * @author Morten Meiling Olsen
 */
public class TestDataForReplaying extends TestData {

    protected final LinkedList<Object> records = new LinkedList<>();

    /**
     * If conversion of old record is desired, this converter must be set.
     */
    @Setter
    private @Nullable
    OldFormatConverter converter;

    public TestDataForReplaying(String pathToTestData) {
        super(pathToTestData);
    }

    @Override
    public void init(String testClassName, String testMethodName) {
        super.init(testClassName, testMethodName);
        try {
            try(InputStream inputStream = Files.newInputStream(Paths.get(pathToTestData, fileName))) {
                // first read the single NUL char written during recording to fool git into classifying the file as binary
                //noinspection ResultOfMethodCallIgnored
                inputStream.read();
                records.addAll(testDataHandler.deserialize(inputStream));
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Error loading file: " + fileName + ". Most likely you need to record data first.");
        }
    }

    public boolean hasData() {
        return !records.isEmpty();
    }

    /**
     * Retrieves a record. The record must be of the given class, or the conversion will fail.
     */
    public <K> K consumeFirstRecord(Class<K> expectedClass) {
        Object objectToConvert = records.removeFirst();
        if (converter != null) {
            // if an old format converter is in effect, we need to let it do its magic first
            objectToConvert = converter.convert(objectToConvert, expectedClass);
        }
        return testDataHandler.convertValue(objectToConvert, expectedClass);
    }

    @Override
    public void finish() {
        // if a converter is in effect, make it write the converted records
        if (converter != null) {
            converter.record(fileName);
        }
    }
}
