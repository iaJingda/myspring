package org.myspring.context;

import org.myspring.beans.factory.Aware;
import org.myspring.core.io.ResourceLoader;

public interface ResourceLoaderAware extends Aware {

    void setResourceLoader(ResourceLoader resourceLoader);

}
