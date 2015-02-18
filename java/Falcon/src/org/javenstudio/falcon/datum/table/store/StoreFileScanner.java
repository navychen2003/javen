package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

/**
 * KeyValueScanner adaptor over the Reader.  It also provides hooks into
 * bloom filter things.
 */
class StoreFileScanner implements KeyValueScanner {
  //private static final Logger LOG = Logger.getLogger(StoreFileScanner.class);

  // the reader it comes from:
  private final StoreFile.Reader mReader;
  private final DBFileScanner mHfs;
  private KeyValue mCur = null;

  /**
   * Implements a {@link KeyValueScanner} on top of the specified {@link DBFileScanner}
   * @param hfs DBFile scanner
   */
  public StoreFileScanner(StoreFile.Reader reader, DBFileScanner hfs) {
    this.mReader = reader;
    this.mHfs = hfs;
  }

  /**
   * Return an array of scanners corresponding to the given
   * set of store files.
   */
  public static List<StoreFileScanner> getScannersForStoreFiles(
      Collection<StoreFile> filesToCompact,
      boolean cacheBlocks,
      boolean usePread) throws IOException {
    List<StoreFileScanner> scanners =
      new ArrayList<StoreFileScanner>(filesToCompact.size());
    for (StoreFile file : filesToCompact) {
      StoreFile.Reader r = file.createReader();
      scanners.add(r.getStoreFileScanner(cacheBlocks, usePread));
    }
    return scanners;
  }

  @Override
  public String toString() {
    return "StoreFileScanner[" + mHfs.toString() + ", cur=" + mCur + "]";
  }

  @Override
  public KeyValue peek() {
    return mCur;
  }

  @Override
  public KeyValue next() throws IOException {
    KeyValue retKey = mCur;
    mCur = mHfs.getKeyValue();
    try {
      // only seek if we arent at the end. cur == null implies 'end'.
      if (mCur != null)
        mHfs.next();
    } catch(IOException e) {
      throw new IOException("Could not iterate " + this, e);
    }
    return retKey;
  }

  @Override
  public boolean seek(KeyValue key) throws IOException {
    try {
      if(!seekAtOrAfter(mHfs, key)) {
        close();
        return false;
      }
      mCur = mHfs.getKeyValue();
      mHfs.next();
      return true;
    } catch(IOException ioe) {
      throw new IOException("Could not seek " + this, ioe);
    }
  }

  @Override
  public boolean reseek(KeyValue key) throws IOException {
    try {
      if (!reseekAtOrAfter(mHfs, key)) {
        close();
        return false;
      }
      mCur = mHfs.getKeyValue();
      mHfs.next();
      return true;
    } catch (IOException ioe) {
      throw new IOException("Could not seek " + this, ioe);
    }
  }

  @Override
  public void close() {
    // Nothing to close on DBFileScanner?
    mCur = null;
  }

  /**
   *
   * @param s
   * @param k
   * @return
   * @throws IOException
   */
  public static boolean seekAtOrAfter(DBFileScanner s, KeyValue k)
      throws IOException {
    int result = s.seekTo(k.getBuffer(), k.getKeyOffset(), k.getKeyLength());
    if(result < 0) {
      // Passed KV is smaller than first KV in file, work from start of file
      return s.seekTo();
    } else if(result > 0) {
      // Passed KV is larger than current KV in file, if there is a next
      // it is the "after", if not then this scanner is done.
      return s.next();
    }
    // Seeked to the exact key
    return true;
  }

  static boolean reseekAtOrAfter(DBFileScanner s, KeyValue k)
      throws IOException {
    //This function is similar to seekAtOrAfter function
    int result = s.reseekTo(k.getBuffer(), k.getKeyOffset(), k.getKeyLength());
    if (result <= 0) {
      return true;
    } else {
      // passed KV is larger than current KV in file, if there is a next
      // it is after, if not then this scanner is done.
      return s.next();
    }
  }

  // StoreFile filter hook.
  public boolean shouldSeek(Scan scan, final SortedSet<byte[]> columns) {
    return mReader.shouldSeek(scan, columns);
  }
}
