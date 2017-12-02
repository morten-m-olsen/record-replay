package io.github.mortenmolsen.recordreplay;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Simple extension of the normal {@link AnnotationConfigContextLoader}. The only job of this class is to disallow
 * bean overriding - we do not want that - it is messy and leads to not really knowing which beans are in use.
 *
 * @author Morten Meiling Olsen
 */
public class TestContextLoader extends AnnotationConfigContextLoader {

    @Override
    protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
        context.setAllowBeanDefinitionOverriding(false);
        super.loadBeanDefinitions(context, mergedConfig);
    }
}
