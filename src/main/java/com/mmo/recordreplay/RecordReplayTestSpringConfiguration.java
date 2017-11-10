package com.mmo.recordreplay;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.recordreplay.messages.Message;
import com.mmo.recordreplay.messages.MessageSender;
import com.mmo.recordreplay.testdata.ArgumentStringGenerator;
import com.mmo.recordreplay.testdata.TestDataForRecording;
import com.mmo.recordreplay.testdata.TestDataForReplaying;
import com.mmo.recordreplay.messages.MessageCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/**
 * The base class for record/replay tests. Ensures the presence of test data and other common beans.
 *
 * @author Morten Meiling Olsen
 */
@Order(100)
@Configuration
public class RecordReplayTestSpringConfiguration<M extends Message>  {

    @Autowired
    private RecordReplayConfigurer<M> configurer;

    @Bean
    @Profile(RRProfiles.REPLAY)
    public ReplayingTestCallManager<M> replayingTestCallManager(
            TestDataForReplaying testData, ObjectMapper objectMapper,
            ArgumentStringGenerator argumentStringGenerator, MessageSender<M> messageSender) throws IOException {
        return new ReplayingTestCallManager<>(configurer.getRecordClass(), testData, objectMapper, argumentStringGenerator, messageSender);
    }

    @Bean
    @Profile(RRProfiles.RECORD)
    public RecordingTestCallManager<M> recordingTestCallManager(MessageSender<M> messageSender) {
        RecordingTestCallManager<M> recordingTestCallManager = new RecordingTestCallManager<>(configurer.getRecordClass(), messageCollector(), recordingTestData(),
                argumentStringGenerator(), messageSender);
        configurer.configureRecordingTestCallManager(recordingTestCallManager);
        return recordingTestCallManager;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        configurer.configureObjectMapper(objectMapper);
        return objectMapper;
    }

    @Bean
    @Profile(RRProfiles.RECORD)
    public MessageCollector<M> messageCollector() {
        MessageCollector<M> messageCollector = new MessageCollector<>();
        configurer.configureMessageCollector(messageCollector);
        return messageCollector;
    }

    @Bean
    @Profile(RRProfiles.REPLAY)
    public TestDataForReplaying replayTestData() {
        TestDataForReplaying testData = new TestDataForReplaying(configurer.getPathToData());
        configurer.configureTestData(testData);
        return testData;
    }

    @Bean
    @Profile(RRProfiles.RECORD)
    public TestDataForRecording recordingTestData() {
        TestDataForRecording testData = new TestDataForRecording(configurer.getPathToData());
        configurer.configureTestData(testData);
        return testData;
    }

    @Bean
    public ArgumentStringGenerator argumentStringGenerator() {
        ArgumentStringGenerator argumentStringGenerator = new ArgumentStringGenerator();
        configurer.configureArgumentStringGenerator(argumentStringGenerator);
        return argumentStringGenerator;
    }

    @Bean
    public TestCallManagerAwareTestRule testCallManagerAwareTestRule(TestCallManager<M> testCallManager) {
        return new TestCallManagerAwareTestRule(testCallManager);
    }

    @Bean
    public TimeOffsetter timeOffsetter() {
        return new TimeOffsetter();
    }
}
