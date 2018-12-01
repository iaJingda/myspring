package org.myspring.beans.factory.config;

import org.myspring.beans.BeansException;

public interface BeanExpressionResolver {

    Object evaluate(String value, BeanExpressionContext evalContext) throws BeansException;

}
