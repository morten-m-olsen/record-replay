package com.mmo.recordreplay.calls;

import com.mmo.recordreplay.TestCallManager;

/**
 * This call is handled in a special way in the {@link TestCallManager} allowing multiple of these calls to occur in succession (or none at all). It is used for when a call must
 * poll some information an unknown number of times.
 *
 * @author Morten Meiling Olsen
 */
public class WildcardCall<T> extends ExpectedCall<T> {

    public WildcardCall(String methodName, Class<T> expectedResponseClass) {
        super(methodName, expectedResponseClass);
    }
}
