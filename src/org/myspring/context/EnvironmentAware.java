package org.myspring.context;

import org.myspring.beans.factory.Aware;
import org.myspring.core.env.Environment;

public interface EnvironmentAware extends Aware {

    void setEnvironment(Environment environment);

}
