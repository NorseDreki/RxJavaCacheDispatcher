package com.github.upelsin.rxJavaCacheDispatcher.test.mocks;

import com.github.upelsin.rxJavaCacheDispatcher.ICache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 16.03.2015.
 */
public class MockCache implements ICache {

    private final ConcurrentHashMap<String, Entry> storage;

    public MockCache() {
        storage = new ConcurrentHashMap<String, Entry>();
    }

    @Override
    public Entry get(String key) {
        return storage.get(key);
    }

    @Override
    public void put(String key, Entry entry) {
        storage.put(key, entry);
    }

    @Override
    public void remove(String key) {
        storage.remove(key);
    }

    @Override
    public void clear() {
        storage.clear();
    }
}
