package com.vertxboot.core;

import java.util.List;

public interface MessageCodecConfig {
    List<Class<?>> builtInMessageClasses();
}
