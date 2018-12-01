package org.myspring.context;

import org.myspring.beans.BeansException;
import org.myspring.beans.factory.BeanDefinitionStoreException;
import org.myspring.beans.factory.NoSuchBeanDefinitionException;
import org.myspring.beans.factory.config.*;
import org.myspring.core.env.ConfigurableEnvironment;
import org.myspring.core.io.ProtocolResolver;
import org.myspring.core.util.StringValueResolver;

import java.io.Closeable;
import java.security.AccessControlContext;

public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {

    String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

    String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

    String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

    String ENVIRONMENT_BEAN_NAME = "environment";

    String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

    String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";

    void setId(String id);

    void setParent(ApplicationContext parent);

    void setEnvironment(ConfigurableEnvironment environment);

    @Override
    ConfigurableEnvironment getEnvironment();

    void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

    void addApplicationListener(ApplicationListener<?> listener);

    void addProtocolResolver(ProtocolResolver resolver);

    void refresh() throws BeansException, IllegalStateException;

    void registerShutdownHook();

    @Override
    void close();

    boolean isActive();

    ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


}
