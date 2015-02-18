package com.forizon.jimage.viewer.imagelist.wrapspi;

import com.forizon.jimage.viewer.imagelist.*;

public interface ImageWrapper<E> {
    @SuppressWarnings("rawtypes")
	public ImageIdentity wrap(E wrapee) throws WrapException;
    public Class<E> getWrappedType();
}

