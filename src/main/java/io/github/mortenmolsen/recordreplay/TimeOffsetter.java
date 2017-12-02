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
        Long callResult = testCallManager.simpleCall(() -> System.currentTimeMillis(), Long.TYPE, CALL_IDENTIFIER);
        //TODO(mmo): fix the below - rerecord...
        // NOTE: since I did not want to re-record everything, a value of -1 was hacked into the existing test data - this makes the offsetter not really do anything.
        // If time offsetting is ever really needed in, for instance, the broker tests - they need to be re-recorded
        baseOffset = callResult != -1 ? Math.abs(callResult - System.currentTimeMillis()) : 0;
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
