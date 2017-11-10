package com.mmo.recordreplay.testdata;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A test data handler that uses an object mapper.
 *
 * @author Morten Meiling Olsen
 */
public class ObjectMapperTestDataHandler implements TestDataHandler {

    private final ObjectMapper objectMapper;

    public ObjectMapperTestDataHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(List<Object> objects) throws IOException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(objects);
    }

    @Override
    public List<Object> deserialize(InputStream inputStream)  throws IOException {
        return objectMapper.readValue(inputStream, new TypeReference<LinkedList<Object>>() { });
    }

    @Override
    public <T> T convertValue(Object value, Class<T> newClass) {
        return objectMapper.convertValue(value, newClass);
    }

    @Override
    public String getExtension() {
        return ".json";
    }
}
