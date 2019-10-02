package com.vertxboot.commons.utils;

import java.util.function.Consumer;
import java.util.function.Function;

public class FutureUtils {
    public static <T> Function<T, T> peek(Consumer<T> peeker) {
        return t -> {
            peeker.accept(t);
            return t;
        };
    }
}
