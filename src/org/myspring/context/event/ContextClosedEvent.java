package org.myspring.context.event;

import org.myspring.context.ApplicationContext;

public class ContextClosedEvent extends ApplicationContextEvent {

    public ContextClosedEvent(ApplicationContext source) {
        super(source);
    }
}
