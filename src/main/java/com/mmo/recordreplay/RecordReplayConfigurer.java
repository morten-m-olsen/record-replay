package com.mmo.recordreplay;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.recordreplay.messages.Message;
import com.mmo.recordreplay.testdata.ArgumentStringGenerator;
import com.mmo.recordreplay.testdata.Record;
import com.mmo.recordreplay.messages.MessageCollector;
import com.mmo.recordreplay.testdata.TestData;

/**
 * A configurer used by the {@link RecordReplayTestSpringConfiguration} to customize the beans.
 * One of these must be implemented when working with the RR framework.
 *
 * @author Morten Meiling Olsen
 */
public abstract class RecordReplayConfigurer<M extends Message> {

    public abstract String getPathToData();

    public abstract Class<? extends Record<M>> getRecordClass();

    protected void configureRecordingTestCallManager(RecordingTestCallManager<M> recordingTestCallManager) {
        // does nothing by default, can be overwritten to add real instances that the test call manager needs to know about
    }

    protected void configureMessageCollector(MessageCollector<M> messageCollector) {
        // do nothing, can be overwritten to set an "unexpected" message acceptor - ie. which unexpected messages are "ok"
    }

    protected abstract void configureTestData(TestData testData);

    protected void configureArgumentStringGenerator(ArgumentStringGenerator argumentStringGenerator) {
        // no extra configuration done by default
    }

    /**
     * Sub classes may override this to further configure the object mapper, like adding MixIns to customize serialization of 3rd party API objects
     * NOTE: subclasses should call super!
     * @param objectMapper the object mapper
     */
    protected void configureObjectMapper(ObjectMapper objectMapper) {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
