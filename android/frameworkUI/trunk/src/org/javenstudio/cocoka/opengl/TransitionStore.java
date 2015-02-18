package org.javenstudio.cocoka.opengl;

import java.util.HashMap;

public class TransitionStore {
    private HashMap<Object, Object> mStorage = new HashMap<Object, Object>();

    public void put(Object key, Object value) {
        mStorage.put(key, value);
    }

    public <T> void putIfNotPresent(Object key, T valueIfNull) {
        mStorage.put(key, get(key, valueIfNull));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object key) {
        return (T) mStorage.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object key, T valueIfNull) {
        T value = (T) mStorage.get(key);
        return value == null ? valueIfNull : value;
    }

    public void clear() {
        mStorage.clear();
    }
}
