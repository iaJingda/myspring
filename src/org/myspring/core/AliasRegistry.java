package org.myspring.core;

public interface AliasRegistry {

    void registerAlias(String name, String alias);

    /**
     * Remove the specified alias from this registry.
     * @param alias the alias to remove
     * @throws IllegalStateException if no such alias was found
     */
    void removeAlias(String alias);

    /**
     * Determine whether this given name is defines as an alias
     * (as opposed to the name of an actually registered component).
     * @param name the name to check
     * @return whether the given name is an alias
     */
    boolean isAlias(String name);

    /**
     * Return the aliases for the given name, if defined.
     * @param name the name to check for aliases
     * @return the aliases, or an empty array if none
     */
    String[] getAliases(String name);
}
