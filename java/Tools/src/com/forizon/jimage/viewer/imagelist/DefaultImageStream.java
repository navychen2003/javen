package com.forizon.jimage.viewer.imagelist;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public abstract class DefaultImageStream
        extends DefaultImageWrapper<InputStream> {
	
    protected DefaultImageStream(InputStream stream, String name) {
        super(stream, name);
    }

    protected DefaultImageStream(String name) {
        super(null, name);
    }

    @Override
    public BufferedImage getImage() throws ImageException {
        BufferedImage result = null;
        try {
            InputStream stream = createInputStream();
            try {
                result = ImageIO.read(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw new ImageException(e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
	@Override
    public <E> E toType(Class<E> type) {
        return (type == String.class && name != null)? (E)name
                    : (E)super.toType(type);
    }

    protected abstract InputStream createInputStream() throws IOException;

    @Override
    public Class<InputStream> getWrappedType() {
        return InputStream.class;
    }
}

