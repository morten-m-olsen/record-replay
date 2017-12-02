package io.github.mortenmolsen.recordreplay.calls;

/**
 * A call that never returns anything (void).
 *
 * @author Morten Meiling Olsen
 */
public class VoidCall extends ExpectedCall<Void> {
    public VoidCall(String methodName) {
        super(methodName, Void.TYPE);
    }
}
