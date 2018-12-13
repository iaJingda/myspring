package org.myspring.aop.framework.adapter;

import org.myspring.aop.AfterAdvice;
import org.myspring.aop.AfterReturningAdvice;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;
import org.myspring.aop.aopalliance.intercept.MethodInvocation;
import org.myspring.core.util.Assert;

import java.io.Serializable;

public class AfterReturningAdviceInterceptor implements MethodInterceptor, AfterAdvice, Serializable {

    private final AfterReturningAdvice advice;


    /**
     * Create a new AfterReturningAdviceInterceptor for the given advice.
     * @param advice the AfterReturningAdvice to wrap
     */
    public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
        Assert.notNull(advice, "Advice must not be null");
        this.advice = advice;
    }


    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        Object retVal = mi.proceed();
        this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
        return retVal;
    }
}
