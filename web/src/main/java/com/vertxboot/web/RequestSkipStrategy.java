package com.vertxboot.web;

import io.vertx.ext.web.RoutingContext;

@FunctionalInterface
public interface RequestSkipStrategy {
    boolean doSkip(RoutingContext routingContext);
}
