package org.myspring.core.io.support;

import org.myspring.core.io.Resource;
import org.myspring.core.io.ResourceLoader;

import java.io.IOException;

/**
 * 资源模式解决器
 */
public interface ResourcePatternResolver extends ResourceLoader {

    String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

    Resource[] getResources(String locationPattern) throws IOException;
}
