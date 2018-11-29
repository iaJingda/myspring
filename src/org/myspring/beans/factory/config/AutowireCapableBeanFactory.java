package org.myspring.beans.factory.config;

import org.myspring.beans.BeansException;
import org.myspring.beans.TypeConverter;

import java.util.Set;

public interface AutowireCapableBeanFactory {

    int AUTOWIRE_NO = 0;

    int AUTOWIRE_BY_NAME = 1;

    int AUTOWIRE_BY_TYPE = 2;

    int AUTOWIRE_CONSTRUCTOR = 3;

    @Deprecated
    int AUTOWIRE_AUTODETECT = 4;

    <T> T createBean(Class<T> beanClass) throws BeansException;

    void autowireBean(Object existingBean) throws BeansException;

    Object configureBean(Object existingBean, String beanName) throws BeansException;

    Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;


    Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

    void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
            throws BeansException;

    void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;


    Object initializeBean(Object existingBean, String beanName) throws BeansException;


    Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
            throws BeansException;

    Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
            throws BeansException;

    void destroyBean(Object existingBean);


    <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

    Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName) throws BeansException;


    Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName,
                             Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException;

}
