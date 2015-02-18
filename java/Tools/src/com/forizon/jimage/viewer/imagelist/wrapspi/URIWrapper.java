package com.forizon.jimage.viewer.imagelist.wrapspi;

import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.net.URI;
import java.util.HashMap;

@SuppressWarnings("rawtypes")
public class URIWrapper implements ImageWrapper<URI> {
    HashMap<String, URISchemeWrapper> schemeHandlers;

    public URIWrapper() {
       this(new URLWrapper());
    }

    public URIWrapper(URISchemeWrapper catchAll) {
        if (catchAll == null) {
            throw new NullPointerException();
        }
        schemeHandlers = new HashMap<String, URISchemeWrapper>(2);
        schemeHandlers.put(null, catchAll);
    }

    public void setSchemeHandler(String scheme, URISchemeWrapper handler) {
        schemeHandlers.put(scheme, handler);
    }

    @Override
    public ImageIdentity wrap(URI wrapee) throws WrapException {
        ImageIdentity imageIdentity;
        String scheme = wrapee.getScheme();
        imageIdentity = (wrapee != null && schemeHandlers.containsKey(scheme))
            ? schemeHandlers.get(scheme).wrap(wrapee)
            : schemeHandlers.get(null).wrap(wrapee);
        return imageIdentity;
    }

    @Override
    public Class<URI> getWrappedType() {
        return URI.class;
    }
}

