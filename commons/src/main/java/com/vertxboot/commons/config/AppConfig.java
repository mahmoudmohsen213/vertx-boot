package com.vertxboot.commons.config;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface AppConfig {

    JsonObject getSync();

    Future<JsonObject> get();
}
