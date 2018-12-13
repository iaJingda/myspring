package org.myspring.beans.propertyeditors;

import org.myspring.core.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.TimeZone;

public class TimeZoneEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(StringUtils.parseTimeZoneString(text));
    }

    @Override
    public String getAsText() {
        TimeZone value = (TimeZone) getValue();
        return (value != null ? value.getID() : "");
    }
}
