package org.myspring.beans.factory.config;

import org.myspring.beans.BeanMetadataElement;
import org.myspring.beans.factory.BeanFactoryUtils;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;
import org.myspring.core.util.StringUtils;

public class BeanDefinitionHolder implements BeanMetadataElement {

    private final BeanDefinition beanDefinition;

    private final String beanName;

    private final String[] aliases;

    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
        this(beanDefinition, beanName, null);
    }

    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, String[] aliases) {
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
        Assert.notNull(beanName, "Bean name must not be null");
        this.beanDefinition = beanDefinition;
        this.beanName = beanName;
        this.aliases = aliases;
    }

    public BeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder) {
        Assert.notNull(beanDefinitionHolder, "BeanDefinitionHolder must not be null");
        this.beanDefinition = beanDefinitionHolder.getBeanDefinition();
        this.beanName = beanDefinitionHolder.getBeanName();
        this.aliases = beanDefinitionHolder.getAliases();
    }

    public BeanDefinition getBeanDefinition() {
        return this.beanDefinition;
    }
    public String getBeanName() {
        return this.beanName;
    }

    /**
     * Return the alias names for the bean, as specified directly for the bean definition.
     * @return the array of alias names, or {@code null} if none
     */
    public String[] getAliases() {
        return this.aliases;
    }

    /**
     * Expose the bean definition's source object.
     * @see BeanDefinition#getSource()
     */
    @Override
    public Object getSource() {
        return this.beanDefinition.getSource();
    }

    /**
     * Determine whether the given candidate name matches the bean name
     * or the aliases stored in this bean definition.
     */
    public boolean matchesName(String candidateName) {
        return (candidateName != null && (candidateName.equals(this.beanName) ||
                candidateName.equals(BeanFactoryUtils.transformedBeanName(this.beanName)) ||
                ObjectUtils.containsElement(this.aliases, candidateName)));
    }


    /**
     * Return a friendly, short description for the bean, stating name and aliases.
     * @see #getBeanName()
     * @see #getAliases()
     */
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bean definition with name '").append(this.beanName).append("'");
        if (this.aliases != null) {
            sb.append(" and aliases [").append(StringUtils.arrayToCommaDelimitedString(this.aliases)).append("]");
        }
        return sb.toString();
    }

    /**
     * Return a long description for the bean, including name and aliases
     * as well as a description of the contained {@link BeanDefinition}.
     * @see #getShortDescription()
     * @see #getBeanDefinition()
     */
    public String getLongDescription() {
        StringBuilder sb = new StringBuilder(getShortDescription());
        sb.append(": ").append(this.beanDefinition);
        return sb.toString();
    }

    /**
     * This implementation returns the long description. Can be overridden
     * to return the short description or any kind of custom description instead.
     * @see #getLongDescription()
     * @see #getShortDescription()
     */
    @Override
    public String toString() {
        return getLongDescription();
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BeanDefinitionHolder)) {
            return false;
        }
        BeanDefinitionHolder otherHolder = (BeanDefinitionHolder) other;
        return this.beanDefinition.equals(otherHolder.beanDefinition) &&
                this.beanName.equals(otherHolder.beanName) &&
                ObjectUtils.nullSafeEquals(this.aliases, otherHolder.aliases);
    }

    @Override
    public int hashCode() {
        int hashCode = this.beanDefinition.hashCode();
        hashCode = 29 * hashCode + this.beanName.hashCode();
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.aliases);
        return hashCode;
    }
}
