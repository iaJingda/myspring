package org.myspring.beans;

import org.myspring.core.ResolvableType;
import org.myspring.core.convert.Property;
import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.*;

public class BeanWrapperImpl  extends AbstractNestablePropertyAccessor implements BeanWrapper {

    private CachedIntrospectionResults cachedIntrospectionResults;

    /**
     * The security context used for invoking the property methods
     */
    private AccessControlContext acc;


    /**
     * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
     * Registers default editors.
     * @see #setWrappedInstance
     */
    public BeanWrapperImpl() {
        this(true);
    }

    /**
     * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
     * @param registerDefaultEditors whether to register default editors
     * (can be suppressed if the BeanWrapper won't need any type conversion)
     * @see #setWrappedInstance
     */
    public BeanWrapperImpl(boolean registerDefaultEditors) {
        super(registerDefaultEditors);
    }

    /**
     * Create a new BeanWrapperImpl for the given object.
     * @param object object wrapped by this BeanWrapper
     */
    public BeanWrapperImpl(Object object) {
        super(object);
    }

    /**
     * Create a new BeanWrapperImpl, wrapping a new instance of the specified class.
     * @param clazz class to instantiate and wrap
     */
    public BeanWrapperImpl(Class<?> clazz) {
        super(clazz);
    }

    /**
     * Create a new BeanWrapperImpl for the given object,
     * registering a nested path that the object is in.
     * @param object object wrapped by this BeanWrapper
     * @param nestedPath the nested path of the object
     * @param rootObject the root object at the top of the path
     */
    public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
        super(object, nestedPath, rootObject);
    }

    /**
     * Create a new BeanWrapperImpl for the given object,
     * registering a nested path that the object is in.
     * @param object object wrapped by this BeanWrapper
     * @param nestedPath the nested path of the object
     * @param parent the containing BeanWrapper (must not be {@code null})
     */
    private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
        super(object, nestedPath, parent);
        setSecurityContext(parent.acc);
    }


    /**
     * Set a bean instance to hold, without any unwrapping of {@link java.util.Optional}.
     * @param object the actual target object
     * @since 4.3
     * @see #setWrappedInstance(Object)
     */
    public void setBeanInstance(Object object) {
        this.wrappedObject = object;
        this.rootObject = object;
        this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
        setIntrospectionClass(object.getClass());
    }

    @Override
    public void setWrappedInstance(Object object, String nestedPath, Object rootObject) {
        super.setWrappedInstance(object, nestedPath, rootObject);
        setIntrospectionClass(getWrappedClass());
    }

    /**
     * Set the class to introspect.
     * Needs to be called when the target object changes.
     * @param clazz the class to introspect
     */
    protected void setIntrospectionClass(Class<?> clazz) {
        if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
            this.cachedIntrospectionResults = null;
        }
    }

    /**
     * Obtain a lazily initializted CachedIntrospectionResults instance
     * for the wrapped object.
     */
    private CachedIntrospectionResults getCachedIntrospectionResults() {
        Assert.state(getWrappedInstance() != null, "BeanWrapper does not hold a bean instance");
        if (this.cachedIntrospectionResults == null) {
            this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
        }
        return this.cachedIntrospectionResults;
    }

    /**
     * Set the security context used during the invocation of the wrapped instance methods.
     * Can be null.
     */
    public void setSecurityContext(AccessControlContext acc) {
        this.acc = acc;
    }

    /**
     * Return the security context used during the invocation of the wrapped instance methods.
     * Can be null.
     */
    public AccessControlContext getSecurityContext() {
        return this.acc;
    }


    /**
     * Convert the given value for the specified property to the latter's type.
     * <p>This method is only intended for optimizations in a BeanFactory.
     * Use the {@code convertIfNecessary} methods for programmatic conversion.
     * @param value the value to convert
     * @param propertyName the target property
     * (note that nested or indexed properties are not supported here)
     * @return the new value, possibly the result of type conversion
     * @throws TypeMismatchException if type conversion failed
     */
    public Object convertForProperty(Object value, String propertyName) throws TypeMismatchException {
        CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
        PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
                    "No property '" + propertyName + "' found");
        }
        TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
        if (td == null) {
            td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
        }
        return convertForProperty(propertyName, null, value, td);
    }

    private Property property(PropertyDescriptor pd) {
        GenericTypeAwarePropertyDescriptor gpd = (GenericTypeAwarePropertyDescriptor) pd;
        return new Property(gpd.getBeanClass(), gpd.getReadMethod(), gpd.getWriteMethod(), gpd.getName());
    }

    @Override
    protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
        PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
        return (pd != null ? new BeanPropertyHandler(pd) : null);
    }

    @Override
    protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
        return new BeanWrapperImpl(object, nestedPath, this);
    }

    @Override
    protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
        PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
        throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName,
                matches.buildErrorMessage(), matches.getPossibleMatches());
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return getCachedIntrospectionResults().getPropertyDescriptors();
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
        BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
        String finalPath = getFinalPath(nestedBw, propertyName);
        PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
                    "No property '" + propertyName + "' found");
        }
        return pd;
    }


    private class BeanPropertyHandler extends PropertyHandler {

        private final PropertyDescriptor pd;

        public BeanPropertyHandler(PropertyDescriptor pd) {
            super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
            this.pd = pd;
        }

        @Override
        public ResolvableType getResolvableType() {
            return ResolvableType.forMethodReturnType(this.pd.getReadMethod());
        }

        @Override
        public TypeDescriptor toTypeDescriptor() {
            return new TypeDescriptor(property(this.pd));
        }

        @Override
        public TypeDescriptor nested(int level) {
            return TypeDescriptor.nested(property(pd), level);
        }

        @Override
        public Object getValue() throws Exception {
            final Method readMethod = this.pd.getReadMethod();
            if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers()) && !readMethod.isAccessible()) {
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            readMethod.setAccessible(true);
                            return null;
                        }
                    });
                }
                else {
                    readMethod.setAccessible(true);
                }
            }
            if (System.getSecurityManager() != null) {
                try {
                    return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            return readMethod.invoke(getWrappedInstance(), (Object[]) null);
                        }
                    }, acc);
                }
                catch (PrivilegedActionException pae) {
                    throw pae.getException();
                }
            }
            else {
                return readMethod.invoke(getWrappedInstance(), (Object[]) null);
            }
        }

        @Override
        public void setValue(final Object object, Object valueToApply) throws Exception {
            final Method writeMethod = (this.pd instanceof GenericTypeAwarePropertyDescriptor ?
                    ((GenericTypeAwarePropertyDescriptor) this.pd).getWriteMethodForActualAccess() :
                    this.pd.getWriteMethod());
            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers()) && !writeMethod.isAccessible()) {
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            writeMethod.setAccessible(true);
                            return null;
                        }
                    });
                }
                else {
                    writeMethod.setAccessible(true);
                }
            }
            final Object value = valueToApply;
            if (System.getSecurityManager() != null) {
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            writeMethod.invoke(object, value);
                            return null;
                        }
                    }, acc);
                }
                catch (PrivilegedActionException ex) {
                    throw ex.getException();
                }
            }
            else {
                writeMethod.invoke(getWrappedInstance(), value);
            }
        }
    }

}
