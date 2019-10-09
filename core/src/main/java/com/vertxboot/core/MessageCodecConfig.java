package com.vertxboot.core;

import com.vertxboot.beans.BeanConfig;

import java.util.Collections;
import java.util.List;

public interface MessageCodecConfig {
    default List<Class<?>> builtInMessageClasses() {
        return Collections.emptyList();
    }

    @BeanConfig(async = false, overridable = true)
    static MessageCodecConfig messageCodecConfig() {
        return new MessageCodecConfig() {
        };
    }
}
