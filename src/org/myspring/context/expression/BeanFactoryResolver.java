package org.myspring.context.expression;

import org.myspring.beans.BeansException;
import org.myspring.beans.factory.BeanFactory;
import org.myspring.core.util.Assert;
import org.myspring.expression.AccessException;
import org.myspring.expression.BeanResolver;
import org.myspring.expression.EvaluationContext;

public class BeanFactoryResolver implements BeanResolver {

    private final BeanFactory beanFactory;



    public BeanFactoryResolver(BeanFactory beanFactory) {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        this.beanFactory = beanFactory;
    }


    @Override
    public Object resolve(EvaluationContext context, String beanName) throws AccessException {
        try {
            return this.beanFactory.getBean(beanName);
        }
        catch (BeansException ex) {
            throw new AccessException("Could not resolve bean reference against BeanFactory", ex);
        }
    }

}
