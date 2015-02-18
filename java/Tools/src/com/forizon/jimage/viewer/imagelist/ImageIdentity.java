package com.forizon.jimage.viewer.imagelist;

import java.awt.Image;

/**
 *
 * @author David
 */
@SuppressWarnings("rawtypes")
public interface ImageIdentity<T> extends Comparable<ImageIdentity>
{
    public Image getImage() throws ImageException;

    public boolean hasRepresentation(Class type);

    /**
     * toType(URI.class) must never be null
     * @throws UnsupportedTypeException extends Exception
     * @throws RepresentationCouldNotBeCreated extends RuntimeException
     */
    public <E> E toType(Class<E> type);

    public T getWrapped();
    public Class<T> getWrappedType();
    public String getName(); 
}

