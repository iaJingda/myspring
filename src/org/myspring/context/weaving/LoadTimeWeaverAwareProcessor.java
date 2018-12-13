package org.myspring.context.weaving;

import org.myspring.beans.BeansException;
import org.myspring.beans.factory.BeanFactory;
import org.myspring.beans.factory.BeanFactoryAware;
import org.myspring.beans.factory.config.BeanPostProcessor;
import org.myspring.context.ConfigurableApplicationContext;
import org.myspring.context.instrument.classloading.LoadTimeWeaver;
import org.myspring.core.util.Assert;

public class LoadTimeWeaverAwareProcessor implements BeanPostProcessor, BeanFactoryAware {

    private LoadTimeWeaver loadTimeWeaver;
    private BeanFactory beanFactory;
    public LoadTimeWeaverAwareProcessor() {
    }
    public LoadTimeWeaverAwareProcessor(LoadTimeWeaver loadTimeWeaver) {
        this.loadTimeWeaver = loadTimeWeaver;
    }

    public LoadTimeWeaverAwareProcessor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof LoadTimeWeaverAware) {
            LoadTimeWeaver ltw = this.loadTimeWeaver;
            if (ltw == null) {
                Assert.state(this.beanFactory != null,
                        "BeanFactory required if no LoadTimeWeaver explicitly specified");
                ltw = this.beanFactory.getBean(
                        ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class);
            }
            ((LoadTimeWeaverAware) bean).setLoadTimeWeaver(ltw);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        return bean;
    }

}
