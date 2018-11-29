package org.myspring.beans.factory.config;

import org.myspring.beans.factory.NamedBean;
import org.myspring.core.util.Assert;

public class NamedBeanHolder<T> implements NamedBean {

    private final String beanName;

    private final T beanInstance;

    public NamedBeanHolder(String beanName, T beanInstance) {
        Assert.notNull(beanName, "Bean name must not be null");
        this.beanName = beanName;
        this.beanInstance = beanInstance;
    }

    @Override
    public String getBeanName() {
        return this.beanName;
    }

    /**
     * Return the corresponding bean instance (can be {@code null}).
     */
    public T getBeanInstance() {
        return this.beanInstance;
    }
}
