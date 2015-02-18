package com.forizon.jimage.viewer.imagelist.wrapspi;

import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.util.IdentityHashMap;
import java.util.Map;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ObjectImageWrapper implements ImageWrapper<Object> {
    final Map<Class, ImageWrapper> wrappers;

    public ObjectImageWrapper() {
        this(null);
    }

    public ObjectImageWrapper(Map<Class, ImageWrapper> wrappers) {
        if (wrappers == null) {
            this.wrappers = new IdentityHashMap<Class, ImageWrapper>(4);
        } else {
            this.wrappers = wrappers;
        }
    }

    public void put(ImageWrapper wrapper) {
        put(wrapper.getWrappedType(), wrapper);
    }

    public <E> void put(Class<E> wrappedType, ImageWrapper<E> wrapper) {
        wrappers.put(wrappedType, wrapper);
    }

    public <E> ImageWrapper<E> get(Class<E> wrappedType) {
        return wrappers.get(wrappedType);
    }

    @Override
    public ImageIdentity wrap(Object image) throws WrapException {
        ImageIdentity result = null;
        if (image != null) {
            ImageWrapper wrapper = null;
            Class current = image.getClass();
            while (wrapper == null && current != null) {
                wrapper = wrappers.get(current);
                current = current.getSuperclass();
            }
            if (wrapper != null) {
                result = wrapper.wrap(image);
            } else {
                Class[] classes = image.getClass().getInterfaces();
                for (int i = 0; wrapper == null && i < classes.length; i++) {
                    wrapper = wrappers.get(classes[i]);
                }
                if (wrapper != null) {
                    result = wrapper.wrap(image);
                }
            }
        }
        return result;
    }

    @Override
    public Class<Object> getWrappedType() {
        return Object.class;
    }
}

