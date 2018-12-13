package org.myspring.context.event;

import org.myspring.context.ApplicationEvent;
import org.myspring.context.ApplicationListener;
import org.myspring.core.Ordered;

public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

    boolean supportsEventType(Class<? extends ApplicationEvent> eventType);

    boolean supportsSourceType(Class<?> sourceType);

}
