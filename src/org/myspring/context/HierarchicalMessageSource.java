package org.myspring.context;

public interface HierarchicalMessageSource extends MessageSource {

    void setParentMessageSource(MessageSource parent);

    MessageSource getParentMessageSource();

}
