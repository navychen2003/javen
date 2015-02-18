package com.forizon.jimage.viewer.imagelist.wrapspi;

import com.forizon.jimage.viewer.imagelist.*;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ObjectWrapper implements ImageWrapper {
    final URIWrapper uriWrapper;
    final FileWrapper fileWrapper;

    public ObjectWrapper() {
        this(null, null);
    }

    public ObjectWrapper(URIWrapper uriWrapper, FileWrapper fileWrapper) {
        this.uriWrapper = (uriWrapper == null)? new URIWrapper() : uriWrapper;
        this.fileWrapper = (fileWrapper == null)? new FileWrapper() : fileWrapper;
    }

    @Override
    public ImageIdentity wrap(Object wrapee) throws WrapException {
        if (wrapee instanceof String) {
            return wrap((String)wrapee);
        } else {
            throw new WrapException("Type not supported");
        }
    }

    /**
     * Converts <code>{@link java.lang.String}</code>s into
     * <code>{@link com.?forizon.?jimage.?viewer.?imagelist.ImageIdentity}</code>
     */
    ImageIdentity wrap(String wrapee) throws WrapException {
        ImageIdentity imageIdentity;
        try {
            URI uri = new URI(wrapee);
            imageIdentity = uriWrapper.wrap(uri);
        } catch (URISyntaxException e) {
            File file = new File(wrapee);
            imageIdentity = fileWrapper.wrap(file);
        }
        return imageIdentity;
    }

    @Override
    public Class<Object> getWrappedType() {
        return Object.class;
    }
}

