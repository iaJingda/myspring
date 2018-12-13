package org.myspring.beans.factory;

import org.myspring.beans.BeansException;

public interface ObjectProvider<T> extends ObjectFactory<T>  {

    T getObject(Object... args) throws BeansException;

    T getIfAvailable() throws BeansException;

    T getIfUnique() throws BeansException;

}
