package org.myspring.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myspring.beans.factory.DisposableBean;
import org.myspring.beans.factory.config.BeanFactoryPostProcessor;
import org.myspring.context.*;
import org.myspring.context.event.ApplicationEventMulticaster;
import org.myspring.context.event.ContextClosedEvent;
import org.myspring.core.env.ConfigurableEnvironment;
import org.myspring.core.env.StandardEnvironment;
import org.myspring.core.io.DefaultResourceLoader;
import org.myspring.core.io.support.ResourcePatternResolver;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractApplicationContext extends DefaultResourceLoader
        implements ConfigurableApplicationContext, DisposableBean {

    public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

    public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

    public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

    static {
        // Eagerly load the ContextClosedEvent class to avoid weird classloader issues
        // on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
        ContextClosedEvent.class.getName();
    }

    protected final Log logger = LogFactory.getLog(getClass());

    private String id = ObjectUtils.identityToString(this);

    private String displayName = ObjectUtils.identityToString(this);

    private ApplicationContext parent;

    private ConfigurableEnvironment environment;

    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors =
            new ArrayList<BeanFactoryPostProcessor>();

    private long startupDate;

    private final AtomicBoolean active = new AtomicBoolean();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final Object startupShutdownMonitor = new Object();

    private Thread shutdownHook;

    private ResourcePatternResolver resourcePatternResolver;

    private LifecycleProcessor lifecycleProcessor;

    private MessageSource messageSource;

    private ApplicationEventMulticaster applicationEventMulticaster;

    private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<ApplicationListener<?>>();

    private Set<ApplicationEvent> earlyApplicationEvents;

    public AbstractApplicationContext() {
        this.resourcePatternResolver = getResourcePatternResolver();
    }

    public AbstractApplicationContext(ApplicationContext parent) {
        this();
        setParent(parent);
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getApplicationName() {
        return "";
    }

    public void setDisplayName(String displayName) {
        Assert.hasLength(displayName, "Display name must not be empty");
        this.displayName = displayName;
    }

    protected ResourcePatternResolver getResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver(this);
    }



    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public ApplicationContext getParent() {
        return this.parent;
    }

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }

    protected ConfigurableEnvironment createEnvironment() {
        return new StandardEnvironment();
    }






}
