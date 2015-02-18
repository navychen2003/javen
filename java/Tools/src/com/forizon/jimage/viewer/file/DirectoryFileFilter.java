package com.forizon.jimage.viewer.file;

import java.io.File;

/**
 * A class that extends <code>javax.swing.filechooser.FileFilter</code>,
 * implements <code>java.io.FileFilter</code> and wraps <code>Object</code>s.
 * The filter accepts all directories and will try to use decorated objects'
 * <code>boolean accept(File file)</code>, and
 * <code>Object getDescription()</code> methods if implemented and accessable.
 */
public class DirectoryFileFilter extends FileFilter {
    /** Decorated file filter */
    final FileFilter filter;
    final String description;

    public DirectoryFileFilter() {
        this(null, null);
    }

    public DirectoryFileFilter(String aDescription) {
        this(null, aDescription);
    }

    public DirectoryFileFilter(FileFilter aFilter) {
        this(aFilter, null);
    }

    /**
     * Constructs an instance of <code>DirectoryFileFilterDecorator</code>
     * 
     * @param aFilter
     */
    public DirectoryFileFilter(FileFilter aFilter, String aDescription) {
        filter = aFilter;
        description = aDescription;
    }

    @Override
    public boolean accept(File file) {
        return file != null && ((filter != null && filter.accept(file))
                                || file.isDirectory());
    }

    @Override
    public String getDescription() {
        return (description == null && filter != null)
                ? filter.getDescription()
                : description;
    }
}
