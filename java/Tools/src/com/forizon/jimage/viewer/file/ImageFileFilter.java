package com.forizon.jimage.viewer.file;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;
import javax.imageio.ImageIO;

/**
 * FileFilter that accepts all files with suffixes readable by
 * <code>javax.imageio.ImageIO</code>. Directories are not accepted.
 * Wrap this with <code>{@link DirectoryFileFilter}</code> for a
 * FileFilter that accepts both images and directories
 * can be used by JFileChooser.
 */
public class ImageFileFilter extends FileFilter {
    // Cache of readable suffixes registed by ImageIO
    static String[] acceptedCache;
    static ImageFileFilter instance;
    static DirectoryFileFilter instanceAllowDirectory;

    String description;

    /**
     * Returns singleton instance
     * @return singleton instance
     */
    public static ImageFileFilter getInstance() {
        if (instance == null) {
           instance = new ImageFileFilter();
        }
        return instance;
    }

    /**
     * Returns singleton instance
     * @return singleton instance
     */
    public static DirectoryFileFilter getInstanceAllowDirectory() {
        if (instanceAllowDirectory == null) {
           instanceAllowDirectory = new DirectoryFileFilter(getInstance());
        }
        return instanceAllowDirectory;
    }

    public ImageFileFilter() {
        this("image/*");
    }

    /**
     * Constructs a FileFilter that only accepts files with extensions
     * recognized by <code>ImageIO</code>.
     * 
     * Directories are not accepted, if you want a filter that also accepts
     * directories, use
     * <code>{@link #getInstanceAllowDirectory() getInstanceAllowDirectory()}</code>
     * instead.
     */
    public ImageFileFilter(String description) {
        if (acceptedCache == null) {
            refreshAcceptedCache();
        }
        this.description = description;
    }

    /**
     * Updates the accepted file suffixes with the current list of image
     * readable suffixes registered with <code>ImageIO</code>.
     */
    public static void refreshAcceptedCache() {
        String[] raw = ImageIO.getReaderFileSuffixes();
        // Use TreeSet to sort and remove duplicates
        TreeSet<String> set = new TreeSet<String>();
        for (int i = 0; i < raw.length; i++) {
            set.add(raw[i].toLowerCase(Locale.ENGLISH));
        }
        acceptedCache = set.toArray(new String[0]);
    }

    /**
     * Returns a description of what is accepted by this filter
     * @return a description of what is accepted by this filter
     */
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean accept(File file) {
        boolean result = false;
        if (file != null) {
            String fileName = file.getName();
            int pos = fileName.lastIndexOf('.');
            if (pos >= 0 && pos != fileName.length() - 1) {
                String extension = fileName.substring(pos + 1).toLowerCase(Locale.ENGLISH);
                result = Arrays.binarySearch(acceptedCache, extension) >= 0
                            ? file.isFile() : false;
            }
        }
        return result;
    }
}

