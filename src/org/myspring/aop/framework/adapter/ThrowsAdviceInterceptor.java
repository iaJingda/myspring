package org.myspring.aop.framework.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myspring.aop.AfterAdvice;
import org.myspring.aop.aopalliance.intercept.MethodInterceptor;
import org.myspring.aop.aopalliance.intercept.MethodInvocation;
import org.myspring.core.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ThrowsAdviceInterceptor implements MethodInterceptor, AfterAdvice {

    private static final String AFTER_THROWING = "afterThrowing";

    private static final Log logger = LogFactory.getLog(ThrowsAdviceInterceptor.class);


    private final Object throwsAdvice;

    /** Methods on throws advice, keyed by exception class */
    private final Map<Class<?>, Method> exceptionHandlerMap = new HashMap<Class<?>, Method>();



    public ThrowsAdviceInterceptor(Object throwsAdvice) {
        Assert.notNull(throwsAdvice, "Advice must not be null");
        this.throwsAdvice = throwsAdvice;

        Method[] methods = throwsAdvice.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(AFTER_THROWING)) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1 || paramTypes.length == 4) {
                    Class<?> throwableParam = paramTypes[paramTypes.length - 1];
                    if (Throwable.class.isAssignableFrom(throwableParam)) {
                        // An exception handler to register...
                        this.exceptionHandlerMap.put(throwableParam, method);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Found exception handler method on throws advice: " + method);
                        }
                    }
                }
            }
        }

        if (this.exceptionHandlerMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one handler method must be found in class [" + throwsAdvice.getClass() + "]");
        }
    }


    /**
     * Return the number of handler methods in this advice.
     */
    public int getHandlerMethodCount() {
        return this.exceptionHandlerMap.size();
    }


    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        try {
            return mi.proceed();
        }
        catch (Throwable ex) {
            Method handlerMethod = getExceptionHandler(ex);
            if (handlerMethod != null) {
                invokeHandlerMethod(mi, ex, handlerMethod);
            }
            throw ex;
        }
    }

    /**
     * Determine the exception handle method for the given exception.
     * @param exception the exception thrown
     * @return a handler for the given exception type, or {@code null} if none found
     */
    private Method getExceptionHandler(Throwable exception) {
        Class<?> exceptionClass = exception.getClass();
        if (logger.isTraceEnabled()) {
            logger.trace("Trying to find handler for exception of type [" + exceptionClass.getName() + "]");
        }
        Method handler = this.exceptionHandlerMap.get(exceptionClass);
        while (handler == null && exceptionClass != Throwable.class) {
            exceptionClass = exceptionClass.getSuperclass();
            handler = this.exceptionHandlerMap.get(exceptionClass);
        }
        if (handler != null && logger.isDebugEnabled()) {
            logger.debug("Found handler for exception of type [" + exceptionClass.getName() + "]: " + handler);
        }
        return handler;
    }

    private void invokeHandlerMethod(MethodInvocation mi, Throwable ex, Method method) throws Throwable {
        Object[] handlerArgs;
        if (method.getParameterTypes().length == 1) {
            handlerArgs = new Object[] {ex};
        }
        else {
            handlerArgs = new Object[] {mi.getMethod(), mi.getArguments(), mi.getThis(), ex};
        }
        try {
            method.invoke(this.throwsAdvice, handlerArgs);
        }
        catch (InvocationTargetException targetEx) {
            throw targetEx.getTargetException();
        }
    }

}
