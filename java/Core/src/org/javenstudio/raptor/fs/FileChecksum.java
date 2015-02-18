package org.javenstudio.raptor.fs;

import java.util.Arrays;

import org.javenstudio.raptor.io.Writable;

/** An abstract class representing file checksums for files. */
public abstract class FileChecksum implements Writable {
  /** The checksum algorithm name */ 
  public abstract String getAlgorithmName();

  /** The length of the checksum in bytes */ 
  public abstract int getLength();

  /** The value of the checksum in bytes */ 
  public abstract byte[] getBytes();

  /** Return true if both the algorithms and the values are the same. */
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other == null || !(other instanceof FileChecksum)) {
      return false;
    }

    final FileChecksum that = (FileChecksum)other;
    return this.getAlgorithmName().equals(that.getAlgorithmName())
      && Arrays.equals(this.getBytes(), that.getBytes());
  }
  
  /** {@inheritDoc} */
  public int hashCode() {
    return getAlgorithmName().hashCode() ^ Arrays.hashCode(getBytes());
  }
}
