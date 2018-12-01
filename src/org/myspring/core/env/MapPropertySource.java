package org.myspring.core.env;

import org.myspring.core.util.StringUtils;

import java.util.Map;

public class MapPropertySource extends EnumerablePropertySource<Map<String, Object>> {

    public MapPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }


    @Override
    public Object getProperty(String name) {
        return this.source.get(name);
    }

    @Override
    public boolean containsProperty(String name) {
        return this.source.containsKey(name);
    }

    @Override
    public String[] getPropertyNames() {
        return StringUtils.toStringArray(this.source.keySet());
    }

}
