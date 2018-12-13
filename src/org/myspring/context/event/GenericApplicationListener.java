package org.myspring.context.event;

import org.myspring.context.ApplicationEvent;
import org.myspring.context.ApplicationListener;
import org.myspring.core.Ordered;
import org.myspring.core.ResolvableType;

public interface GenericApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

    boolean supportsEventType(ResolvableType eventType);

    boolean supportsSourceType(Class<?> sourceType);

}
