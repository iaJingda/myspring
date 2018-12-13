package org.myspring.aop.framework.adapter;

import org.myspring.aop.Advisor;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;

public interface AdvisorAdapterRegistry {

    Advisor wrap(Object advice) throws UnknownAdviceTypeException;

    MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException;

    void registerAdvisorAdapter(AdvisorAdapter adapter);

}
