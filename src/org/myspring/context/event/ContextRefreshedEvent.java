package org.myspring.context.event;

import org.myspring.context.ApplicationContext;

public class ContextRefreshedEvent extends ApplicationContextEvent {

    public ContextRefreshedEvent(ApplicationContext source) {
        super(source);
    }
}
