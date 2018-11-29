package org.myspring.context;

public interface ApplicationEventPublisher {

    void publishEvent(ApplicationEvent event);


    void publishEvent(Object event);

}
