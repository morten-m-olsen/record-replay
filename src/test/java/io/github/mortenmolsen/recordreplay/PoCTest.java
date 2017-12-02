package io.github.mortenmolsen.recordreplay;

import io.github.mortenmolsen.recordreplay.calls.ExpectedCall;
import io.github.mortenmolsen.recordreplay.testsystem.rr.PoCConfiguration;
import io.github.mortenmolsen.recordreplay.testsystem.rr.PoCMessage;
import io.github.mortenmolsen.recordreplay.testsystem.DefaultExternalConnection;
import io.github.mortenmolsen.recordreplay.testsystem.ExternalConnection;
import io.github.mortenmolsen.recordreplay.testsystem.Post;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * This is a simple test with a very basic test system that demonstrates how the framework can be used.
 * <br/>
 * The test "system" consists basically of the {@link ExternalConnection} interface and the
 * implementation {@link DefaultExternalConnection}. This test then shows how the connection
 * can be mocked so that it, in recording mode, records the data returned, and, in replaying mode, replays it.
 * <br/>
 * Naturally, this is extremely simplified and normally the test would not work directly on the connection but on some
 * other system components on which proper assertions could be made (these components would then use the external
 * connection somewhere in their logic).
 *
 * TODO: also add example of handling of messages coming in as a result of the call.
 *
 * @author Morten Meiling Olsen
 */
@ActiveProfiles(RRProfiles.REPLAY)
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class, classes = PoCConfiguration.class)
@TestExecutionListeners({DirtiesContextTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, RecordReplayTestExecutionListener.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class PoCTest {

    @Autowired
    private ExternalConnection externalConnection;

    @Autowired
    protected TestCallManager<PoCMessage> testCallManager;

    @Test
    public void pocTest() throws Exception {
        // we need to tell the test data manager what call to expect - in this way we also verify that no unexpected
        // calls are made and can have complete control of our external communication
        testCallManager.expectCall(new ExpectedCall<>("getTestPost", Post.class));
        Post testPost = externalConnection.getTestPost();
    }

}
