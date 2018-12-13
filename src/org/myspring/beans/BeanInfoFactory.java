package org.myspring.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

public interface BeanInfoFactory {

    BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;

}
