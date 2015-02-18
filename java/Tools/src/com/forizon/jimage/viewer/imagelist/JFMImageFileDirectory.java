package com.forizon.jimage.viewer.imagelist;

import org.javenstudio.jfm.filesystems.JFMFile; 
import com.forizon.jimage.viewer.file.FileFilter;
import com.forizon.jimage.viewer.file.ImageFileFilter;
import com.forizon.jimage.viewer.imagelist.wrapspi.JFMFileWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.ImageWrapper;
import com.forizon.jimage.viewer.imagelist.wrapspi.WrapException;
import java.awt.Image;
import java.lang.ref.SoftReference;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * @todo Treat directories as images
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class JFMImageFileDirectory<E extends JFMFile>
    extends JFMImageFile<E>
    implements ImageCollection<ImageIdentity<E>, E>
{
    final FileFilter filter;
    final Comparator<? super ImageIdentity<E>> comparator;
    final ImageWrapper<E> fileWrapper;

    SoftReference<JFMFile[]> files;
    SoftReference<TreeSet<ImageIdentity<E>>> collection;
    SoftReference<Image> cachedImage;

	public JFMImageFileDirectory(E folder,
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
            this.fileWrapper = (ImageWrapper<E>)new JFMFileWrapper();
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

    public JFMImageFileDirectory(E folder,
                              ImageWrapper<E> fileWrapper)
    {
        this(folder, folder.getName(), fileWrapper, null, null);
    }

    public JFMImageFileDirectory(E folder,
                              ImageWrapper<E> fileWrapper,
                              FileFilter filter,
                              Comparator<? super ImageIdentity<E>> comparator)
    {
        this(folder, folder.getName(), fileWrapper, filter, comparator);
    }

    public JFMImageFileDirectory(E folder, String name)
    {
        this(folder, name, null, null, null);
    }

    public JFMImageFileDirectory(E folder)
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
                Icon icon = this.wrapped.getIcon(); 
                if (icon instanceof ImageIcon) {
                    image = ((ImageIcon)icon).getImage();
                }
            }
            cachedImage = new SoftReference<Image>(image);
        }
        return image;
    }

    public JFMFile[] asArray() {
        JFMFile[] lFiles = (files == null)? null : files.get();
        if (lFiles == null) {
            try {
              lFiles = wrapped.listFiles();
            } catch (Exception e) {
              lFiles = null; 
            }
            if (lFiles == null) {
                lFiles = new JFMFile[0];
            }
            files = new SoftReference<JFMFile[]>(lFiles);
        }
        return lFiles;
    }

    @Override
    public TreeSet<ImageIdentity<E>> asCollection() {
        JFMFile[] lFiles = asArray();
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
            JFMFile parent = ((JFMFile)element.getWrapped()).getParentFile();
            if (parent != null) {
                result = parent.equals((JFMFile)element.getWrapped());
            }
        }
        return result;
    }
}

