package org.myspring.beans.factory.config;

import org.myspring.beans.factory.ObjectFactory;

public interface Scope {

    Object get(String name, ObjectFactory<?> objectFactory);

    Object remove(String name);

    void registerDestructionCallback(String name, Runnable callback);

    Object resolveContextualObject(String key);

    String getConversationId();

}
