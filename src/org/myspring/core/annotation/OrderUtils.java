package org.myspring.core.annotation;

import org.myspring.core.util.ClassUtils;

import java.lang.annotation.Annotation;

public abstract class OrderUtils {

    private static Class<? extends Annotation> priorityAnnotationType = null;

    static {
        try {
            priorityAnnotationType = (Class<? extends Annotation>)
                    ClassUtils.forName("javax.annotation.Priority", OrderUtils.class.getClassLoader());
        }
        catch (Throwable ex) {
            // javax.annotation.Priority not available, or present but not loadable (on JDK 6)
        }
    }
    public static Integer getOrder(Class<?> type) {
        return getOrder(type, null);
    }

    public static Integer getOrder(Class<?> type, Integer defaultOrder) {
        Order order = AnnotationUtils.findAnnotation(type, Order.class);
        if (order != null) {
            return order.value();
        }
        Integer priorityOrder = getPriority(type);
        if (priorityOrder != null) {
            return priorityOrder;
        }
        return defaultOrder;
    }

    public static Integer getPriority(Class<?> type) {
        if (priorityAnnotationType != null) {
            Annotation priority = AnnotationUtils.findAnnotation(type, priorityAnnotationType);
            if (priority != null) {
                return (Integer) AnnotationUtils.getValue(priority);
            }
        }
        return null;
    }
}
