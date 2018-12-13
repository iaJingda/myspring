package org.myspring.beans.factory.config;

import org.myspring.beans.BeansException;

public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

    void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

    boolean requiresDestruction(Object bean);

}
