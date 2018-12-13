package org.myspring.aop;

import org.myspring.aop.aopalliance.aop.Advice;

public interface DynamicIntroductionAdvice extends Advice {

    boolean implementsInterface(Class<?> intf);

}
