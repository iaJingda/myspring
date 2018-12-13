package org.myspring.aop.framework.adapter;

import org.myspring.aop.AfterReturningAdvice;
import org.myspring.aop.aopalliance.aop.Advice;
import org.myspring.aop.Advisor;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;

import java.io.Serializable;

 class AfterReturningAdviceAdapter  implements AdvisorAdapter, Serializable {

     @Override
     public boolean supportsAdvice(Advice advice) {
         return (advice instanceof AfterReturningAdvice);
     }

     @Override
     public MethodInterceptor getInterceptor(Advisor advisor) {
         AfterReturningAdvice advice = (AfterReturningAdvice) advisor.getAdvice();
         return new AfterReturningAdviceInterceptor(advice);
     }
}
