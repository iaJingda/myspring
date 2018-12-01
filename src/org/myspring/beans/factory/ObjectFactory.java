package org.myspring.beans.factory;

import org.myspring.beans.BeansException;

public interface ObjectFactory<T> {

    T getObject() throws BeansException;

}
