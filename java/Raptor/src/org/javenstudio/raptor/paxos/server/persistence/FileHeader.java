package org.javenstudio.raptor.paxos.server.persistence;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.WritableComparable;


public class FileHeader implements WritableComparable<FileHeader> {

  private int magic = 0;
  private int version = 0;
  private long dbid = 0;

  private FileHeader() {} 

  public FileHeader(int magic, int version, long dbid) {
    this.magic = magic; 
    this.version = version; 
    this.dbid = dbid; 
  }

  public int getMagic() { return magic; }
  public int getVersion() { return version; }
  public long getDbid() { return dbid; }

  public boolean equals(Object to) {
    if (this == to) 
      return true;
    if (!(to instanceof FileHeader)) 
      return false;
    FileHeader other = (FileHeader)to; 
    return magic == other.magic &&
           version == other.version && 
           dbid == other.dbid; 
  }

  public int compareTo(FileHeader that) {
    if (that == null) return 1; 
    int ret = 0;
    ret = (magic == that.magic)? 0 :((magic<that.magic)?-1:1);
    if (ret != 0) return ret;
    ret = (version == that.version)? 0 :((version<that.version)?-1:1);
    if (ret != 0) return ret;
    ret = (dbid == that.dbid)? 0 :((dbid<that.dbid)?-1:1);
    if (ret != 0) return ret;
    return ret;
  }

  public int hashCode() {
    int result = 17;
    int ret = 0;
    ret = (int)magic;
    result = 37*result + ret;
    ret = (int)version;
    result = 37*result + ret;
    ret = (int) (dbid^(dbid>>>32));
    result = 37*result + ret;
    return result;
  }

  /////////////////////////////////////////////////
  // Writable
  /////////////////////////////////////////////////
  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeInt(magic); 
    out.writeInt(version); 
    out.writeLong(dbid); 
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    magic = in.readInt(); 
    version = in.readInt(); 
    dbid = in.readLong(); 
  }

  public static FileHeader read(DataInput in) throws IOException {
    FileHeader result = new FileHeader();
    result.readFields(in);
    return result;
  }
}
