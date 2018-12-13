package org.myspring.context.event;

import org.myspring.aop.framework.AopProxyUtils;
import org.myspring.beans.factory.BeanClassLoaderAware;
import org.myspring.beans.factory.BeanFactory;
import org.myspring.beans.factory.BeanFactoryAware;
import org.myspring.beans.factory.NoSuchBeanDefinitionException;
import org.myspring.beans.factory.config.ConfigurableBeanFactory;
import org.myspring.context.ApplicationEvent;
import org.myspring.context.ApplicationListener;
import org.myspring.core.ResolvableType;
import org.myspring.core.annotation.AnnotationAwareOrderComparator;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractApplicationEventMulticaster
        implements ApplicationEventMulticaster, BeanClassLoaderAware, BeanFactoryAware {


    private final ListenerRetriever defaultRetriever = new ListenerRetriever(false);

    final Map<ListenerCacheKey, ListenerRetriever> retrieverCache =
            new ConcurrentHashMap<ListenerCacheKey, ListenerRetriever>(64);

    private ClassLoader beanClassLoader;

    private BeanFactory beanFactory;

    private Object retrievalMutex = this.defaultRetriever;


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        if (beanFactory instanceof ConfigurableBeanFactory) {
            ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) beanFactory;
            if (this.beanClassLoader == null) {
                this.beanClassLoader = cbf.getBeanClassLoader();
            }
            this.retrievalMutex = cbf.getSingletonMutex();
        }
    }

    private BeanFactory getBeanFactory() {
        if (this.beanFactory == null) {
            throw new IllegalStateException("ApplicationEventMulticaster cannot retrieve listener beans " +
                    "because it is not associated with a BeanFactory");
        }
        return this.beanFactory;
    }


    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        synchronized (this.retrievalMutex) {
            // Explicitly remove target for a proxy, if registered already,
            // in order to avoid double invocations of the same listener.
            Object singletonTarget = AopProxyUtils.getSingletonTarget(listener);
            if (singletonTarget instanceof ApplicationListener) {
                this.defaultRetriever.applicationListeners.remove(singletonTarget);
            }
            this.defaultRetriever.applicationListeners.add(listener);
            this.retrieverCache.clear();
        }
    }

    @Override
    public void addApplicationListenerBean(String listenerBeanName) {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListenerBeans.add(listenerBeanName);
            this.retrieverCache.clear();
        }
    }

    @Override
    public void removeApplicationListener(ApplicationListener<?> listener) {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListeners.remove(listener);
            this.retrieverCache.clear();
        }
    }

    @Override
    public void removeApplicationListenerBean(String listenerBeanName) {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListenerBeans.remove(listenerBeanName);
            this.retrieverCache.clear();
        }
    }

    @Override
    public void removeAllListeners() {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListeners.clear();
            this.defaultRetriever.applicationListenerBeans.clear();
            this.retrieverCache.clear();
        }
    }



    protected Collection<ApplicationListener<?>> getApplicationListeners() {
        synchronized (this.retrievalMutex) {
            return this.defaultRetriever.getApplicationListeners();
        }
    }


    protected Collection<ApplicationListener<?>> getApplicationListeners(
            ApplicationEvent event, ResolvableType eventType) {

        Object source = event.getSource();
        Class<?> sourceType = (source != null ? source.getClass() : null);
        ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);

        // Quick check for existing entry on ConcurrentHashMap...
        ListenerRetriever retriever = this.retrieverCache.get(cacheKey);
        if (retriever != null) {
            return retriever.getApplicationListeners();
        }

        if (this.beanClassLoader == null ||
                (ClassUtils.isCacheSafe(event.getClass(), this.beanClassLoader) &&
                        (sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
            // Fully synchronized building and caching of a ListenerRetriever
            synchronized (this.retrievalMutex) {
                retriever = this.retrieverCache.get(cacheKey);
                if (retriever != null) {
                    return retriever.getApplicationListeners();
                }
                retriever = new ListenerRetriever(true);
                Collection<ApplicationListener<?>> listeners =
                        retrieveApplicationListeners(eventType, sourceType, retriever);
                this.retrieverCache.put(cacheKey, retriever);
                return listeners;
            }
        }
        else {
            // No ListenerRetriever caching -> no synchronization necessary
            return retrieveApplicationListeners(eventType, sourceType, null);
        }
    }

    /**
     * Actually retrieve the application listeners for the given event and source type.
     * @param eventType the event type
     * @param sourceType the event source type
     * @param retriever the ListenerRetriever, if supposed to populate one (for caching purposes)
     * @return the pre-filtered list of application listeners for the given event and source type
     */
    private Collection<ApplicationListener<?>> retrieveApplicationListeners(
            ResolvableType eventType, Class<?> sourceType, ListenerRetriever retriever) {

        List<ApplicationListener<?>> allListeners = new ArrayList<ApplicationListener<?>>();
        Set<ApplicationListener<?>> listeners;
        Set<String> listenerBeans;
        synchronized (this.retrievalMutex) {
            listeners = new LinkedHashSet<ApplicationListener<?>>(this.defaultRetriever.applicationListeners);
            listenerBeans = new LinkedHashSet<String>(this.defaultRetriever.applicationListenerBeans);
        }
        for (ApplicationListener<?> listener : listeners) {
            if (supportsEvent(listener, eventType, sourceType)) {
                if (retriever != null) {
                    retriever.applicationListeners.add(listener);
                }
                allListeners.add(listener);
            }
        }
        if (!listenerBeans.isEmpty()) {
            BeanFactory beanFactory = getBeanFactory();
            for (String listenerBeanName : listenerBeans) {
                try {
                    Class<?> listenerType = beanFactory.getType(listenerBeanName);
                    if (listenerType == null || supportsEvent(listenerType, eventType)) {
                        ApplicationListener<?> listener =
                                beanFactory.getBean(listenerBeanName, ApplicationListener.class);
                        if (!allListeners.contains(listener) && supportsEvent(listener, eventType, sourceType)) {
                            if (retriever != null) {
                                retriever.applicationListenerBeans.add(listenerBeanName);
                            }
                            allListeners.add(listener);
                        }
                    }
                }
                catch (NoSuchBeanDefinitionException ex) {
                    // Singleton listener instance (without backing bean definition) disappeared -
                    // probably in the middle of the destruction phase
                }
            }
        }
        AnnotationAwareOrderComparator.sort(allListeners);
        return allListeners;
    }

    /**
     * Filter a listener early through checking its generically declared event
     * type before trying to instantiate it.
     * <p>If this method returns {@code true} for a given listener as a first pass,
     * the listener instance will get retrieved and fully evaluated through a
     * {@link #supportsEvent(ApplicationListener,ResolvableType, Class)}  call afterwards.
     * @param listenerType the listener's type as determined by the BeanFactory
     * @param eventType the event type to check
     * @return whether the given listener should be included in the candidates
     * for the given event type
     */
    protected boolean supportsEvent(Class<?> listenerType, ResolvableType eventType) {
        if (GenericApplicationListener.class.isAssignableFrom(listenerType) ||
                SmartApplicationListener.class.isAssignableFrom(listenerType)) {
            return true;
        }
        ResolvableType declaredEventType = GenericApplicationListenerAdapter.resolveDeclaredEventType(listenerType);
        return (declaredEventType == null || declaredEventType.isAssignableFrom(eventType));
    }


    protected boolean supportsEvent(ApplicationListener<?> listener, ResolvableType eventType, Class<?> sourceType) {
        GenericApplicationListener smartListener = (listener instanceof GenericApplicationListener ?
                (GenericApplicationListener) listener : new GenericApplicationListenerAdapter(listener));
        return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
    }


    /**
     * Cache key for ListenerRetrievers, based on event type and source type.
     */
    private static final class ListenerCacheKey implements Comparable<ListenerCacheKey> {

        private final ResolvableType eventType;

        private final Class<?> sourceType;

        public ListenerCacheKey(ResolvableType eventType, Class<?> sourceType) {
            this.eventType = eventType;
            this.sourceType = sourceType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            ListenerCacheKey otherKey = (ListenerCacheKey) other;
            return (ObjectUtils.nullSafeEquals(this.eventType, otherKey.eventType) &&
                    ObjectUtils.nullSafeEquals(this.sourceType, otherKey.sourceType));
        }

        @Override
        public int hashCode() {
            return (ObjectUtils.nullSafeHashCode(this.eventType) * 29 + ObjectUtils.nullSafeHashCode(this.sourceType));
        }

        @Override
        public String toString() {
            return "ListenerCacheKey [eventType = " + this.eventType + ", sourceType = " + this.sourceType.getName() + "]";
        }

        @Override
        public int compareTo(ListenerCacheKey other) {
            int result = 0;
            if (this.eventType != null) {
                result = this.eventType.toString().compareTo(other.eventType.toString());
            }
            if (result == 0 && this.sourceType != null) {
                result = this.sourceType.getName().compareTo(other.sourceType.getName());
            }
            return result;
        }
    }


    /**
     * Helper class that encapsulates a specific set of target listeners,
     * allowing for efficient retrieval of pre-filtered listeners.
     * <p>An instance of this helper gets cached per event type and source type.
     */
    private class ListenerRetriever {

        public final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<ApplicationListener<?>>();

        public final Set<String> applicationListenerBeans = new LinkedHashSet<String>();

        private final boolean preFiltered;

        public ListenerRetriever(boolean preFiltered) {
            this.preFiltered = preFiltered;
        }

        public Collection<ApplicationListener<?>> getApplicationListeners() {
            List<ApplicationListener<?>> allListeners = new ArrayList<ApplicationListener<?>>(
                    this.applicationListeners.size() + this.applicationListenerBeans.size());
            allListeners.addAll(this.applicationListeners);
            if (!this.applicationListenerBeans.isEmpty()) {
                BeanFactory beanFactory = getBeanFactory();
                for (String listenerBeanName : this.applicationListenerBeans) {
                    try {
                        ApplicationListener<?> listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
                        if (this.preFiltered || !allListeners.contains(listener)) {
                            allListeners.add(listener);
                        }
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        // Singleton listener instance (without backing bean definition) disappeared -
                        // probably in the middle of the destruction phase
                    }
                }
            }
            AnnotationAwareOrderComparator.sort(allListeners);
            return allListeners;
        }
    }
}
