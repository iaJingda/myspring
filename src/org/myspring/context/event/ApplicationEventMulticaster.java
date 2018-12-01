package org.myspring.context.event;

import org.myspring.context.ApplicationEvent;
import org.myspring.context.ApplicationListener;
import org.myspring.core.ResolvableType;

public interface ApplicationEventMulticaster {

    void addApplicationListener(ApplicationListener<?> listener);

    void addApplicationListenerBean(String listenerBeanName);

    void removeApplicationListener(ApplicationListener<?> listener);

    void removeApplicationListenerBean(String listenerBeanName);

    void removeAllListeners();

    void multicastEvent(ApplicationEvent event);

    void multicastEvent(ApplicationEvent event, ResolvableType eventType);


}
