package io.github.mortenmolsen.recordreplay.testsystem;

import lombok.Data;

/**
 * @author Morten Meiling Olsen
 */
@Data
public class Post {
    long userId;
    long id;
    String title;
    String body;
}
