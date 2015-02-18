package com.forizon.jimage.viewer.imagelist.wrapspi;

import com.forizon.jimage.viewer.imagelist.ImageFile;
import com.forizon.jimage.viewer.imagelist.ImageFileDirectory;
import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.io.File;
import java.io.IOException;
import java.net.URI;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class FileWrapper<E extends File> implements ImageWrapper<E>, URISchemeWrapper {
    @Override
    public ImageIdentity<E> wrap(E wrapee) throws WrapException {
        ImageIdentity<E> result;
        E normalized = normalizeFile(wrapee);
        if (normalized.isFile()) {
            result = new ImageFile<E>(normalized);
        } else if (normalized.isDirectory()) {
            result = new ImageFileDirectory<E>(normalized, this);
        } else if (!normalized.exists()) {
            throw new WrapException("!exists(file)");
        } else {
            throw new WrapException("exists(file) && !isFile(file) && !isDirectory(file)");
        }
        return result;
    }

    @Override
    public ImageIdentity wrap(URI wrapee) throws WrapException {
        return wrap((E)new File(wrapee));
    }

    @Override
    public Class<E> getWrappedType() {
        return (Class<E>)File.class;
    }

    protected E normalizeFile(E wrapee) {
        java.io.File canonical;
        try {
            canonical = wrapee.getCanonicalFile();
        } catch (IOException e) {
            canonical = wrapee;
        }
        return (E)canonical;
    }
}

