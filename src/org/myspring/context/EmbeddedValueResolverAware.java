package org.myspring.context;

import org.myspring.beans.factory.Aware;
import org.myspring.core.util.StringValueResolver;

public interface EmbeddedValueResolverAware extends Aware {

    void setEmbeddedValueResolver(StringValueResolver resolver);

}
