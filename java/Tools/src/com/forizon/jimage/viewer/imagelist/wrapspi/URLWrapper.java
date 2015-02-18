package com.forizon.jimage.viewer.imagelist.wrapspi;

import com.forizon.jimage.viewer.imagelist.*;
import java.net.URI;
import java.net.URL;

public class URLWrapper implements ImageWrapper<URL>, URISchemeWrapper {
    @Override
    public ImageIdentity<URL> wrap(URL wrapee) throws WrapException {
        return new ImageURL(wrapee);
    }

    @SuppressWarnings("rawtypes")
	@Override
    public ImageIdentity wrap(URI wrapee) throws WrapException {
        try {
            return wrap(wrapee.toURL());
        } catch (Exception e) {
            throw new WrapException(e);
        }
    }

    @Override
    public Class<URL> getWrappedType() {
        return URL.class;
    }
}

