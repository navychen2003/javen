package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

/**
 * A facade for a {@link DBFile.Reader} that serves up
 * either the top or bottom half of a DBFile where 'bottom' is the first half
 * of the file containing the keys that sort lowest and 'top' is the second half
 * of the file with keys that sort greater than those of the bottom half.
 * The top includes the split files midkey, of the key that follows if it does
 * not exist in the file.
 *
 * <p>This type works in tandem with the {@link Reference} type.  This class
 * is used reading while Reference is used writing.
 *
 * <p>This file is not splitable.  Calls to {@link #midkey()} return null.
 */
public class HalfStoreFileReader extends StoreFile.Reader {
  private static final Logger LOG = Logger.getLogger(HalfStoreFileReader.class);
  
  // This is the key we split around.  Its the first possible entry on a row:
  // i.e. empty column and a timestamp of LATEST_TIMESTAMP.
  private final byte[] mSplitkey;
  private final boolean mTop;

  /**
   * @param fs
   * @param p
   * @param c
   * @param r
   * @throws IOException
   */
  public HalfStoreFileReader(final FileSystem fs, final Path p, 
	  final BlockCache c, final Reference r) throws IOException {
    super(fs, p, c, false);
    // This is not actual midkey for this half-file; its just border
    // around which we split top and bottom.  Have to look in files to find
    // actual last and first keys for bottom and top halves.  Half-files don't
    // have an actual midkey themselves. No midkey is how we indicate file is
    // not splittable.
    this.mSplitkey = r.getSplitKey();
    // Is it top or bottom half?
    this.mTop = Reference.isTopFileRegion(r.getFileRegion());
  }

  protected boolean isTop() {
    return this.mTop;
  }

  @SuppressWarnings("deprecation")
  @Override
  public DBFileScanner getScanner(final boolean cacheBlocks, final boolean pread) {
    final DBFileScanner s = super.getScanner(cacheBlocks, pread);
    return new DBFileScanner() {
      private final DBFileScanner mDelegate = s;
      private boolean mAtEnd = false;

      @Override
      public ByteBuffer getKey() {
        if (mAtEnd) return null;
        return mDelegate.getKey();
      }

      @Override
      public String getKeyString() {
        if (mAtEnd) return null;
        return mDelegate.getKeyString();
      }

      @Override
      public ByteBuffer getValue() {
        if (mAtEnd) return null;
        return mDelegate.getValue();
      }

      @Override
      public String getValueString() {
        if (mAtEnd) return null;
        return mDelegate.getValueString();
      }

      @Override
      public KeyValue getKeyValue() {
        if (mAtEnd) return null;
        return mDelegate.getKeyValue();
      }

      @Override
      public boolean next() throws IOException {
        if (mAtEnd) return false;
        boolean b = mDelegate.next();
        if (!b) return b;
        
        // constrain the bottom.
        if (!mTop) {
          ByteBuffer bb = getKey();
          if (getComparator().compare(bb.array(), bb.arrayOffset(), bb.limit(),
              mSplitkey, 0, mSplitkey.length) >= 0) {
            mAtEnd = true;
            return false;
          }
        }
        return true;
      }

      @Override
      public boolean seekBefore(byte[] key) throws IOException {
        return seekBefore(key, 0, key.length);
      }

      @Override
      public boolean seekBefore(byte[] key, int offset, int length)
          throws IOException {
        if (mTop) {
          if (getComparator().compare(key, offset, length, mSplitkey, 0,
              mSplitkey.length) < 0) {
            return false;
          }
        } else {
          if (getComparator().compare(key, offset, length, mSplitkey, 0,
              mSplitkey.length) >= 0) {
            return seekBefore(mSplitkey, 0, mSplitkey.length);
          }
        }
        return this.mDelegate.seekBefore(key, offset, length);
      }

      @Override
      public boolean seekTo() throws IOException {
        if (mTop) {
          int r = this.mDelegate.seekTo(mSplitkey);
          if (r < 0) {
            // midkey is < first key in file
            return this.mDelegate.seekTo();
          }
          if (r > 0) {
            return this.mDelegate.next();
          }
          return true;
        }

        boolean b = mDelegate.seekTo();
        if (!b) return b;
        
        // Check key.
        ByteBuffer k = this.mDelegate.getKey();
        return this.mDelegate.getReader().getComparator().
          compare(k.array(), k.arrayOffset(), k.limit(),
            mSplitkey, 0, mSplitkey.length) < 0;
      }

      @Override
      public int seekTo(byte[] key) throws IOException {
        return seekTo(key, 0, key.length);
      }

      @Override
      public int seekTo(byte[] key, int offset, int length) throws IOException {
        if (mTop) {
          if (getComparator().compare(key, offset, length, mSplitkey, 0,
              mSplitkey.length) < 0) {
            return -1;
          }
        } else {
          if (getComparator().compare(key, offset, length, mSplitkey, 0,
              mSplitkey.length) >= 0) {
            // we would place the scanner in the second half.
            // it might be an error to return false here ever...
            boolean res = mDelegate.seekBefore(mSplitkey, 0, mSplitkey.length);
            if (!res) {
              throw new IOException("Seeking for a key in bottom of file, " 
            		  + "but key exists in top of file, failed on seekBefore(midkey)");
            }
            return 1;
          }
        }
        return mDelegate.seekTo(key, offset, length);
      }

      @Override
      public int reseekTo(byte[] key) throws IOException {
        return reseekTo(key, 0, key.length);
      }

      @Override
      public int reseekTo(byte[] key, int offset, int length)
          throws IOException {
        //This function is identical to the corresponding seekTo function except
        //that we call reseekTo (and not seekTo) on the delegate.
        if (mTop) {
          if (getComparator().compare(key, offset, length, mSplitkey, 0,
              mSplitkey.length) < 0) {
            return -1;
          }
        } else {
          if (getComparator().compare(key, offset, length, mSplitkey, 0,
              mSplitkey.length) >= 0) {
            // we would place the scanner in the second half.
            // it might be an error to return false here ever...
            boolean res = mDelegate.seekBefore(mSplitkey, 0, mSplitkey.length);
            if (!res) {
              throw new IOException("Seeking for a key in bottom of file, but" +
                  " key exists in top of file, failed on seekBefore(midkey)");
            }
            return 1;
          }
        }
        return mDelegate.reseekTo(key, offset, length);
      }

      @Override
      public DBFile.Reader getReader() {
        return this.mDelegate.getReader();
      }

      @Override
      public boolean isSeeked() {
        return this.mDelegate.isSeeked();
      }
    };
  }

  @Override
  public byte[] getLastKey() {
    if (mTop) 
      return super.getLastKey();
    
    // Get a scanner that caches the block and that uses pread.
    DBFileScanner scanner = getScanner(true, true);
    try {
      if (scanner.seekBefore(this.mSplitkey)) 
        return Bytes.toBytes(scanner.getKey());
      
    } catch (IOException e) {
      if (LOG.isWarnEnabled())
        LOG.warn("Failed seekBefore " + Bytes.toString(this.mSplitkey), e);
    }
    
    return null;
  }

  @Override
  public byte[] midkey() throws IOException {
    // Returns null to indicate file is not splitable.
    return null;
  }
}
