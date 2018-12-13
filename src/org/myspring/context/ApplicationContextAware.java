package org.myspring.context;

import org.myspring.beans.BeansException;
import org.myspring.beans.factory.Aware;

public interface ApplicationContextAware extends Aware {

    void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

}
