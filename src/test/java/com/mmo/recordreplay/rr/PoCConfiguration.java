package com.mmo.recordreplay.rr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.recordreplay.*;
import com.mmo.recordreplay.messages.MessageSender;
import com.mmo.recordreplay.messages.NoOpMessageSender;
import com.mmo.recordreplay.testdata.ObjectMapperTestDataHandler;
import com.mmo.recordreplay.testdata.Record;
import com.mmo.recordreplay.testdata.TestData;
import com.mmo.recordreplay.testsystem.DefaultExternalConnection;
import com.mmo.recordreplay.testsystem.ExternalConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;

/**
 * @author Morten Meiling Olsen
 */
@EnableRR
@Configuration
@Profile({RRProfiles.RECORD, RRProfiles.REPLAY})
public class PoCConfiguration extends RecordReplayConfigurer<PoCMessage> {

    @Override
    public String getPathToData() {
        return new File("src/test/resources/com/mmo/recordreplay").getAbsolutePath();
    }

    @Override
    public Class<? extends Record<PoCMessage>> getRecordClass() {
        return PoCRecord.class;
    }

    @Override
    protected void configureTestData(TestData testData) {
        testData.setTestDataHandler(new ObjectMapperTestDataHandler(new ObjectMapper()));
    }

    @Bean
    public MessageSender<PoCMessage> messageSender() {
        return new NoOpMessageSender<>();
    }

    @Bean
    public ExternalConnection connection(TestCallManager<PoCMessage> testCallManager) {
        return testCallManager.createRRMock(ExternalConnection.class);
    }

    @Override
    protected void configureRecordingTestCallManager(RecordingTestCallManager<PoCMessage> recordingTestCallManager) {
        super.configureRecordingTestCallManager(recordingTestCallManager);
        recordingTestCallManager.registerRealInstance(new DefaultExternalConnection());
    }
}
