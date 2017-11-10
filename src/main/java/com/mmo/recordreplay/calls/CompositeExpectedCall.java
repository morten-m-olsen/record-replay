package com.mmo.recordreplay.calls;

import com.mmo.recordreplay.testdata.Record;
import com.mmo.recordreplay.testdata.TestDataHandler;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * An expected call that is in reality just multiple expected calls.
 *
 * @author Morten Meiling Olsen
 */
public final class CompositeExpectedCall extends ExpectedCall<Void> {

    @Getter
    private final List<? extends ExpectedCall> calls;

    public CompositeExpectedCall(List<? extends ExpectedCall> calls) {
        super("composite", null);
        this.calls = calls;
    }

    public CompositeExpectedCall(ExpectedCall... calls) {
        super("composite", null);
        this.calls = Arrays.asList(calls);
    }

    @Override
    public void record(@Nullable Object result, Record<?> record) {
        throw new IllegalStateException("May not be called.");
    }

    @Override
    public Void replay(Record<?> record, TestDataHandler testDataHandler) {
        throw new IllegalStateException("May not be called.");
    }
}
