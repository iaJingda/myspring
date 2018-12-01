package org.myspring.context.event;

import org.myspring.context.ApplicationContext;
import org.myspring.context.ApplicationEvent;

public abstract class ApplicationContextEvent extends ApplicationEvent {

    public ApplicationContextEvent(ApplicationContext source) {
        super(source);
    }

    public final ApplicationContext getApplicationContext() {
        return (ApplicationContext) getSource();
    }

}
