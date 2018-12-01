package org.myspring.context;

public interface LifecycleProcessor extends Lifecycle {

    void onRefresh();


    void onClose();

}
