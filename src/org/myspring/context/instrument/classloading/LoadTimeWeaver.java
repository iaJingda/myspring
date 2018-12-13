package org.myspring.context.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

public interface LoadTimeWeaver {

    void addTransformer(ClassFileTransformer transformer);
    ClassLoader getInstrumentableClassLoader();
    ClassLoader getThrowawayClassLoader();

}
