package org.myspring.context;

import org.myspring.beans.factory.Aware;

public interface ApplicationEventPublisherAware  extends Aware {

    void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher);

}
