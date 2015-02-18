package com.forizon.jimage.viewer.imagelist;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 * @author David
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class AbstractImageIdentity<T> implements ImageIdentity<T> {
    private URI cacheURI;

    @Override
    public int hashCode() {
        if (cacheURI == null) {
            cacheURI(true);
        }
        return cacheURI.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ImageIdentity)
            ? compareTo((ImageIdentity)obj) == 0
            : false;
    }

	@Override
    public boolean hasRepresentation(Class type) {
        return type == String.class || type.isAssignableFrom(getWrappedType());
    }

	@Override
    public <E> E toType(Class<E> type)
            throws RepresentationException {
        E result = null;
        if (type == String.class) {
            result = (E)toString();
        } else if (type == URI.class) {
            if (cacheURI == null) {
                cacheURI();
            }
            result = (E)cacheURI;
        } else if (type.isAssignableFrom(getWrappedType())) {
            try {
                result = type.cast(getWrapped());
            } catch (RepresentationException e) {
                // getWrappedType() is not up to spec
                throw new RepresentationException(e);
            }
        }
        return result;
    }

    @Override
    public int compareTo(ImageIdentity other) {
        // NullPointerException if other == null
        URI otherURI = (URI)other.toType(URI.class);

        if (cacheURI == null) {
            cacheURI(true);
        }
        return cacheURI.compareTo(otherURI);
    }

    @Override
    public String toString() {
        return toType(URI.class).toString();
    }

    final void cacheURI(boolean useToRepresentation) {
        if (useToRepresentation) {
            cacheURI = toType(URI.class);
            if (cacheURI == null) {
                cacheURI();
            }
        } else {
            cacheURI();
        }
        assert cacheURI != null;
    }

    final void cacheURI() throws RepresentationException {
        Class wrappedType = getWrappedType();
        String wrapped = (wrappedType != null)
                       ? "(" + wrappedType.getCanonicalName() + ")" : "";
        try {
            // Escaping should be done by the URI constructor
            cacheURI = new URI("object",
                               getClass().getCanonicalName() + wrapped,
                               Integer.toHexString(hashCode()));
        } catch (URISyntaxException e) {
            // Should not happen since the set of legal java identifier
            // chars union {'(', ')', and '.'} are either valid URI
            // characters or should be "quoted" by
            // URI(String, String, String)
            throw new RepresentationException(e);
            //assert false;
        }
    }
}
