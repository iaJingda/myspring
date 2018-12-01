package org.myspring.beans.factory.config;

import org.myspring.beans.BeanMetadataElement;
import org.myspring.beans.MutablePropertyValues;
import org.myspring.core.AttributeAccessor;

public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

    int ROLE_APPLICATION = 0;

    int ROLE_SUPPORT = 1;

    int ROLE_INFRASTRUCTURE = 2;

    void setParentName(String parentName);

    String getParentName();


    void setBeanClassName(String beanClassName);

    String getBeanClassName();

    void setScope(String scope);

    String getScope();

    void setLazyInit(boolean lazyInit);

    boolean isLazyInit();

    void setDependsOn(String... dependsOn);

    String[] getDependsOn();

    void setAutowireCandidate(boolean autowireCandidate);

    boolean isAutowireCandidate();

    void setPrimary(boolean primary);

    boolean isPrimary();

    void setFactoryBeanName(String factoryBeanName);

    String getFactoryBeanName();

    void setFactoryMethodName(String factoryMethodName);

    String getFactoryMethodName();

    ConstructorArgumentValues getConstructorArgumentValues();

    MutablePropertyValues getPropertyValues();

    boolean isSingleton();

    boolean isPrototype();

    boolean isAbstract();

    int getRole();

    String getDescription();

    String getResourceDescription();

    BeanDefinition getOriginatingBeanDefinition();


}
