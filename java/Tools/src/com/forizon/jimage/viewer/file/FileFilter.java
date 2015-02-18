package com.forizon.jimage.viewer.file;

/**
 * Workaround javax.swing.filechooser.FileFilter implementing java.io.FileFilter
 * and not declaring it
 */
public abstract class FileFilter extends javax.swing.filechooser.FileFilter
        implements java.io.FileFilter
{}
