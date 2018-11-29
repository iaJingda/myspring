package org.myspring.core.io;

import org.myspring.core.util.ResourceUtils;

/**
 * 资源加载器
 */
public interface ResourceLoader {

    String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;

    Resource getResource(String location);

    ClassLoader getClassLoader();

}
