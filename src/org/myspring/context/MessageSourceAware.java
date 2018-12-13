package org.myspring.context;

import org.myspring.beans.factory.Aware;

public interface MessageSourceAware  extends Aware {

    void setMessageSource(MessageSource messageSource);

}
