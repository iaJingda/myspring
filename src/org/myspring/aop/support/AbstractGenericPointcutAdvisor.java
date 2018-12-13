package org.myspring.aop.support;

import org.myspring.aop.aopalliance.aop.Advice;

public abstract  class AbstractGenericPointcutAdvisor extends AbstractPointcutAdvisor  {

    private Advice advice;


    /**
     * Specify the advice that this advisor should apply.
     */
    public void setAdvice(Advice advice) {
        this.advice = advice;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }


    @Override
    public String toString() {
        return getClass().getName() + ": advice [" + getAdvice() + "]";
    }
}
