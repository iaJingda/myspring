package org.myspring.context.event;

import org.myspring.context.ApplicationContext;

public class ContextStartedEvent extends ApplicationContextEvent {

    public ContextStartedEvent(ApplicationContext source) {
        super(source);
    }
}
