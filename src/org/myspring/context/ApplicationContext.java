package org.myspring.context;

import org.myspring.beans.factory.HierarchicalBeanFactory;
import org.myspring.beans.factory.ListableBeanFactory;
import org.myspring.beans.factory.config.AutowireCapableBeanFactory;
import org.myspring.core.env.EnvironmentCapable;
import org.myspring.core.io.support.ResourcePatternResolver;

/**
 * 应用上下文
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
        MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

    String getId();

    String getApplicationName();

    String getDisplayName();

    long getStartupDate();

    ApplicationContext getParent();

    AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;



}
