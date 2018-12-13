package org.myspring.beans.propertyeditors;

import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ObjectUtils;
import org.myspring.core.util.StringUtils;

import java.beans.PropertyEditorSupport;

public class ClassArrayEditor extends PropertyEditorSupport {

    private final ClassLoader classLoader;


    /**
     * Create a default {@code ClassEditor}, using the thread
     * context {@code ClassLoader}.
     */
    public ClassArrayEditor() {
        this(null);
    }

    /**
     * Create a default {@code ClassArrayEditor}, using the given
     * {@code ClassLoader}.
     * @param classLoader the {@code ClassLoader} to use
     * (or pass {@code null} for the thread context {@code ClassLoader})
     */
    public ClassArrayEditor(ClassLoader classLoader) {
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    }


    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            String[] classNames = StringUtils.commaDelimitedListToStringArray(text);
            Class<?>[] classes = new Class<?>[classNames.length];
            for (int i = 0; i < classNames.length; i++) {
                String className = classNames[i].trim();
                classes[i] = ClassUtils.resolveClassName(className, this.classLoader);
            }
            setValue(classes);
        }
        else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        Class<?>[] classes = (Class[]) getValue();
        if (ObjectUtils.isEmpty(classes)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < classes.length; ++i) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(ClassUtils.getQualifiedName(classes[i]));
        }
        return sb.toString();
    }

}
