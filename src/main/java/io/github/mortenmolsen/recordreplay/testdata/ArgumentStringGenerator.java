package io.github.mortenmolsen.recordreplay.testdata;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generates argument strings for use with the recorded test data.
 *
 * @author Morten Meiling Olsen
 */
public class ArgumentStringGenerator {

    /**
     * String that represents arguments that should be ignored.
     */
    private static final String IGNORE_STRING = "IGNORE_STRING";

    /**
     * Generator function to use for classes that we don't want to include in the arguments string.
     */
    private static final Function<Object, String> IGNORE_GENERATOR = arg -> IGNORE_STRING;

    /**
     * The argument part generators. They reside in a linkedlist so that the most specific can be added first.
     */
    private final LinkedList<ArgumentPartGenerator> argumentPartGenerators = new LinkedList<>();

    public ArgumentStringGenerator() {

        // the base generator for all objects simply calls toString on them
        addGenerator(Object.class, arg -> arg.toString());

        // instants are always excluded
        ignoreInArgumentStrings(Instant.class);
    }

    /**
     * Adds the given generator for the given class. IMPORTANT: add the least specific generators first.
     */
    public void addGenerator(Class<?> clazz, Function<Object, String> argumentStringFunction) {
        argumentPartGenerators.addFirst(new ArgumentPartGenerator(clazz, argumentStringFunction));
    }

    /**
     * Call to ensure that the given class is ignored in argument strings (not included). IMPORTANT: remember the call order. If a generator is added later
     * that also matches the class, it will generate a string for it.
     */
    public void ignoreInArgumentStrings(Class<?> clazz) {
        addGenerator(clazz, IGNORE_GENERATOR);
    }

    public String generateString(Object[] args) {
        return Arrays.asList(args)
                .stream()
                .map(arg -> getArgumentPartString(arg))
                // we remove the argument part strings that are specifically set to the ignore string by the ignore generator
                .filter(generatedString -> !generatedString.equals(IGNORE_STRING))
                .collect(Collectors.joining(", "));
    }

    private String getArgumentPartString(Object argument) {
        for (ArgumentPartGenerator argumentPartGenerator : argumentPartGenerators) {
            Optional<String> argumentPartString = argumentPartGenerator.generateIfPossible(argument);
            if (argumentPartString.isPresent()) {
                return argumentPartString.get();
            }
        }
        throw new IllegalStateException("Argument generation must be possible for all objects.");
    }

    private static class ArgumentPartGenerator {

        private final Class<?> forClass;
        private final Function<Object, String> argumentStringFunction;

        public ArgumentPartGenerator(Class<?> forClass, Function<Object, String> argumentStringFunction) {
            this.forClass = forClass;
            this.argumentStringFunction = argumentStringFunction;
        }

        public Optional<String> generateIfPossible(Object arg) {
            return Optional.ofNullable(forClass.isAssignableFrom(arg.getClass()) ?
                    argumentStringFunction.apply(arg) : null);
        }
    }
}
