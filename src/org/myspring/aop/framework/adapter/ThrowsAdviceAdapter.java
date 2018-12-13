package org.myspring.aop.framework.adapter;

import org.myspring.aop.Advisor;
import org.myspring.aop.ThrowsAdvice;
import org.myspring.aop.aopalliance.aop.Advice;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;

import java.io.Serializable;

 class ThrowsAdviceAdapter implements AdvisorAdapter, Serializable {

     @Override
     public boolean supportsAdvice(Advice advice) {
         return (advice instanceof ThrowsAdvice);
     }

     @Override
     public MethodInterceptor getInterceptor(Advisor advisor) {
         return new ThrowsAdviceInterceptor(advisor.getAdvice());
     }

 }
