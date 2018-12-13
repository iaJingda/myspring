package org.myspring.context.event;

import org.myspring.context.ApplicationContext;

public class ContextStoppedEvent extends ApplicationContextEvent {

    public ContextStoppedEvent(ApplicationContext source) {
        super(source);
    }
}
