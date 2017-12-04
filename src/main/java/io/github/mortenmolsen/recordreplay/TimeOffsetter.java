package io.github.mortenmolsen.recordreplay;

import java.util.concurrent.TimeUnit;

/**
 * @author Morten Meiling Olsen
 */
public class TimeOffsetter {

    public static final String CALL_IDENTIFIER = "timeOffsetterCurrentTimeMillis";

    /**
     * The base offset is used to make the time returned during replaying match the time during recording.
     */
    private long baseOffset = 0;

    /**
     * The extra offset can be used to simulate a later or earlier time, see {@link #addOffset(TimeUnit, int)}.
     */
    private long extraOffset = 0;

    public void init(TestCallManager<?> testCallManager) {
        Long callResult = testCallManager.simpleCall(System::currentTimeMillis, Long.TYPE, CALL_IDENTIFIER);
        baseOffset = Math.abs(callResult - System.currentTimeMillis());
    }

    /**
     * Adds the offset specified to the time returned.
     */
    public void addOffset(TimeUnit timeUnit, int value) {
        extraOffset += timeUnit.toMillis(value);
    }

    /**
     * @return the time after applying any offsets.
     */
    public long getTime() {
        return System.currentTimeMillis() - baseOffset + extraOffset;
    }
}
