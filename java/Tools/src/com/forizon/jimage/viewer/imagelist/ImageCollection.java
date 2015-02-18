package com.forizon.jimage.viewer.imagelist;

import java.util.Collection;

/**
 *
 * @author David
 */
@SuppressWarnings("rawtypes")
public interface ImageCollection<T extends ImageIdentity, U> extends ImageIdentity<U> {
    public Collection<T> asCollection();
    public boolean contains(T element);
}

