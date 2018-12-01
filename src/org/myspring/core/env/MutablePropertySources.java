package org.myspring.core.env;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CopyOnWriteArrayList 是什么?怎么用?
 *
 * Copy-On-Write简称COW，是一种用于程序设计中的优化策略。其基本思路是，从一开始大家都在共享同一个内容，
 * 当某个人想要修改这个内容的时候，才会真正把内容Copy出去形成一个新的内容然后再改，这是一种延时懒惰策略。
 * 从JDK1.5开始Java并发包里提供了两个使用CopyOnWrite机制实现的并发容器,
 * 它们是CopyOnWriteArrayList和CopyOnWriteArraySet。CopyOnWrite容器非常有用，可以在非常多的并发场景中使用到。
 *
 * 什么是CopyOnWrite容器?
 * CopyOnWrite容器即写时复制的容器。通俗的理解是当我们往一个容器添加元素的时候，不直接往当前容器添加，而是先将当前容器进行Copy，
 * 复制出一个新的容器，然后新的容器里添加元素，添加完元素之后，再将原容器的引用指向新的容器。
 * 这样做的好处是我们可以对CopyOnWrite容器进行并发的读，而不需要加锁，因为当前容器不会添加任何元素。
 * 所以CopyOnWrite容器也是一种读写分离的思想，读和写不同的容器。
 *
 * 参考:  http://ifeve.com/java-copy-on-write/
 */
public class MutablePropertySources implements PropertySources {

    private final Log logger;

    private final List<PropertySource<?>> propertySourceList = new CopyOnWriteArrayList<PropertySource<?>>();

    public MutablePropertySources() {
        this.logger = LogFactory.getLog(getClass());
    }

    public MutablePropertySources(PropertySources propertySources) {
        this();
        for (PropertySource<?> propertySource : propertySources) {
            addLast(propertySource);
        }
    }

    MutablePropertySources(Log logger) {
        this.logger = logger;
    }

    @Override
    public boolean contains(String name) {
        return this.propertySourceList.contains(PropertySource.named(name));
    }


    @Override
    public PropertySource<?> get(String name) {
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
        return (index != -1 ? this.propertySourceList.get(index) : null);
    }

    @Override
    public Iterator<PropertySource<?>> iterator() {
        return this.propertySourceList.iterator();
    }

    public void addFirst(PropertySource<?> propertySource) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding PropertySource '" + propertySource.getName() + "' with highest search precedence");
        }
        removeIfPresent(propertySource);
        this.propertySourceList.add(0, propertySource);
    }

    public void addLast(PropertySource<?> propertySource) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding PropertySource '" + propertySource.getName() + "' with lowest search precedence");
        }
        removeIfPresent(propertySource);
        this.propertySourceList.add(propertySource);
    }

    public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding PropertySource '" + propertySource.getName() +
                    "' with search precedence immediately higher than '" + relativePropertySourceName + "'");
        }
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        removeIfPresent(propertySource);
        int index = assertPresentAndGetIndex(relativePropertySourceName);
        addAtIndex(index, propertySource);
    }

    public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding PropertySource '" + propertySource.getName() +
                    "' with search precedence immediately lower than '" + relativePropertySourceName + "'");
        }
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        removeIfPresent(propertySource);
        int index = assertPresentAndGetIndex(relativePropertySourceName);
        addAtIndex(index + 1, propertySource);
    }

    public int precedenceOf(PropertySource<?> propertySource) {
        return this.propertySourceList.indexOf(propertySource);
    }
    public PropertySource<?> remove(String name) {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing PropertySource '" + name + "'");
        }
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
        return (index != -1 ? this.propertySourceList.remove(index) : null);
    }

    public void replace(String name, PropertySource<?> propertySource) {
        if (logger.isDebugEnabled()) {
            logger.debug("Replacing PropertySource '" + name + "' with '" + propertySource.getName() + "'");
        }
        int index = assertPresentAndGetIndex(name);
        this.propertySourceList.set(index, propertySource);
    }

    public int size() {
        return this.propertySourceList.size();
    }

    @Override
    public String toString() {
        return this.propertySourceList.toString();
    }

    protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
        String newPropertySourceName = propertySource.getName();
        if (relativePropertySourceName.equals(newPropertySourceName)) {
            throw new IllegalArgumentException(
                    "PropertySource named '" + newPropertySourceName + "' cannot be added relative to itself");
        }
    }

    protected void removeIfPresent(PropertySource<?> propertySource) {
        this.propertySourceList.remove(propertySource);
    }

    private void addAtIndex(int index, PropertySource<?> propertySource) {
        removeIfPresent(propertySource);
        this.propertySourceList.add(index, propertySource);
    }

    private int assertPresentAndGetIndex(String name) {
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
        if (index == -1) {
            throw new IllegalArgumentException("PropertySource named '" + name + "' does not exist");
        }
        return index;
    }
}
