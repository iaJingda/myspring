package org.myspring.expression.spel.support;

import org.myspring.core.BridgeMethodResolver;
import org.myspring.core.MethodParameter;
import org.myspring.core.convert.TypeDescriptor;
import org.myspring.expression.*;
import org.myspring.expression.spel.SpelEvaluationException;
import org.myspring.expression.spel.SpelMessage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;

public class ReflectiveMethodResolver  implements MethodResolver {

    private final boolean useDistance;

    private Map<Class<?>, MethodFilter> filters;


    public ReflectiveMethodResolver() {
        this.useDistance = true;
    }

    public ReflectiveMethodResolver(boolean useDistance) {
        this.useDistance = useDistance;
    }

    public void registerMethodFilter(Class<?> type, MethodFilter filter) {
        if (this.filters == null) {
            this.filters = new HashMap<Class<?>, MethodFilter>();
        }
        if (filter != null) {
            this.filters.put(type, filter);
        }
        else {
            this.filters.remove(type);
        }
    }

    @Override
    public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
                                  List<TypeDescriptor> argumentTypes) throws AccessException {

        try {
            TypeConverter typeConverter = context.getTypeConverter();
            Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
            List<Method> methods = new ArrayList<Method>(getMethods(type, targetObject));

            // If a filter is registered for this type, call it
            MethodFilter filter = (this.filters != null ? this.filters.get(type) : null);
            if (filter != null) {
                List<Method> filtered = filter.filter(methods);
                methods = (filtered instanceof ArrayList ? filtered : new ArrayList<Method>(filtered));
            }

            // Sort methods into a sensible order
            if (methods.size() > 1) {
                Collections.sort(methods, new Comparator<Method>() {
                    @Override
                    public int compare(Method m1, Method m2) {
                        int m1pl = m1.getParameterTypes().length;
                        int m2pl = m2.getParameterTypes().length;
                        // varargs methods go last
                        if (m1pl == m2pl) {
                            if (!m1.isVarArgs() && m2.isVarArgs()) {
                                return -1;
                            }
                            else if (m1.isVarArgs() && !m2.isVarArgs()) {
                                return 1;
                            }
                            else {
                                return 0;
                            }
                        }
                        return (m1pl < m2pl ? -1 : (m1pl > m2pl ? 1 : 0));
                    }
                });
            }

            // Resolve any bridge methods
            for (int i = 0; i < methods.size(); i++) {
                methods.set(i, BridgeMethodResolver.findBridgedMethod(methods.get(i)));
            }

            // Remove duplicate methods (possible due to resolved bridge methods)
            Set<Method> methodsToIterate = new LinkedHashSet<Method>(methods);

            Method closeMatch = null;
            int closeMatchDistance = Integer.MAX_VALUE;
            Method matchRequiringConversion = null;
            boolean multipleOptions = false;

            for (Method method : methodsToIterate) {
                if (method.getName().equals(name)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    List<TypeDescriptor> paramDescriptors = new ArrayList<TypeDescriptor>(paramTypes.length);
                    for (int i = 0; i < paramTypes.length; i++) {
                        paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, i)));
                    }
                    ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
                    if (method.isVarArgs() && argumentTypes.size() >= (paramTypes.length - 1)) {
                        // *sigh* complicated
                        matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
                    }
                    else if (paramTypes.length == argumentTypes.size()) {
                        // Name and parameter number match, check the arguments
                        matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
                    }
                    if (matchInfo != null) {
                        if (matchInfo.isExactMatch()) {
                            return new ReflectiveMethodExecutor(method);
                        }
                        else if (matchInfo.isCloseMatch()) {
                            if (this.useDistance) {
                                int matchDistance = ReflectionHelper.getTypeDifferenceWeight(paramDescriptors, argumentTypes);
                                if (closeMatch == null || matchDistance < closeMatchDistance) {
                                    // This is a better match...
                                    closeMatch = method;
                                    closeMatchDistance = matchDistance;
                                }
                            }
                            else {
                                // Take this as a close match if there isn't one already
                                if (closeMatch == null) {
                                    closeMatch = method;
                                }
                            }
                        }
                        else if (matchInfo.isMatchRequiringConversion()) {
                            if (matchRequiringConversion != null) {
                                multipleOptions = true;
                            }
                            matchRequiringConversion = method;
                        }
                    }
                }
            }
            if (closeMatch != null) {
                return new ReflectiveMethodExecutor(closeMatch);
            }
            else if (matchRequiringConversion != null) {
                if (multipleOptions) {
                    throw new SpelEvaluationException(SpelMessage.MULTIPLE_POSSIBLE_METHODS, name);
                }
                return new ReflectiveMethodExecutor(matchRequiringConversion);
            }
            else {
                return null;
            }
        }
        catch (EvaluationException ex) {
            throw new AccessException("Failed to resolve method", ex);
        }
    }

    private Set<Method> getMethods(Class<?> type, Object targetObject) {
        if (targetObject instanceof Class) {
            Set<Method> result = new LinkedHashSet<Method>();
            // Add these so that static methods are invocable on the type: e.g. Float.valueOf(..)
            Method[] methods = getMethods(type);
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    result.add(method);
                }
            }
            // Also expose methods from java.lang.Class itself
            result.addAll(Arrays.asList(getMethods(Class.class)));
            return result;
        }
        else if (Proxy.isProxyClass(type)) {
            Set<Method> result = new LinkedHashSet<Method>();
            // Expose interface methods (not proxy-declared overrides) for proper vararg introspection
            for (Class<?> ifc : type.getInterfaces()) {
                Method[] methods = getMethods(ifc);
                for (Method method : methods) {
                    if (isCandidateForInvocation(method, type)) {
                        result.add(method);
                    }
                }
            }
            return result;
        }
        else {
            Set<Method> result = new LinkedHashSet<Method>();
            Method[] methods = getMethods(type);
            for (Method method : methods) {
                if (isCandidateForInvocation(method, type)) {
                    result.add(method);
                }
            }
            return result;
        }
    }
    protected Method[] getMethods(Class<?> type) {
        return type.getMethods();
    }
    protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
        return true;
    }


}
