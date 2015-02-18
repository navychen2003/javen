package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.fs.FSDataInputStream;
import org.javenstudio.raptor.fs.FSDataOutputStream;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.Writable;

/**
 * A reference to the top or bottom half of a store file.  The file referenced
 * lives under a different region.  References are made at region split time.
 *
 * <p>References work with a special half store file type.  References know how
 * to write out the reference format in the file system and are whats juggled
 * when references are mixed in with direct store files.  The half store file
 * type is used reading the referred to file.
 *
 * <p>References to store files located over in some other region look like
 * this in the file system
 * <code>1278437856009925445.3323223323</code>:
 * i.e. an id followed by hash of the referenced region.
 * Note, a region is itself not splitable if it has instances of store file
 * references.  References are cleaned up by compactions.
 */
public class Reference implements Writable {

  private byte[] mSplitkey;
  private Range mRegion;

  /**
   * For split DBStoreFiles, it specifies if the file covers the lower half or
   * the upper half of the key range
   */
  public static enum Range {
    /** DBStoreFile contains upper half of key range */
    top,
    /** DBStoreFile contains lower half of key range */
    bottom
  }

  /**
   * Constructor
   * @param splitRow This is row we are splitting around.
   * @param fr
   */
  public Reference(final byte[] splitRow, final Range fr) {
    this.mSplitkey = splitRow == null ? null : 
    	KeyValue.createFirstOnRow(splitRow).getKey();
    this.mRegion = fr;
  }

  /**
   * Used by serializations.
   */
  public Reference() {
    this(null, Range.bottom);
  }

  /**
   * @return Range
   */
  public Range getFileRegion() {
    return this.mRegion;
  }

  /**
   * @return splitKey
   */
  public byte[] getSplitKey() {
    return this.mSplitkey;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "" + this.mRegion;
  }

  // Make it serializable.

  @Override
  public void write(DataOutput out) throws IOException {
    // Write true if we're doing top of the file.
    out.writeBoolean(isTopFileRegion(this.mRegion));
    Bytes.writeByteArray(out, this.mSplitkey);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    boolean tmp = in.readBoolean();
    // If true, set region to top.
    this.mRegion = tmp ? Range.top : Range.bottom;
    this.mSplitkey = Bytes.readByteArray(in);
  }

  public static boolean isTopFileRegion(final Range r) {
    return r.equals(Range.top);
  }

  public Path write(final FileSystem fs, final Path p)
      throws IOException {
    create(fs, p);
    FSDataOutputStream out = fs.create(p);
    try {
      write(out);
    } finally {
      out.close();
    }
    return p;
  }

  /**
   * Create file.
   * @param fs filesystem object
   * @param p path to create
   * @return Path
   * @throws IOException e
   */
  public static Path create(final FileSystem fs, final Path p)
      throws IOException {
    if (fs.exists(p)) 
      throw new IOException("File already exists " + p.toString());
    
    if (!fs.createNewFile(p)) 
      throw new IOException("Failed create of " + p);
    
    return p;
  }
  
  /**
   * Read a Reference from FileSystem.
   * @param fs
   * @param p
   * @return New Reference made from passed <code>p</code>
   * @throws IOException
   */
  public static Reference read(final FileSystem fs, final Path p)
      throws IOException {
    FSDataInputStream in = fs.open(p);
    try {
      Reference r = new Reference();
      r.readFields(in);
      return r;
    } finally {
      in.close();
    }
  }
}
