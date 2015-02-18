package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.DBColumnDescriptor;
import org.javenstudio.raptor.bigdb.io.dbfile.Compression;

/**
 * Immutable DBColumnDescriptor
 */
public class UnmodifyableDBColumnDescriptor extends DBColumnDescriptor {

  /**
   * @param desc wrapped
   */
  public UnmodifyableDBColumnDescriptor (final DBColumnDescriptor desc) {
    super(desc);
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBColumnDescriptor#setValue(byte[], byte[])
   */
  @Override
  public void setValue(byte[] key, byte[] value) {
    throw new UnsupportedOperationException("DBColumnDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBColumnDescriptor#setValue(java.lang.String, java.lang.String)
   */
  @Override
  public void setValue(String key, String value) {
    throw new UnsupportedOperationException("DBColumnDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBColumnDescriptor#setMaxVersions(int)
   */
  @Override
  public void setMaxVersions(int maxVersions) {
    throw new UnsupportedOperationException("DBColumnDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBColumnDescriptor#setInMemory(boolean)
   */
  @Override
  public void setInMemory(boolean inMemory) {
    throw new UnsupportedOperationException("DBColumnDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBColumnDescriptor#setBlockCacheEnabled(boolean)
   */
  @Override
  public void setBlockCacheEnabled(boolean blockCacheEnabled) {
    throw new UnsupportedOperationException("DBColumnDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBColumnDescriptor#setTimeToLive(int)
   */
  @Override
  public void setTimeToLive(int timeToLive) {
    throw new UnsupportedOperationException("DBColumnDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBColumnDescriptor#setCompressionType(org.javenstudio.raptor.bigdb.io.dbfile.Compression.Algorithm)
   */
  @Override
  public void setCompressionType(Compression.Algorithm type) {
    throw new UnsupportedOperationException("DBColumnDescriptor is read-only");
  }
}
