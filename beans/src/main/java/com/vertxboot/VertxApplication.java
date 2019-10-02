package com.vertxboot;

import com.vertxboot.beans.BeanLoader;

public class VertxApplication {
    public static void run(Class<?> primarySource, String... args) {
        BeanLoader.load(primarySource.getPackage().getName());
    }
}
