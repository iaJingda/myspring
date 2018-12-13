package org.myspring.aop.framework.adapter;

public abstract class GlobalAdvisorAdapterRegistry {

    private static AdvisorAdapterRegistry instance = new DefaultAdvisorAdapterRegistry();

    /**
     * Return the singleton {@link DefaultAdvisorAdapterRegistry} instance.
     */
    public static AdvisorAdapterRegistry getInstance() {
        return instance;
    }

    /**
     * Reset the singleton {@link DefaultAdvisorAdapterRegistry}, removing any
     * {@link AdvisorAdapterRegistry#registerAdvisorAdapter(AdvisorAdapter) registered}
     * adapters.
     */
    static void reset() {
        instance = new DefaultAdvisorAdapterRegistry();
    }
}
