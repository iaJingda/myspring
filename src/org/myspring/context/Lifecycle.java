package org.myspring.context;

public interface Lifecycle {

    void start();

    void stop();

    boolean isRunning();
}
