package com.vertxboot.beans;

import io.vertx.core.Future;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanFactory {
    private static final BeanFactory beanFactory = new BeanFactory();

    protected Map<Class<?>, Bean<?>> beanMap = new ConcurrentHashMap<>();

    private BeanFactory() {
    }

    public static BeanFactory instance() {
        return beanFactory;
    }

    @SuppressWarnings("unchecked")
    public <T> BeanFactory registerBean(Class<T> beanClass, Bean<T> bean) {
        Bean<T> oldBean = (Bean<T>) this.beanMap.put(beanClass, bean);
        if (oldBean instanceof PendingBean)
            ((PendingBean<T>) oldBean).initialize(bean);
        return this;
    }

    public <T> boolean has(Class<T> beanClass) {
        return this.beanMap.containsKey(beanClass);
    }

    public <T> boolean isInitialized(Class<T> beanClass) {
        return this.beanMap.containsKey(beanClass) && this.beanMap.get(beanClass).isInitialized();
    }

    @SuppressWarnings("unchecked")
    public <T> T getSync(Class<T> beanClass) {
        return (T) beanMap.computeIfAbsent(beanClass, key -> new PendingBean<T>()).getSync();
    }

    @SuppressWarnings("unchecked")
    public <T> Future<T> get(Class<T> beanClass) {
        return (Future<T>) beanMap.computeIfAbsent(beanClass, key -> new PendingBean<T>()).get();
    }

    private static class PendingBean<T> implements Bean<T> {
        private SingletonBean<Bean<T>> wrappedBean;

        PendingBean() {
            wrappedBean = new SingletonBean<>();
        }

        void initialize(Bean<T> bean) {
            wrappedBean.initialize(bean);
        }

        @Override
        public T getSync() {
            return wrappedBean.getSync().getSync();
        }

        @Override
        public Future<T> get() {
            return wrappedBean.get().compose(Bean::get);
        }

        @Override
        public boolean isInitialized() {
            return wrappedBean.isInitialized();
        }
    }
}
