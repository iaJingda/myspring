package org.myspring.beans.factory.config;

import org.myspring.beans.BeansException;

public interface BeanFactoryPostProcessor {

    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
