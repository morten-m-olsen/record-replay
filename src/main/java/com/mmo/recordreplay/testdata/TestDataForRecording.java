package com.mmo.recordreplay.testdata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import lombok.Synchronized;

/**
 * The test data collected during recording.
 *
 * @author Morten Meiling Olsen
 */
public class TestDataForRecording extends TestData {

    private final List<Object> recordsToWrite = new ArrayList<>();

    public TestDataForRecording(String pathToTestData) {
        super(pathToTestData);
    }

    @Synchronized
    @SneakyThrows(IOException.class)
    @Override
    public void finish() {
        // save the data in a file
        Path pathToFile = Paths.get(pathToTestData, fileName);

        if (Files.exists(pathToFile)) {
            Files.delete(pathToFile);
        }

        // first write NUL char to make git recognise file as binary
        Files.write(pathToFile, new byte[]{0});
        Files.write(pathToFile, testDataHandler.serialize(recordsToWrite), StandardOpenOption.APPEND);
    }

    @Synchronized
    public boolean recordFileExists() {
        return Files.exists(Paths.get(pathToTestData, fileName));
    }

    /**
     * Can be called by anyone from the outside to add a record to those that will be stored.
     */
    @Synchronized
    public void addRecord(Object record) {
        recordsToWrite.add(record);
    }
}