package org.javenstudio.falcon.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collection;

public class FileUtils {
	
  /**
   * Resolves a path relative a base directory.
   *
   * <p>
   * This method does what "new File(base,path)" <b>Should</b> do, if it wasn't
   * completely lame: If path is absolute, then a File for that path is returned;
   * if it's not absolute, then a File is returned using "path" as a child
   * of "base")
   * </p>
   */
  public static File resolvePath(File base, String path) {
    File r = new File(path);
    return r.isAbsolute() ? r : new File(base, path);
  }

  public static void copyFile(File src , File destination) throws IOException {
    FileChannel in = null;
    FileChannel out = null;
    try {
      in = new FileInputStream(src).getChannel();
      out = new FileOutputStream(destination).getChannel();
      in.transferTo(0, in.size(), out);
    } finally {
      try { if (in != null) in.close(); } catch (IOException e) {}
      try { if (out != null) out.close(); } catch (IOException e) {}
    }
  }

  /**
   * Copied from Lucene's FSDirectory.fsync(String)
   *
   * @param fullFile the File to be synced to disk
   * @throws IOException if the file could not be synced
   */
  public static void sync(File fullFile) throws IOException  {
    if (fullFile == null || !fullFile.exists())
      throw new FileNotFoundException("File does not exist " + fullFile);

    boolean success = false;
    int retryCount = 0;
    IOException exc = null;
    while(!success && retryCount < 5) {
      retryCount++;
      RandomAccessFile file = null;
      try {
        try {
          file = new RandomAccessFile(fullFile, "rw");
          file.getFD().sync();
          success = true;
        } finally {
          if (file != null)
            file.close();
        }
      } catch (IOException ioe) {
        if (exc == null)
          exc = ioe;
        try {
          // Pause 5 msec
          Thread.sleep(5);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
    if (!success)
      // Throw original exception
      throw exc;
  }
  
  /**
   * Counts the size of a directory recursively (sum of the length of all files).
   * 
   * @param directory
   *            directory to inspect, must not be {@code null}
   * @return size of directory in bytes, 0 if directory is security restricted, a negative number when the real total
   *         is greater than {@link Long#MAX_VALUE}.
   * @throws NullPointerException
   *             if the directory is {@code null}
   */
  public static long sizeOfDirectory(File directory) {
      checkDirectory(directory);

      final File[] files = directory.listFiles();
      if (files == null) {  // null if security restricted
          return 0L;
      }
      long size = 0;

      for (final File file : files) {
          try {
              if (!isSymlink(file)) {
                  size += sizeOf(file);
                  if (size < 0) {
                      break;
                  }
              }
          } catch (IOException ioe) {
              // Ignore exceptions caught when asking if a File is a symlink.
          }
      }

      return size;
  }
  
  /**
   * Returns the size of the specified file or directory. If the provided 
   * {@link File} is a regular file, then the file's length is returned.
   * If the argument is a directory, then the size of the directory is
   * calculated recursively. If a directory or subdirectory is security 
   * restricted, its size will not be included.
   * 
   * @param file the regular file or directory to return the size 
   *        of (must not be {@code null}).
   * 
   * @return the length of the file, or recursive size of the directory, 
   *         provided (in bytes).
   * 
   * @throws NullPointerException if the file is {@code null}
   * @throws IllegalArgumentException if the file does not exist.
   *         
   * @since 2.0
   */
  public static long sizeOf(File file) {

      if (!file.exists()) {
          String message = file + " does not exist";
          throw new IllegalArgumentException(message);
      }

      if (file.isDirectory()) {
          return sizeOfDirectory(file);
      } else {
          return file.length();
      }

  }
  
  /**
   * Checks that the given {@code File} exists and is a directory.
   * 
   * @param directory The {@code File} to check.
   * @throws IllegalArgumentException if the given {@code File} does not exist or is not a directory.
   */
  private static void checkDirectory(File directory) {
      if (!directory.exists()) {
          throw new IllegalArgumentException(directory + " does not exist");
      }
      if (!directory.isDirectory()) {
          throw new IllegalArgumentException(directory + " is not a directory");
      }
  }
  
  /**
   * Determines whether the specified file is a Symbolic Link rather than an actual file.
   * <p>
   * Will not return true if there is a Symbolic Link anywhere in the path,
   * only if the specific file is.
   * <p>
   * <b>Note:</b> the current implementation always returns {@code false} if the system
   * is detected as Windows using {@link FilenameUtils#isSystemWindows()}
   * 
   * @param file the file to check
   * @return true if the file is a Symbolic Link
   * @throws IOException if an IO error occurs while checking the file
   * @since 2.0
   */
  public static boolean isSymlink(File file) throws IOException {
      if (file == null) {
          throw new NullPointerException("File must not be null");
      }
      if (isSystemWindows()) {
          return false;
      }
      File fileInCanonicalDir = null;
      if (file.getParent() == null) {
          fileInCanonicalDir = file;
      } else {
          File canonicalDir = file.getParentFile().getCanonicalFile();
          fileInCanonicalDir = new File(canonicalDir, file.getName());
      }
      
      if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
          return false;
      } else {
          return true;
      }
  }
  
  //private static final char UNIX_SEPARATOR = '/';
  private static final char WINDOWS_SEPARATOR = '\\';
  private static final char SYSTEM_SEPARATOR = File.separatorChar;
  
  /**
   * Determines if Windows file system is in use.
   * 
   * @return true if the system is Windows
   */
  static boolean isSystemWindows() {
      return SYSTEM_SEPARATOR == WINDOWS_SEPARATOR;
  }
  
  //-----------------------------------------------------------------------
  /**
   * Writes a String to a file creating the file if it does not exist.
   *
   * NOTE: As from v1.3, the parent directories of the file will be created
   * if they do not exist.
   *
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param encoding  the encoding to use, {@code null} means platform default
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   * @since 2.4
   */
  public static void writeStringToFile(File file, String data, Charset encoding) 
		  throws IOException {
      writeStringToFile(file, data, encoding, false);
  }

  /**
   * Writes a String to a file creating the file if it does not exist.
   *
   * NOTE: As from v1.3, the parent directories of the file will be created
   * if they do not exist.
   *
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param encoding  the encoding to use, {@code null} means platform default
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   */
  public static void writeStringToFile(File file, String data, String encoding) 
		  throws IOException {
      writeStringToFile(file, data, encoding, false);
  }

  /**
   * Writes a String to a file creating the file if it does not exist.
   *
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param encoding  the encoding to use, {@code null} means platform default
   * @param append if {@code true}, then the String will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @since 2.3
   */
  public static void writeStringToFile(File file, String data, Charset encoding, 
		  boolean append) throws IOException {
      OutputStream out = null;
      try {
          out = openOutputStream(file, append);
          IOUtils.write(data, out, encoding);
          out.close(); // don't swallow close Exception if copy completes normally
      } finally {
          IOUtils.closeQuietly(out);
      }
  }

  /**
   * Writes a String to a file creating the file if it does not exist.
   *
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param encoding  the encoding to use, {@code null} means platform default
   * @param append if {@code true}, then the String will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @throws UnsupportedCharsetException
   *             thrown instead of {@link UnsupportedEncodingException} 
   *             in version 2.2 if the encoding is not
   *             supported by the VM
   * @since 2.1
   */
  public static void writeStringToFile(File file, String data, String encoding, 
		  boolean append) throws IOException {
      writeStringToFile(file, data, Charsets.toCharset(encoding), append);
  }

  /**
   * Writes a String to a file creating the file if it does not exist 
   * using the default encoding for the VM.
   * 
   * @param file  the file to write
   * @param data  the content to write to the file
   * @throws IOException in case of an I/O error
   */
  public static void writeStringToFile(File file, String data) throws IOException {
      writeStringToFile(file, data, Charset.defaultCharset(), false);
  }

  /**
   * Writes a String to a file creating the file if it does not exist 
   * using the default encoding for the VM.
   * 
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param append if {@code true}, then the String will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @since 2.1
   */
  public static void writeStringToFile(File file, String data, boolean append) throws IOException {
      writeStringToFile(file, data, Charset.defaultCharset(), append);
  }

  /**
   * Writes a CharSequence to a file creating the file if it does not exist 
   * using the default encoding for the VM.
   * 
   * @param file  the file to write
   * @param data  the content to write to the file
   * @throws IOException in case of an I/O error
   * @since 2.0
   */
  public static void write(File file, CharSequence data) throws IOException {
      write(file, data, Charset.defaultCharset(), false);
  }

  /**
   * Writes a CharSequence to a file creating the file if it does not exist 
   * using the default encoding for the VM.
   * 
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param append if {@code true}, then the data will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @since 2.1
   */
  public static void write(File file, CharSequence data, boolean append) throws IOException {
      write(file, data, Charset.defaultCharset(), append);
  }

  /**
   * Writes a CharSequence to a file creating the file if it does not exist.
   *
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param encoding  the encoding to use, {@code null} means platform default
   * @throws IOException in case of an I/O error
   * @since 2.3
   */
  public static void write(File file, CharSequence data, Charset encoding) throws IOException {
      write(file, data, encoding, false);
  }

  /**
   * Writes a CharSequence to a file creating the file if it does not exist.
   *
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param encoding  the encoding to use, {@code null} means platform default
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   * @since 2.0
   */
  public static void write(File file, CharSequence data, String encoding) throws IOException {
      write(file, data, encoding, false);
  }

  /**
   * Writes a CharSequence to a file creating the file if it does not exist.
   *
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param encoding  the encoding to use, {@code null} means platform default
   * @param append if {@code true}, then the data will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @since 2.3
   */
  public static void write(File file, CharSequence data, Charset encoding, 
		  boolean append) throws IOException {
      String str = data == null ? null : data.toString();
      writeStringToFile(file, str, encoding, append);
  }

  /**
   * Writes a CharSequence to a file creating the file if it does not exist.
   *
   * @param file  the file to write
   * @param data  the content to write to the file
   * @param encoding  the encoding to use, {@code null} means platform default
   * @param append if {@code true}, then the data will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @throws UnsupportedCharsetException
   *             thrown instead of {@link UnsupportedEncodingException} 
   *             in version 2.2 if the encoding is not
   *             supported by the VM
   * @since IO 2.1
   */
  public static void write(File file, CharSequence data, String encoding, 
		  boolean append) throws IOException {
      write(file, data, Charsets.toCharset(encoding), append);
  }

  /**
   * Writes a byte array to a file creating the file if it does not exist.
   * <p>
   * NOTE: As from v1.3, the parent directories of the file will be created
   * if they do not exist.
   *
   * @param file  the file to write to
   * @param data  the content to write to the file
   * @throws IOException in case of an I/O error
   * @since 1.1
   */
  public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
      writeByteArrayToFile(file, data, false);
  }

  /**
   * Writes a byte array to a file creating the file if it does not exist.
   *
   * @param file  the file to write to
   * @param data  the content to write to the file
   * @param append if {@code true}, then bytes will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @since IO 2.1
   */
  public static void writeByteArrayToFile(File file, byte[] data, 
		  boolean append) throws IOException {
      OutputStream out = null;
      try {
          out = openOutputStream(file, append);
          out.write(data);
          out.close(); // don't swallow close Exception if copy completes normally
      } finally {
          IOUtils.closeQuietly(out);
      }
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to
   * the specified <code>File</code> line by line.
   * The specified character encoding and the default line ending will be used.
   * <p>
   * NOTE: As from v1.3, the parent directories of the file will be created
   * if they do not exist.
   *
   * @param file  the file to write to
   * @param encoding  the encoding to use, {@code null} means platform default
   * @param lines  the lines to write, {@code null} entries produce blank lines
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   * @since 1.1
   */
  public static void writeLines(File file, String encoding, Collection<?> lines) 
		  throws IOException {
      writeLines(file, encoding, lines, null, false);
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to
   * the specified <code>File</code> line by line, optionally appending.
   * The specified character encoding and the default line ending will be used.
   *
   * @param file  the file to write to
   * @param encoding  the encoding to use, {@code null} means platform default
   * @param lines  the lines to write, {@code null} entries produce blank lines
   * @param append if {@code true}, then the lines will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   * @since 2.1
   */
  public static void writeLines(File file, String encoding, Collection<?> lines, 
		  boolean append) throws IOException {
      writeLines(file, encoding, lines, null, append);
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to
   * the specified <code>File</code> line by line.
   * The default VM encoding and the default line ending will be used.
   *
   * @param file  the file to write to
   * @param lines  the lines to write, {@code null} entries produce blank lines
   * @throws IOException in case of an I/O error
   * @since 1.3
   */
  public static void writeLines(File file, Collection<?> lines) throws IOException {
      writeLines(file, null, lines, null, false);
  }
  
  /**
   * Writes the <code>toString()</code> value of each item in a collection to
   * the specified <code>File</code> line by line.
   * The default VM encoding and the default line ending will be used.
   *
   * @param file  the file to write to
   * @param lines  the lines to write, {@code null} entries produce blank lines
   * @param append if {@code true}, then the lines will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @since 2.1
   */
  public static void writeLines(File file, Collection<?> lines, boolean append) 
		  throws IOException {
      writeLines(file, null, lines, null, append);
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to
   * the specified <code>File</code> line by line.
   * The specified character encoding and the line ending will be used.
   * <p>
   * NOTE: As from v1.3, the parent directories of the file will be created
   * if they do not exist.
   *
   * @param file  the file to write to
   * @param encoding  the encoding to use, {@code null} means platform default
   * @param lines  the lines to write, {@code null} entries produce blank lines
   * @param lineEnding  the line separator to use, {@code null} is system default
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   * @since 1.1
   */
  public static void writeLines(File file, String encoding, Collection<?> lines, 
		  String lineEnding) throws IOException {
      writeLines(file, encoding, lines, lineEnding, false);
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to
   * the specified <code>File</code> line by line.
   * The specified character encoding and the line ending will be used.
   *
   * @param file  the file to write to
   * @param encoding  the encoding to use, {@code null} means platform default
   * @param lines  the lines to write, {@code null} entries produce blank lines
   * @param lineEnding  the line separator to use, {@code null} is system default
   * @param append if {@code true}, then the lines will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   * @since 2.1
   */
  public static void writeLines(File file, String encoding, Collection<?> lines, 
		  String lineEnding, boolean append)
          throws IOException {
      FileOutputStream out = null;
      try {
          out = openOutputStream(file, append);
          final BufferedOutputStream buffer = new BufferedOutputStream(out);
          IOUtils.writeLines(lines, lineEnding, buffer, encoding);
          buffer.flush();
          out.close(); // don't swallow close Exception if copy completes normally
      } finally {
          IOUtils.closeQuietly(out);
      }
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to
   * the specified <code>File</code> line by line.
   * The default VM encoding and the specified line ending will be used.
   *
   * @param file  the file to write to
   * @param lines  the lines to write, {@code null} entries produce blank lines
   * @param lineEnding  the line separator to use, {@code null} is system default
   * @throws IOException in case of an I/O error
   * @since 1.3
   */
  public static void writeLines(File file, Collection<?> lines, String lineEnding) 
		  throws IOException {
      writeLines(file, null, lines, lineEnding, false);
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to
   * the specified <code>File</code> line by line.
   * The default VM encoding and the specified line ending will be used.
   *
   * @param file  the file to write to
   * @param lines  the lines to write, {@code null} entries produce blank lines
   * @param lineEnding  the line separator to use, {@code null} is system default
   * @param append if {@code true}, then the lines will be added to the
   * end of the file rather than overwriting
   * @throws IOException in case of an I/O error
   * @since 2.1
   */
  public static void writeLines(File file, Collection<?> lines, String lineEnding, 
		  boolean append) throws IOException {
      writeLines(file, null, lines, lineEnding, append);
  }
  
  //-----------------------------------------------------------------------
  /**
   * Opens a {@link FileOutputStream} for the specified file, checking and
   * creating the parent directory if it does not exist.
   * <p>
   * At the end of the method either the stream will be successfully opened,
   * or an exception will have been thrown.
   * <p>
   * The parent directory will be created if it does not exist.
   * The file will be created if it does not exist.
   * An exception is thrown if the file object exists but is a directory.
   * An exception is thrown if the file exists but cannot be written to.
   * An exception is thrown if the parent directory cannot be created.
   * 
   * @param file  the file to open for output, must not be {@code null}
   * @return a new {@link FileOutputStream} for the specified file
   * @throws IOException if the file object is a directory
   * @throws IOException if the file cannot be written to
   * @throws IOException if a parent directory needs creating but that fails
   * @since 1.3
   */
  public static FileOutputStream openOutputStream(File file) throws IOException {
      return openOutputStream(file, false);
  }

  /**
   * Opens a {@link FileOutputStream} for the specified file, checking and
   * creating the parent directory if it does not exist.
   * <p>
   * At the end of the method either the stream will be successfully opened,
   * or an exception will have been thrown.
   * <p>
   * The parent directory will be created if it does not exist.
   * The file will be created if it does not exist.
   * An exception is thrown if the file object exists but is a directory.
   * An exception is thrown if the file exists but cannot be written to.
   * An exception is thrown if the parent directory cannot be created.
   * 
   * @param file  the file to open for output, must not be {@code null}
   * @param append if {@code true}, then bytes will be added to the
   * end of the file rather than overwriting
   * @return a new {@link FileOutputStream} for the specified file
   * @throws IOException if the file object is a directory
   * @throws IOException if the file cannot be written to
   * @throws IOException if a parent directory needs creating but that fails
   * @since 2.1
   */
  public static FileOutputStream openOutputStream(File file, boolean append) 
		  throws IOException {
      if (file.exists()) {
          if (file.isDirectory()) {
              throw new IOException("File '" + file + "' exists but is a directory");
          }
          if (file.canWrite() == false) {
              throw new IOException("File '" + file + "' cannot be written to");
          }
      } else {
          File parent = file.getParentFile();
          if (parent != null) {
              if (!parent.mkdirs() && !parent.isDirectory()) {
                  throw new IOException("Directory '" + parent + "' could not be created");
              }
          }
      }
      return new FileOutputStream(file, append);
  }
  
}
