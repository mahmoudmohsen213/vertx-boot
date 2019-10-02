package com.vertxboot.beans;

import io.vertx.core.Future;
import io.vertx.core.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SingletonBean<T> implements Bean<T> {
    private T bean;
    private volatile boolean initializationFlag;
    private final ReentrantLock initializationLock;
    private final Condition initializationMonitor;
    private final List<Promise<T>> waitingPromises;

    public SingletonBean() {
        this.bean = null;
        this.initializationFlag = false;
        this.initializationLock = new ReentrantLock();
        this.initializationMonitor = this.initializationLock.newCondition();
        this.waitingPromises = new ArrayList<>();
    }

    public SingletonBean<T> initialize(T bean) {
        this.initializationLock.lock();

        if (this.initializationFlag)
            throw new IllegalStateException("This bean object is already initialized");

        this.bean = bean;
        this.initializationFlag = true;
        this.initializationMonitor.signalAll();
        this.initializationLock.unlock();
        this.waitingPromises.forEach(waitingPromise -> waitingPromise.complete(this.bean));
        this.waitingPromises.clear();
        return this;
    }

    @Override
    public T getSync() {
        this.initializationLock.lock();
        try {
            while (!this.initializationFlag)
                this.initializationMonitor.await();
            this.initializationLock.unlock();
            return this.bean;
        } catch (Exception e) {
            this.initializationLock.unlock();
            throw new RuntimeException("Failed to get bean", e);
        }
    }

    @Override
    public Future<T> get() {
        Promise<T> promise = Promise.promise();
        this.initializationLock.lock();
        try {
            if (this.initializationFlag) {
                this.initializationLock.unlock();
                promise.complete(this.bean);
            } else {
                this.waitingPromises.add(promise);
                this.initializationLock.unlock();
            }
        } catch (Exception e) {
            this.initializationLock.unlock();
            promise.fail(e);
        }

        return promise.future();
    }

    @Override
    public boolean isInitialized() {
        return this.initializationFlag;
    }
}
