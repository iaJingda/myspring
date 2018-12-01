package org.myspring.core.convert.converter;

public interface Converter<S, T> {

    T convert(S source);
}
