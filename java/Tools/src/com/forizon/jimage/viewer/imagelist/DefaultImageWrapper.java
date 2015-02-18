package com.forizon.jimage.viewer.imagelist;

/**
 *
 * @author David
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
abstract public class DefaultImageWrapper<T>
    extends AbstractImageIdentity<T> {
    protected T wrapped;
    protected String name;

    public DefaultImageWrapper(T wrapped, String name) {
        this.wrapped = wrapped;
        this.name = name;
    }

    public DefaultImageWrapper(T wrapped) {
        this(wrapped, null);
    }

    public DefaultImageWrapper() {
    }

    @Override
    public boolean hasRepresentation(Class type) {
        return type == wrapped.getClass()
                || type == String.class
                || super.hasRepresentation(type);
    }

    @Override
    public <E> E toType(Class<E> type)
            throws RepresentationException
    {
        E result;
        if (type == wrapped.getClass()) {
            result = (E)wrapped;
        } else if (type == String.class) {
            result = (E)name;
        } else {
            result = super.toType(type);
        }
        return result;
    }

    @Override
    public T getWrapped() {
        return wrapped;
    }

    public String getName() {
        return name; 
    }
}

