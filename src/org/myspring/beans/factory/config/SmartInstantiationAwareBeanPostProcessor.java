package org.myspring.beans.factory.config;

import org.myspring.beans.BeansException;

import java.lang.reflect.Constructor;

public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

    Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException;

    Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException;

    Object getEarlyBeanReference(Object bean, String beanName) throws BeansException;

}
