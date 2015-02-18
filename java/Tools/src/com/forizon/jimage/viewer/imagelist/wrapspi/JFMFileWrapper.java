package com.forizon.jimage.viewer.imagelist.wrapspi;

import org.javenstudio.jfm.filesystems.JFMFile; 
import org.javenstudio.jfm.filesystems.JFMFileSystem; 
import com.forizon.jimage.viewer.imagelist.JFMImageFile;
import com.forizon.jimage.viewer.imagelist.JFMImageFileDirectory;
import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.io.IOException;
import java.net.URI;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JFMFileWrapper<E extends JFMFile> implements ImageWrapper<E>, URISchemeWrapper {
    @Override
    public ImageIdentity<E> wrap(E wrapee) throws WrapException {
        ImageIdentity<E> result;
        E normalized = normalizeFile(wrapee);
        if (normalized.isFile()) {
            result = new JFMImageFile<E>(normalized);
        } else if (normalized.isDirectory()) {
            result = new JFMImageFileDirectory<E>(normalized, this);
        } else if (!normalized.exists()) {
            throw new WrapException("!exists(file)");
        } else {
            throw new WrapException("exists(file) && !isFile(file) && !isDirectory(file)");
        }
        return result;
    }

    @Override
    public ImageIdentity wrap(URI wrapee) throws WrapException {
        return wrap((E)JFMFileSystem.newFile(wrapee));
    }

    @Override
    public Class<E> getWrappedType() {
        return (Class<E>)JFMFile.class;
    }

    protected E normalizeFile(E wrapee) {
        JFMFile canonical;
        try {
            canonical = wrapee.getCanonicalFile();
        } catch (IOException e) {
            canonical = wrapee;
        }
        return (E)canonical;
    }
}

