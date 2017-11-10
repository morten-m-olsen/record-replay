package com.mmo.recordreplay.conversion;

import com.mmo.recordreplay.testdata.TestData;

/**
 * Converter only used to force a re-serialization of the file.
 *
 * @author Morten Meiling Olsen
 */
public class DoNothingConverter extends OldFormatConverter {

    public DoNothingConverter(TestData testDataForReplaying) {
        super(testDataForReplaying);
    }

    @Override
    protected Object doConvert(Object old, Class expectedClassOfObject) {
        return old;
    }
}
