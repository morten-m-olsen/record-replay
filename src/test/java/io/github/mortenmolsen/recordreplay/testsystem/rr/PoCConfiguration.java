package io.github.mortenmolsen.recordreplay.testsystem.rr;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mortenmolsen.recordreplay.*;
import io.github.mortenmolsen.recordreplay.messages.MessageSender;
import io.github.mortenmolsen.recordreplay.messages.NoOpMessageSender;
import io.github.mortenmolsen.recordreplay.testdata.ObjectMapperTestDataHandler;
import io.github.mortenmolsen.recordreplay.testdata.Record;
import io.github.mortenmolsen.recordreplay.testdata.TestData;
import io.github.mortenmolsen.recordreplay.testsystem.DefaultExternalConnection;
import io.github.mortenmolsen.recordreplay.testsystem.ExternalConnection;
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
        return new File("src/test/resources/io/github/mortenmolsen/recordreplay").getAbsolutePath();
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
