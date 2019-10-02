package com.vertxboot.beans;

import io.vertx.core.Future;

public interface Bean<T> {
    T getSync();

    Future<T> get();

    boolean isInitialized();
}
