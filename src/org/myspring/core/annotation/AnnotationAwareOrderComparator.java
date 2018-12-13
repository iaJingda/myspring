package org.myspring.core.annotation;

import org.myspring.core.DecoratingProxy;
import org.myspring.core.OrderComparator;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AnnotationAwareOrderComparator  extends OrderComparator {

    public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();

    @Override
    protected Integer findOrder(Object obj) {
        // Check for regular Ordered interface
        Integer order = super.findOrder(obj);
        if (order != null) {
            return order;
        }

        // Check for @Order and @Priority on various kinds of elements
        if (obj instanceof Class) {
            return OrderUtils.getOrder((Class<?>) obj);
        }
        else if (obj instanceof Method) {
            Order ann = AnnotationUtils.findAnnotation((Method) obj, Order.class);
            if (ann != null) {
                return ann.value();
            }
        }
        else if (obj instanceof AnnotatedElement) {
            Order ann = AnnotationUtils.getAnnotation((AnnotatedElement) obj, Order.class);
            if (ann != null) {
                return ann.value();
            }
        }
        else if (obj != null) {
            order = OrderUtils.getOrder(obj.getClass());
            if (order == null && obj instanceof DecoratingProxy) {
                order = OrderUtils.getOrder(((DecoratingProxy) obj).getDecoratedClass());
            }
        }

        return order;
    }

    @Override
    public Integer getPriority(Object obj) {
        Integer priority = null;
        if (obj instanceof Class) {
            priority = OrderUtils.getPriority((Class<?>) obj);
        }
        else if (obj != null) {
            priority = OrderUtils.getPriority(obj.getClass());
            if (priority == null && obj instanceof DecoratingProxy) {
                priority = OrderUtils.getPriority(((DecoratingProxy) obj).getDecoratedClass());
            }
        }
        return priority;
    }

    public static void sort(List<?> list) {
        if (list.size() > 1) {
            Collections.sort(list, INSTANCE);
        }
    }

    public static void sort(Object[] array) {
        if (array.length > 1) {
            Arrays.sort(array, INSTANCE);
        }
    }

    public static void sortIfNecessary(Object value) {
        if (value instanceof Object[]) {
            sort((Object[]) value);
        }
        else if (value instanceof List) {
            sort((List<?>) value);
        }
    }
}
