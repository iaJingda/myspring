package org.myspring.context.weaving;

import org.myspring.beans.factory.Aware;
import org.myspring.context.instrument.classloading.LoadTimeWeaver;

public interface LoadTimeWeaverAware extends Aware {

    void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver);

}
