package org.myspring.aop.aopalliance.intercept;

public interface MethodInterceptor extends Interceptor {

    Object invoke(MethodInvocation invocation) throws Throwable;

}
