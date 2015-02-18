package org.javenstudio.raptor.bigdb.regionserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.client.Scan;
import org.javenstudio.raptor.bigdb.io.dbfile.DBFileScanner;

/**
 * KeyValueScanner adaptor over the Reader.  It also provides hooks into
 * bloom filter things.
 */
class StoreFileScanner implements KeyValueScanner {
  static final Logger LOG = Logger.getLogger(Store.class);

  // the reader it comes from:
  private final StoreFile.Reader reader;
  private final DBFileScanner hfs;
  private KeyValue cur = null;

  /**
   * Implements a {@link KeyValueScanner} on top of the specified {@link DBFileScanner}
   * @param hfs DBFile scanner
   */
  public StoreFileScanner(StoreFile.Reader reader, DBFileScanner hfs) {
    this.reader = reader;
    this.hfs = hfs;
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

  public String toString() {
    return "StoreFileScanner[" + hfs.toString() + ", cur=" + cur + "]";
  }

  public KeyValue peek() {
    return cur;
  }

  public KeyValue next() throws IOException {
    KeyValue retKey = cur;
    cur = hfs.getKeyValue();
    try {
      // only seek if we arent at the end. cur == null implies 'end'.
      if (cur != null)
        hfs.next();
    } catch(IOException e) {
      throw new IOException("Could not iterate " + this, e);
    }
    return retKey;
  }

  public boolean seek(KeyValue key) throws IOException {
    try {
      if(!seekAtOrAfter(hfs, key)) {
        close();
        return false;
      }
      cur = hfs.getKeyValue();
      hfs.next();
      return true;
    } catch(IOException ioe) {
      throw new IOException("Could not seek " + this, ioe);
    }
  }

  public boolean reseek(KeyValue key) throws IOException {
    try {
      if (!reseekAtOrAfter(hfs, key)) {
        close();
        return false;
      }
      cur = hfs.getKeyValue();
      hfs.next();
      return true;
    } catch (IOException ioe) {
      throw new IOException("Could not seek " + this, ioe);
    }
  }

  public void close() {
    // Nothing to close on DBFileScanner?
    cur = null;
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
    return reader.shouldSeek(scan, columns);
  }
}

