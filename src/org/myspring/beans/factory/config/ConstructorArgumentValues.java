package org.myspring.beans.factory.config;

import org.myspring.beans.BeanMetadataElement;
import org.myspring.core.util.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConstructorArgumentValues {

    private final Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<Integer, ValueHolder>(0);

    private final List<ValueHolder> genericArgumentValues = new LinkedList<ValueHolder>();


    public static class ValueHolder implements BeanMetadataElement {

        private Object value;

        private String type;

        private String name;

        private Object source;

        private boolean converted = false;

        private Object convertedValue;

        public ValueHolder(Object value) {
            this.value = value;
        }

        public ValueHolder(Object value, String type) {
            this.value = value;
            this.type = type;
        }
        public ValueHolder(Object value, String type, String name) {
            this.value = value;
            this.type = type;
            this.name = name;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return this.value;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }
        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public void setSource(Object source) {
            this.source = source;
        }

        @Override
        public Object getSource() {
            return this.source;
        }

        public synchronized boolean isConverted() {
            return this.converted;
        }

        public synchronized void setConvertedValue(Object value) {
            this.converted = true;
            this.convertedValue = value;
        }

        public synchronized Object getConvertedValue() {
            return this.convertedValue;
        }

        private boolean contentEquals(ValueHolder other) {
            return (this == other ||
                    (ObjectUtils.nullSafeEquals(this.value, other.value) && ObjectUtils.nullSafeEquals(this.type, other.type)));
        }

        private int contentHashCode() {
            return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.type);
        }

        public ValueHolder copy() {
            ValueHolder copy = new ValueHolder(this.value, this.type, this.name);
            copy.setSource(this.source);
            return copy;
        }

    }
}
