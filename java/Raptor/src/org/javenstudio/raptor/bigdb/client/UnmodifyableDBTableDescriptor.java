package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.DBColumnDescriptor;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;

/**
 * Read-only table descriptor.
 */
public class UnmodifyableDBTableDescriptor extends DBTableDescriptor {
  /** Default constructor */
  public UnmodifyableDBTableDescriptor() {
	  super();
  }

  /*
   * Create an unmodifyable copy of an DBTableDescriptor
   * @param desc
   */
  UnmodifyableDBTableDescriptor(final DBTableDescriptor desc) {
    super(desc.getName(), getUnmodifyableFamilies(desc), desc.getValues());
  }


  /*
   * @param desc
   * @return Families as unmodifiable array.
   */
  private static DBColumnDescriptor[] getUnmodifyableFamilies(
      final DBTableDescriptor desc) {
    DBColumnDescriptor [] f = new DBColumnDescriptor[desc.getFamilies().size()];
    int i = 0;
    for (DBColumnDescriptor c: desc.getFamilies()) {
      f[i++] = c;
    }
    return f;
  }

  /**
   * Does NOT add a column family. This object is immutable
   * @param family DBColumnDescriptor of familyto add.
   */
  @Override
  public void addFamily(final DBColumnDescriptor family) {
    throw new UnsupportedOperationException("DBTableDescriptor is read-only");
  }

  /**
   * @param column
   * @return Column descriptor for the passed family name or the family on
   * passed in column.
   */
  @Override
  public DBColumnDescriptor removeFamily(final byte [] column) {
    throw new UnsupportedOperationException("DBTableDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBTableDescriptor#setReadOnly(boolean)
   */
  @Override
  public void setReadOnly(boolean readOnly) {
    throw new UnsupportedOperationException("DBTableDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBTableDescriptor#setValue(byte[], byte[])
   */
  @Override
  public void setValue(byte[] key, byte[] value) {
    throw new UnsupportedOperationException("DBTableDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBTableDescriptor#setValue(java.lang.String, java.lang.String)
   */
  @Override
  public void setValue(String key, String value) {
    throw new UnsupportedOperationException("DBTableDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBTableDescriptor#setMaxFileSize(long)
   */
  @Override
  public void setMaxFileSize(long maxFileSize) {
    throw new UnsupportedOperationException("DBTableDescriptor is read-only");
  }

  /**
   * @see org.javenstudio.raptor.bigdb.DBTableDescriptor#setMemStoreFlushSize(long)
   */
  @Override
  public void setMemStoreFlushSize(long memstoreFlushSize) {
    throw new UnsupportedOperationException("DBTableDescriptor is read-only");
  }

//  /**
//   * @see org.javenstudio.raptor.bigdb.DBTableDescriptor#addIndex(org.javenstudio.raptor.bigdb.client.tableindexed.IndexSpecification)
//   */
//  @Override
//  public void addIndex(IndexSpecification index) {
//    throw new UnsupportedOperationException("DBTableDescriptor is read-only");
//  }
}

