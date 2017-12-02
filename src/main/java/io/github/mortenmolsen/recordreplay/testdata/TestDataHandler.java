package io.github.mortenmolsen.recordreplay.testdata;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * How to handle test data, ie. write, read etc.
 *
 * @author Morten Meiling Olsen
 */
public interface TestDataHandler {

    byte[] serialize(List<Object> objects) throws IOException;

    List<Object> deserialize(InputStream inputStream) throws IOException;

    <T> T convertValue(Object value, Class<T> newClass);

    String getExtension();
}
