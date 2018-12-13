package org.myspring.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

interface AnnotationAttributeExtractor<S> {

    Class<? extends Annotation> getAnnotationType();

    Object getAnnotatedElement();

    S getSource();

    Object getAttributeValue(Method attributeMethod);


}
