package com.vertxboot.beans.impl;

import com.vertxboot.beans.SingletonBean;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class AppConfigImpl {
    private SingletonBean<JsonObject> configSingletonBean;

    private AppConfigImpl(JsonObject config) {
        this.configSingletonBean = new SingletonBean<>();
        this.configSingletonBean.initialize(config);
    }

    public JsonObject getSync() {
        return this.configSingletonBean.getSync();
    }

    public Future<JsonObject> get() {
        return this.configSingletonBean.get();
    }
}
