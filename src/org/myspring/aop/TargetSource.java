package org.myspring.aop;

public interface TargetSource  extends TargetClassAware  {

    @Override
    Class<?> getTargetClass();

    /**
     * Will all calls to {@link #getTarget()} return the same object?
     * <p>In that case, there will be no need to invoke {@link #releaseTarget(Object)},
     * and the AOP framework can cache the return value of {@link #getTarget()}.
     * @return {@code true} if the target is immutable
     * @see #getTarget
     */
    boolean isStatic();

    /**
     * Return a target instance. Invoked immediately before the
     * AOP framework calls the "target" of an AOP method invocation.
     * @return the target object which contains the joinpoint,
     * or {@code null} if there is no actual target instance
     * @throws Exception if the target object can't be resolved
     */
    Object getTarget() throws Exception;

    /**
     * Release the given target object obtained from the
     * {@link #getTarget()} method, if any.
     * @param target object obtained from a call to {@link #getTarget()}
     * @throws Exception if the object can't be released
     */
    void releaseTarget(Object target) throws Exception;
}
