package com.forizon.jimage.viewer.imagelist;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 *
 * @author David
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ImageURL extends DefaultImageWrapper<URL> {
    public ImageURL(URL url, String name) {
        super(url, name);
    }

    public ImageURL(URL url) {
        this(url, url.toString());
    }

    @Override
    public boolean hasRepresentation(Class type) {
        return type == URI.class || type == URL.class || type == InputStream.class
                                  || super.hasRepresentation(type);
    }

    @Override
    public <E> E toType(Class<E> type)
        throws RepresentationException
    {
        E result;
        if (type == URI.class) {
            try {
                result = (E)wrapped.toURI();
            } catch (URISyntaxException e) {
                throw new RepresentationException(e);
            }
        } else if (type == InputStream.class) {
            try {
                result = (E)wrapped.openStream();
            } catch (IOException e) {
                throw new RepresentationException(e);
            }
        } else {
            result = super.toType(type);
        }
        return result;
    }

    @Override
    public Image getImage()
        throws ImageException
    {
        try {
            return ImageIO.read(wrapped);
        } catch (IOException e) {
            throw new ImageException(e);
        }
    }

    @Override
    public Class<URL> getWrappedType() {
        return URL.class;
    }
}

