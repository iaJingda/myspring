package org.myspring.aop.framework;

import org.myspring.aop.MethodMatcher;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;

class InterceptorAndDynamicMethodMatcher {

     final MethodInterceptor interceptor;

     final MethodMatcher methodMatcher;

     public InterceptorAndDynamicMethodMatcher(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
         this.interceptor = interceptor;
         this.methodMatcher = methodMatcher;
     }
}
