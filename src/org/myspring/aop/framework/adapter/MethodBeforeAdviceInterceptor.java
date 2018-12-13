package org.myspring.aop.framework.adapter;

import org.myspring.aop.BeforeAdvice;
import org.myspring.aop.MethodBeforeAdvice;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;
import org.myspring.aop.aopalliance.intercept.MethodInvocation;
import org.myspring.core.util.Assert;

import java.io.Serializable;

public class MethodBeforeAdviceInterceptor implements MethodInterceptor, BeforeAdvice, Serializable {

    private final MethodBeforeAdvice advice;


    /**
     * Create a new MethodBeforeAdviceInterceptor for the given advice.
     * @param advice the MethodBeforeAdvice to wrap
     */
    public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
        Assert.notNull(advice, "Advice must not be null");
        this.advice = advice;
    }


    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
        return mi.proceed();
    }
}
