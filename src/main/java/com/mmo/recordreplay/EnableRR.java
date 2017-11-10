package com.mmo.recordreplay;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Add this annotation to a configuration to enable RR (Record-Replay). Will give access to the basic beans needed to implement an RR solution, most notably
 * the {@link TestCallManager}s.
 * Note that if doing so, a {@link RecordReplayConfigurer} must also be implemented.
 *
 * @author Morten Meiling Olsen
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(RecordReplayTestSpringConfiguration.class)
public @interface EnableRR {
}
