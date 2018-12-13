package org.myspring.beans.factory.support;

import org.myspring.beans.factory.config.BeanDefinitionHolder;
import org.myspring.beans.factory.config.DependencyDescriptor;

public interface AutowireCandidateResolver {

    boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor);

    Object getSuggestedValue(DependencyDescriptor descriptor);

    Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName);

}
