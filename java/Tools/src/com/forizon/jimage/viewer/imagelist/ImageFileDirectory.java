package com.forizon.jimage.viewer.imagelist;

import com.forizon.jimage.viewer.file.FileFilter;
import com.forizon.jimage.viewer.file.ImageFileFilter;
import com.forizon.jimage.viewer.imagelist.wrapspi.FileWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.ImageWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.WrapException;
import java.awt.Image;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

/**
 * @todo Treat directories as images
 */
public class ImageFileDirectory<E extends File>
    extends ImageFile<E>
    implements ImageCollection<ImageIdentity<E>, E>
{
    final FileFilter filter;
    final Comparator<? super ImageIdentity<E>> comparator;
    final ImageWrapper<E> fileWrapper;

    SoftReference<File[]> files;
    SoftReference<TreeSet<ImageIdentity<E>>> collection;
    SoftReference<Image> cachedImage;

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public ImageFileDirectory(E folder,
                              String name,
                              ImageWrapper<E> fileWrapper,
                              FileFilter filter,
                              Comparator<? super ImageIdentity<E>> comparator)
    {
        super(folder, name);
        if (folder == null) {
            throw new IllegalArgumentException("folder == null");
        }
        if (fileWrapper == null) {
            this.fileWrapper = (ImageWrapper<E>)new FileWrapper();
        } else {
            this.fileWrapper = fileWrapper;
        }
        if (filter == null) {
            this.filter = ImageFileFilter.getInstance();
            //this.filter = ImageFileFilter.getInstanceAllowDirectory();
        } else {
            this.filter = filter;
        }
        this.comparator = comparator;
    }

    public ImageFileDirectory(E folder,
                              ImageWrapper<E> fileWrapper)
    {
        this(folder, folder.getName(), fileWrapper, null, null);
    }

    public ImageFileDirectory(E folder,
                              ImageWrapper<E> fileWrapper,
                              FileFilter filter,
                              Comparator<? super ImageIdentity<E>> comparator)
    {
        this(folder, folder.getName(), fileWrapper, filter, comparator);
    }

    public ImageFileDirectory(E folder, String name)
    {
        this(folder, name, null, null, null);
    }

    public ImageFileDirectory(E folder)
    {
        this(folder, folder.getName());
    }

    @Override
    //@todo this method needs to be improved
    public Image getImage() throws ImageException {
        Image image = (cachedImage == null)? null : cachedImage.get();
        if (image == null) {
            TreeSet<ImageIdentity<E>> images = asCollection();
            if (images.size() > 0) {
                Iterator<ImageIdentity<E>> iterator = images.iterator();
                ImageIdentity<E> imageIdentity;
                while (image == null && iterator.hasNext()) {
                    imageIdentity = iterator.next();
                    // Do not recurse
                    if (!(imageIdentity instanceof ImageCollection)) {
                        image = imageIdentity.getImage();
                    }
                }
            }
            if (image == null) {
                Icon icon = FileSystemView.getFileSystemView().getSystemIcon(this.wrapped);
                if (icon instanceof ImageIcon) {
                    image = ((ImageIcon)icon).getImage();
                }
            }
            cachedImage = new SoftReference<Image>(image);
        }
        return image;
    }

    public File[] asArray() {
        File[] lFiles = (files == null)? null : files.get();
        if (lFiles == null) {
            lFiles = wrapped.listFiles(filter);
            if (lFiles == null) {
                lFiles = new File[0];
            }
            files = new SoftReference<File[]>(lFiles);
        }
        return lFiles;
    }

    @SuppressWarnings("unchecked")
	@Override
    public TreeSet<ImageIdentity<E>> asCollection() {
        File[] lFiles = asArray();
        TreeSet<ImageIdentity<E>> images = (collection == null)? null
                                                               : collection.get();
        if (images == null) {
            images = new TreeSet<ImageIdentity<E>>(comparator);
            collection = new SoftReference<TreeSet<ImageIdentity<E>>>(images);
            // generate the collection from lFiles
            for (int i = 0; i < lFiles.length; i++) {
                try {
                    images.add(fileWrapper.wrap((E)lFiles[i]));
                } catch (WrapException e) {
                    // ignored
                }
            }
        }
        return images;
    }

    @Override
    public boolean contains(ImageIdentity<E> element) {
        boolean result = false;
        if (element != null) {
            File parent = ((File)element.getWrapped()).getParentFile();
            if (parent != null) {
                result = parent.equals((File)element.getWrapped());
            }
        }
        return result;
    }
}

