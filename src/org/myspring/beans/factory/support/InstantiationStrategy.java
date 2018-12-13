package org.myspring.beans.factory.support;

import org.myspring.beans.BeansException;
import org.myspring.beans.factory.BeanFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface InstantiationStrategy {

    Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner)
            throws BeansException;

    Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner,
                       Constructor<?> ctor, Object... args) throws BeansException;

    Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner,
                       Object factoryBean, Method factoryMethod, Object... args) throws BeansException;
}
