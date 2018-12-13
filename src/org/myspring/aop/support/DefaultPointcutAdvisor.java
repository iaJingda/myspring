package org.myspring.aop.support;

import org.myspring.aop.Pointcut;
import org.myspring.aop.aopalliance.aop.Advice;

import java.io.Serializable;

public class DefaultPointcutAdvisor extends AbstractGenericPointcutAdvisor implements Serializable {

    private Pointcut pointcut = Pointcut.TRUE;


    /**
     * Create an empty DefaultPointcutAdvisor.
     * <p>Advice must be set before use using setter methods.
     * Pointcut will normally be set also, but defaults to {@code Pointcut.TRUE}.
     */
    public DefaultPointcutAdvisor() {
    }

    /**
     * Create a DefaultPointcutAdvisor that matches all methods.
     * <p>{@code Pointcut.TRUE} will be used as Pointcut.
     * @param advice the Advice to use
     */
    public DefaultPointcutAdvisor(Advice advice) {
        this(Pointcut.TRUE, advice);
    }

    /**
     * Create a DefaultPointcutAdvisor, specifying Pointcut and Advice.
     * @param pointcut the Pointcut targeting the Advice
     * @param advice the Advice to run when Pointcut matches
     */
    public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
        this.pointcut = pointcut;
        setAdvice(advice);
    }


    /**
     * Specify the pointcut targeting the advice.
     * <p>Default is {@code Pointcut.TRUE}.
     * @see #setAdvice
     */
    public void setPointcut(Pointcut pointcut) {
        this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }


    @Override
    public String toString() {
        return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice [" + getAdvice() + "]";
    }

}
