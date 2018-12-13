package org.myspring.context;

import org.myspring.core.ResolvableType;
import org.myspring.core.ResolvableTypeProvider;
import org.myspring.core.util.Assert;

public class PayloadApplicationEvent<T> extends ApplicationEvent implements ResolvableTypeProvider {
    private final T payload;

    public PayloadApplicationEvent(Object source, T payload) {
        super(source);
        Assert.notNull(payload, "Payload must not be null");
        this.payload = payload;
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(getPayload()));
    }

    /**
     * Return the payload of the event.
     */
    public T getPayload() {
        return this.payload;
    }
}
