package org.myspring.aop.framework.adapter;

import org.myspring.aop.aopalliance.aop.Advice;
import org.myspring.aop.Advisor;
import org.myspring.aop.MethodBeforeAdvice;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;

import java.io.Serializable;

 class MethodBeforeAdviceAdapter  implements AdvisorAdapter, Serializable {

     @Override
     public boolean supportsAdvice(Advice advice) {
         return (advice instanceof MethodBeforeAdvice);
     }

     @Override
     public MethodInterceptor getInterceptor(Advisor advisor) {
         MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
         return new MethodBeforeAdviceInterceptor(advice);
     }
}
