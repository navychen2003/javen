package com.forizon.jimage.viewer.imagelist;

import java.util.Collection;

@SuppressWarnings("rawtypes")
public interface ImageSetLister<E extends ImageCollection> {
    public Collection<E> list(ImageIdentity image);
}

