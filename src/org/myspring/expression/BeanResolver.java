package org.myspring.expression;

public interface BeanResolver {

    Object resolve(EvaluationContext context, String beanName) throws AccessException;



}
