package org.myspring.aop.framework.adapter;

import org.myspring.aop.aopalliance.aop.Advice;
import org.myspring.aop.Advisor;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;

public interface AdvisorAdapter {

    boolean supportsAdvice(Advice advice);


    MethodInterceptor getInterceptor(Advisor advisor);

}
