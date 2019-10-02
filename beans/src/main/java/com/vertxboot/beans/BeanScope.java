package com.vertxboot.beans;

import io.vertx.core.Future;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public enum BeanScope {
    SINGLETON {
        @SuppressWarnings("unchecked")
        public <T> Bean<T> create(Class<T> beanClass, Method beanConfig, List<Object> beanDependencyList) {
            SingletonBean<T> singletonBean = new SingletonBean<>();
            try {
                if (beanConfig.getAnnotation(BeanConfig.class).async()) {
                    ((Future<T>) (beanConfig.invoke(null, beanDependencyList.toArray()))).setHandler(
                            beanAsyncResult -> singletonBean.initialize(beanAsyncResult.result()));
                } else {
                    singletonBean.initialize((T) beanConfig.invoke(null, beanDependencyList.toArray()));
                }
            } catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
                throw new RuntimeException("Singleton bean creation failed", e);
            }

            return singletonBean;
        }
    },
    PROTOTYPE {
        @SuppressWarnings("unchecked")
        public <T> Bean<T> create(Class<T> beanClass, Method beanConfig, List<Object> beanDependencyList) {
            PrototypeBean<T> prototypeBean = new PrototypeBean<>();
            if (beanConfig.getAnnotation(BeanConfig.class).async()) {
                throw new IllegalArgumentException("Illegal bean config arguments, prototype scope is not allowed for async beans");
            } else {
                prototypeBean.initialize(() -> {
                    try {
                        return (T) beanConfig.invoke(null, beanDependencyList.toArray());
                    } catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
                        throw new RuntimeException("Prototype bean creation failed", e);
                    }
                });
            }

            return prototypeBean;
        }
    };

    public abstract <T> Bean<T> create(Class<T> beanClass, Method beanConfig, List<Object> beanDependencyList);
}
