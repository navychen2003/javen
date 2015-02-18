package com.forizon.jimage.viewer.context;

import org.javenstudio.jfm.filesystems.JFMFile; 
import com.forizon.jimage.viewer.JImageView;
import com.forizon.jimage.viewer.imagelist.ImageCollection;
import com.forizon.jimage.viewer.imagelist.ImageFileDirectory;
import com.forizon.jimage.viewer.imagelist.JFMImageFileDirectory;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModel;
import com.forizon.jimage.viewer.imagelist.ImageIdentityListModelIterator;
import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import com.forizon.jimage.viewer.imagelist.wrapspi.ObjectImageWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.WrapException;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

@SuppressWarnings("rawtypes")
public class ImageContext {
    final protected Context context;
    protected PropertyChangeSupport changeSupport;
    protected ImageIdentityListModel imageIdentityListModel;
    protected ImageIdentityListModelIterator imageIterator;
    protected ObjectImageWrapper imageWrapper;
	protected Stack selected;

	public ImageContext(Context context) {
        this.context = context;
        this.selected = new Stack<Pair>();
    }

    public ImageIdentityListModelIterator getIterator() {
        return imageIterator;
    }

    public ImageIdentityListModel getImageIdentityListModel() {
        return imageIdentityListModel;
    }

    public ImageIdentity getImageIdentity() {
        ImageIdentity image;
        try {
            image = imageIterator.peekNext();
        } catch (NoSuchElementException e) {
            image = null;
        }
        return image;
    }

    public ImageIdentity getLastImageIdentity() {
        ImageIdentity imageIdentity = getImageIdentity();
        if (imageIdentity == null) {
            try {
                String fileName = context.configuration.getProperty(JImageView.CONFIGURATION_LASTIMAGE);
                if (fileName != null) {
                    imageIdentity = wrap(fileName);
                }
            } catch (WrapException e) {
                context.reporter.report(e);
            }
        }
        return imageIdentity;
    }

    public ObjectImageWrapper getImageWrapper() {
        return imageWrapper;
    }

    public ImageIdentity wrap(Object image) throws WrapException {
        return imageWrapper.wrap(image);
    }

    public void setImage(Object image) throws WrapException {
        ImageIdentity imageIdentity = wrap(image);
        if (imageIdentity instanceof ImageCollection) {
            setImage((ImageCollection)imageIdentity);
        } else {
            setImage(imageIdentity);
        }
    }

    @SuppressWarnings("unchecked")
	public void setImage(ImageIdentity imageIdentity) {
        if (imageIdentity == null) {
            imageIdentityListModel.clear();
        } else {
            try {
                imageIterator.set(imageIdentity);
            } catch (NoSuchElementException e) {
                imageIdentityListModel.clear();
                if (imageIdentity.getWrappedType().isAssignableFrom(File.class)) {
                    setImageFile(imageIdentity);
                } else if (imageIdentity.getWrappedType().isAssignableFrom(JFMFile.class)) {
                    setJFMImageFile(imageIdentity);
                } else {
                    imageIdentityListModel.add(imageIdentity);
                }
            }
            updateConfiguration(imageIdentity);
        }
    }

    @SuppressWarnings("unchecked")
	public void setImage(ImageCollection imageIdentity) {
        if (imageIdentity == null) {
            selected.clear();
            imageIdentityListModel.clear();
        } else {
            boolean found = false;
            if (!selected.empty()) {
                Iterator<ImageCollection> iterator = selected.iterator();
                ImageCollection element;
                while (iterator.hasNext()) {
                    element = iterator.next();
                    if (element.equals(imageIdentity)) {
                        found = true;
                    } else {
                        iterator.remove();
                    }
                }
            }
            if (!found) {
                selected.add(imageIdentity);
            }
            Collection<ImageIdentity> collection = imageIdentity.asCollection();
            try {
                imageIdentityListModel.replace(imageIdentity, collection);
                updateConfiguration(imageIdentity);
            } catch (IndexOutOfBoundsException e) {
                selected.clear();
                if (imageIdentity != null) {
                    imageIdentityListModel.addAll(collection);
                }
            }
        }
    }

    void setImageFile(ImageIdentity<File> imageIdentity)
        throws IllegalArgumentException
    {
        try {
            File parent = imageIdentity.getWrapped().getParentFile();
            setImage(new ImageFileDirectory<File>(parent,
                            context.imageContext.imageWrapper.get(File.class)));
            imageIterator.set(imageIdentity);
        } catch (NoSuchElementException e2) {
            context.reporter.report(e2);
        } catch (NullPointerException e2) {
            throw new IllegalArgumentException("imageIdentity.getWrapped() == null");
        }
    }

    void setJFMImageFile(ImageIdentity<JFMFile> imageIdentity)
        throws IllegalArgumentException
    {
        try {
            JFMFile parent = imageIdentity.getWrapped().getParentFile();
            setImage(new JFMImageFileDirectory<JFMFile>(parent,
                            context.imageContext.imageWrapper.get(JFMFile.class)));
            imageIterator.set(imageIdentity);
        } catch (NoSuchElementException e2) {
            context.reporter.report(e2);
        } catch (NullPointerException e2) {
            throw new IllegalArgumentException("imageIdentity.getWrapped() == null");
        }
    }

    @SuppressWarnings("unchecked")
	void updateConfiguration(ImageIdentity imageIdentity) {
        if (imageIdentity.hasRepresentation(URI.class)) {
            URI uri = (URI) imageIdentity.toType(URI.class);
            if (uri != null) {
                context.getConfiguration().setProperty(JImageView.CONFIGURATION_LASTIMAGE, uri.toString());
            }
        }
    }
}

@SuppressWarnings("rawtypes")
class Pair<K extends ImageCollection, V extends ImageIdentity> {
    final K key;
    final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
