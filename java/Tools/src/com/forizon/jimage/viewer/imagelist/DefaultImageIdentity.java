package com.forizon.jimage.viewer.imagelist;

import java.awt.Image;

public class DefaultImageIdentity extends DefaultImageWrapper<Image> {
    public DefaultImageIdentity(Image image) {
        this(image, null);
    }

    public DefaultImageIdentity(Image image, String name) {
        super(image, name);
    }

    @Override
    public Image getImage() {
        return wrapped;
    }

    @SuppressWarnings("unchecked")
	@Override
    public <E> E toType(Class<E> type) {
        return (type == String.class && name != null)? (E)name
                    : (E)super.toType(type);
    }

    @Override
    public Class<Image> getWrappedType() {
        return Image.class;
    }
}

