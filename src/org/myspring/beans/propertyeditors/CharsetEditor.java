package org.myspring.beans.propertyeditors;

import org.myspring.core.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.nio.charset.Charset;

public class CharsetEditor   extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            setValue(Charset.forName(text));
        }
        else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        Charset value = (Charset) getValue();
        return (value != null ? value.name() : "");
    }

}
