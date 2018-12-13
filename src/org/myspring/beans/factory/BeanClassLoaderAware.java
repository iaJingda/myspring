package org.myspring.beans.factory;

public interface BeanClassLoaderAware  extends Aware {

    void setBeanClassLoader(ClassLoader classLoader);

}
