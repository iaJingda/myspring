package org.myspring.aop;

public interface PointcutAdvisor extends Advisor {

    Pointcut getPointcut();

}
