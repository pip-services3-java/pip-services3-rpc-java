package org.pipservices3.rpc.services;

@FunctionalInterface
public interface AuthorizeFunction<T, U, V> {
    public V apply(T t, U u);
}
