package org.myspring.beans.factory.support;

import org.myspring.beans.factory.config.BeanDefinitionHolder;
import org.myspring.beans.factory.config.DependencyDescriptor;

public class SimpleAutowireCandidateResolver  implements AutowireCandidateResolver {

    @Override
    public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
        return bdHolder.getBeanDefinition().isAutowireCandidate();
    }

    public boolean isRequired(DependencyDescriptor descriptor) {
        return descriptor.isRequired();
    }

    @Override
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
        return null;
    }

    @Override
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
        return null;
    }

}
