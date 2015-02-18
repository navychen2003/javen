package com.forizon.jimage.viewer.imagelist.wrapspi;

import com.forizon.jimage.viewer.imagelist.DefaultImageIdentity;
import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.awt.Image;

public class DefaultImageWrapper implements ImageWrapper<Image> {
    @Override
    public ImageIdentity<Image> wrap(Image wrapee) {
        return new DefaultImageIdentity(wrapee);
    }

    @Override
    public Class<Image> getWrappedType() {
        return Image.class;
    }
}

