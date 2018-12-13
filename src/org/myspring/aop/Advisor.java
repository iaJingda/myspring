package org.myspring.aop;

import org.myspring.aop.aopalliance.aop.Advice;

public interface Advisor {

    Advice getAdvice();
    boolean isPerInstance();

}
