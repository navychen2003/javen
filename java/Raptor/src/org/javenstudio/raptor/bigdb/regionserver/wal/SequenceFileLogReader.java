package org.javenstudio.raptor.bigdb.regionserver.wal;

import java.io.IOException;
import java.lang.reflect.Field;
 
import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FSDataInputStream;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.io.SequenceFile;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLog;
import org.javenstudio.raptor.bigdb.regionserver.wal.WALEdit;

public class SequenceFileLogReader implements DBLog.Reader {
  private static final Logger LOG = Logger.getLogger(SequenceFileLogReader.class);

  /**
   * Hack just to set the correct file length up in SequenceFile.Reader.
   * See HADOOP-6307.  The below is all about setting the right length on the
   * file we are reading.  fs.getFileStatus(file).getLen() is passed down to
   * a private SequenceFile.Reader constructor.  This won't work.  Need to do
   * the available on the stream.  The below is ugly.  It makes getPos, the
   * first time its called, return length of the file -- i.e. tell a lie -- just
   * so this line up in SF.Reader's constructor ends up with right answer:
   *
   *         this.end = in.getPos() + length;
   *
   */
  private static class WALReader extends SequenceFile.Reader {

    WALReader(final FileSystem fs, final Path p, final Configuration c)
    throws IOException {
      super(fs, p, c);

    }

    @Override
    protected FSDataInputStream openFile(FileSystem fs, Path file,
      int bufferSize, long length)
    throws IOException {
      return new WALReaderFSDataInputStream(super.openFile(fs, file,
        bufferSize, length), length);
    }

    /**
     * Override just so can intercept first call to getPos.
     */
    static class WALReaderFSDataInputStream extends FSDataInputStream {
      private boolean firstGetPosInvocation = true;
      private long length;

      WALReaderFSDataInputStream(final FSDataInputStream is, final long l)
      throws IOException {
        super(is);
        this.length = l;
      }

      @Override
      public long getPos() throws IOException {
        if (this.firstGetPosInvocation) {
          this.firstGetPosInvocation = false;
          // Tell a lie.  We're doing this just so that this line up in
          // SequenceFile.Reader constructor comes out with the correct length
          // on the file:
          //         this.end = in.getPos() + length;
          long available = this.in.available();
          // Length gets added up in the SF.Reader constructor so subtract the
          // difference.  If available < this.length, then return this.length.
          return available >= this.length? available - this.length: this.length;
        }
        return super.getPos();
      }
    }
  }

  Configuration conf;
  WALReader reader;
  // Needed logging exceptions
  Path path;
  int edit = 0;
  long entryStart = 0;

  public SequenceFileLogReader() { }

  @Override
  public void init(FileSystem fs, Path path, Configuration conf)
      throws IOException {
    this.conf = conf;
    this.path = path;
    reader = new WALReader(fs, path, conf);
  }

  @Override
  public void close() throws IOException {
    try {
      reader.close();
    } catch (IOException ioe) {
      throw addFileInfoToException(ioe);
    }
  }

  @Override
  public DBLog.Entry next() throws IOException {
    return next(null);
  }

  @Override
  public DBLog.Entry next(DBLog.Entry reuse) throws IOException {
    this.entryStart = this.reader.getPosition();
    DBLog.Entry e = reuse;
    if (e == null) {
      DBLogKey key = DBLog.newKey(conf);
      WALEdit val = new WALEdit();
      e = new DBLog.Entry(key, val);
    }
    boolean b = false;
    try {
      b = this.reader.next(e.getKey(), e.getEdit());
    } catch (IOException ioe) {
      throw addFileInfoToException(ioe);
    }
    edit++;
    return b? e: null;
  }

  @Override
  public void seek(long pos) throws IOException {
    try {
      reader.seek(pos);
    } catch (IOException ioe) {
      throw addFileInfoToException(ioe);
    }
  }

  @Override
  public long getPosition() throws IOException {
    return reader.getPosition();
  }

  private IOException addFileInfoToException(final IOException ioe)
  throws IOException {
    long pos = -1;
    try {
      pos = getPosition();
    } catch (IOException e) {
      LOG.warn("Failed getting position to add to throw", e);
    }

    // See what SequenceFile.Reader thinks is the end of the file
    long end = Long.MAX_VALUE;
    try {
      Field fEnd = SequenceFile.Reader.class.getDeclaredField("end");
      fEnd.setAccessible(true);
      end = fEnd.getLong(this.reader);
    } catch(Exception e) { /* reflection fail. keep going */ }

    String msg = (this.path == null? "": this.path.toString()) +
      ", entryStart=" + entryStart + ", pos=" + pos + 
      ((end == Long.MAX_VALUE) ? "" : ", end=" + end) + 
      ", edit=" + this.edit;

    // Enhance via reflection so we don't change the original class type
    try {
      return (IOException) ioe.getClass()
        .getConstructor(String.class)
        .newInstance(msg)
        .initCause(ioe);
    } catch(Exception e) { /* reflection fail. keep going */ }
    
    return ioe;
  }
}

