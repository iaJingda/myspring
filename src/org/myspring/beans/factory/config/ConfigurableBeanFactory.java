package org.myspring.beans.factory.config;

import org.myspring.beans.PropertyEditorRegistrar;
import org.myspring.beans.PropertyEditorRegistry;
import org.myspring.beans.TypeConverter;
import org.myspring.beans.factory.BeanFactory;
import org.myspring.beans.factory.HierarchicalBeanFactory;
import org.myspring.core.convert.ConversionService;
import org.myspring.core.util.StringValueResolver;

import java.beans.PropertyEditor;

public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

    void setBeanClassLoader(ClassLoader beanClassLoader);

    ClassLoader getBeanClassLoader();

    void setTempClassLoader(ClassLoader tempClassLoader);

    ClassLoader getTempClassLoader();

    void setCacheBeanMetadata(boolean cacheBeanMetadata);

    boolean isCacheBeanMetadata();

    void setBeanExpressionResolver(BeanExpressionResolver resolver);

    BeanExpressionResolver getBeanExpressionResolver();

    void setConversionService(ConversionService conversionService);

    ConversionService getConversionService();

    void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

    void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

    void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

    void setTypeConverter(TypeConverter typeConverter);

    TypeConverter getTypeConverter();


    void addEmbeddedValueResolver(StringValueResolver valueResolver);

    boolean hasEmbeddedValueResolver();

    String resolveEmbeddedValue(String value);


    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);


}
