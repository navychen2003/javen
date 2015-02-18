package com.forizon.jimage.viewer.imagelist;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ImageFile<E extends File>
    extends DefaultImageWrapper<E> {
	
    SoftReference<BufferedImage> cache;

    public ImageFile(E file, String name) {
        super(file, name);
    }

    public ImageFile(E file) {
        this(file, file.getName());
    }

    @Override
    public boolean hasRepresentation(Class type) {
        return type == File.class || type == URL.class || type == InputStream.class || type == URI.class
                                  || super.hasRepresentation(type);
    }

    @SuppressWarnings("hiding")
	@Override
    public <E> E toType(Class<E> type)
        throws RepresentationException
    {
        E result;
        if (type == File.class) {
            result = (E)wrapped;
        } else if (type == URI.class) {
            result = (E)wrapped.toURI();
        } else if (type == InputStream.class) {
            try {
                result = (E)new FileInputStream(wrapped);
            } catch (FileNotFoundException e) {
                throw new RepresentationException(e);
            }
        } else if (type == URL.class) {
            try {
                result = (E)toType(URI.class).toURL();
            } catch (MalformedURLException e) {
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
        BufferedImage image = (cache == null)? null : cache.get();
        if (image == null) {
            try {
                image = ImageIO.read(wrapped);
                cache = new SoftReference<BufferedImage>(image);
            } catch (IOException e) {
                throw new ImageException(e);
            }
        }

        return image;
    }

    @Override
    public Class<E> getWrappedType() {
        return (Class<E>)File.class;
    }
}
