package org.myspring.beans;

public interface Mergeable {

    boolean isMergeEnabled();

    Object merge(Object parent);

}
