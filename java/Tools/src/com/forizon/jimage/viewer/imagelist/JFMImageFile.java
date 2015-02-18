package com.forizon.jimage.viewer.imagelist;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import javax.imageio.ImageIO;

import org.javenstudio.jfm.filesystems.JFMFile; 

/**
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class JFMImageFile<E extends JFMFile>
    extends DefaultImageWrapper<E>
{
    SoftReference<BufferedImage> cache;

    public JFMImageFile(E file, String name) {
        super(file, name);
    }

    public JFMImageFile(E file) {
        this(file, file.getName());
    }

    @Override
    public boolean hasRepresentation(Class type) {
        return type == JFMFile.class || type == InputStream.class || type == URI.class
                                  || super.hasRepresentation(type);
    }

    @SuppressWarnings("hiding")
	@Override
    public <E> E toType(Class<E> type)
        throws RepresentationException
    {
        E result;
        if (type == JFMFile.class) {
            result = (E)wrapped;
        } else if (type == URI.class) {
            result = (E)wrapped.toURI();
        } else if (type == InputStream.class) {
            try {
                result = (E)wrapped.getInputStream(); 
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
        BufferedImage image = (cache == null)? null : cache.get();
        if (image == null) {
            try {
                image = ImageIO.read(wrapped.getInputStream());
                cache = new SoftReference<BufferedImage>(image);
            } catch (IOException e) {
                throw new ImageException(e);
            }
        }

        return image;
    }

    @Override
    public Class<E> getWrappedType() {
        return (Class<E>)JFMFile.class;
    }
}
