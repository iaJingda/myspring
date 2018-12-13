package org.myspring.aop.framework;

import org.myspring.aop.aopalliance.aop.Advice;
import org.myspring.aop.Advisor;
import org.myspring.aop.TargetClassAware;
import org.myspring.aop.TargetSource;

public interface Advised extends TargetClassAware {

    boolean isFrozen();

    /**
     * Are we proxying the full target class instead of specified interfaces?
     */
    boolean isProxyTargetClass();

    /**
     * Return the interfaces proxied by the AOP proxy.
     * <p>Will not include the target class, which may also be proxied.
     */
    Class<?>[] getProxiedInterfaces();

    /**
     * Determine whether the given interface is proxied.
     * @param intf the interface to check
     */
    boolean isInterfaceProxied(Class<?> intf);

    /**
     * Change the {@code TargetSource} used by this {@code Advised} object.
     * <p>Only works if the configuration isn't {@linkplain #isFrozen frozen}.
     * @param targetSource new TargetSource to use
     */
    void setTargetSource(TargetSource targetSource);

    /**
     * Return the {@code TargetSource} used by this {@code Advised} object.
     */
    TargetSource getTargetSource();


    void setExposeProxy(boolean exposeProxy);


    boolean isExposeProxy();

    void setPreFiltered(boolean preFiltered);

    /**
     * Return whether this proxy configuration is pre-filtered so that it only
     * contains applicable advisors (matching this proxy's target class).
     */
    boolean isPreFiltered();

    /**
     * Return the advisors applying to this proxy.
     * @return a list of Advisors applying to this proxy (never {@code null})
     */
    Advisor[] getAdvisors();


    void addAdvisor(Advisor advisor) throws AopConfigException;

    /**
     * Add an Advisor at the specified position in the chain.
     * @param advisor the advisor to add at the specified position in the chain
     * @param pos position in chain (0 is head). Must be valid.
     * @throws AopConfigException in case of invalid advice
     */
    void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

    /**
     * Remove the given advisor.
     * @param advisor the advisor to remove
     * @return {@code true} if the advisor was removed; {@code false}
     * if the advisor was not found and hence could not be removed
     */
    boolean removeAdvisor(Advisor advisor);

    /**
     * Remove the advisor at the given index.
     * @param index index of advisor to remove
     * @throws AopConfigException if the index is invalid
     */
    void removeAdvisor(int index) throws AopConfigException;

    /**
     * Return the index (from 0) of the given advisor,
     * or -1 if no such advisor applies to this proxy.
     * <p>The return value of this method can be used to index into the advisors array.
     * @param advisor the advisor to search for
     * @return index from 0 of this advisor, or -1 if there's no such advisor
     */
    int indexOf(Advisor advisor);


    boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;


    void addAdvice(Advice advice) throws AopConfigException;

    void addAdvice(int pos, Advice advice) throws AopConfigException;

    /**
     * Remove the Advisor containing the given advice.
     * @param advice the advice to remove
     * @return {@code true} of the advice was found and removed;
     * {@code false} if there was no such advice
     */
    boolean removeAdvice(Advice advice);

    /**
     * Return the index (from 0) of the given AOP Alliance Advice,
     * or -1 if no such advice is an advice for this proxy.
     * <p>The return value of this method can be used to index into
     * the advisors array.
     * @param advice AOP Alliance advice to search for
     * @return index from 0 of this advice, or -1 if there's no such advice
     */
    int indexOf(Advice advice);

    /**
     * As {@code toString()} will normally be delegated to the target,
     * this returns the equivalent for the AOP proxy.
     * @return a string description of the proxy configuration
     */
    String toProxyConfigString();

}
