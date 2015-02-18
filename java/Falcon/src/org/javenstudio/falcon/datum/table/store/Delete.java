package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.raptor.io.Writable;

/**
 * Used to perform Delete operations on a single row.
 * <p>
 * To delete an entire row, instantiate a Delete object with the row
 * to delete.  To further define the scope of what to delete, perform
 * additional methods as outlined below.
 * <p>
 * To delete specific families, execute {@link #deleteFamily(byte[]) deleteFamily}
 * for each family to delete.
 * <p>
 * To delete multiple versions of specific columns, execute
 * {@link #deleteColumns(byte[], byte[]) deleteColumns}
 * for each column to delete.
 * <p>
 * To delete specific versions of specific columns, execute
 * {@link #deleteColumn(byte[], byte[], long) deleteColumn}
 * for each column version to delete.
 * <p>
 * Specifying timestamps, deleteFamily and deleteColumns will delete all
 * versions with a timestamp less than or equal to that passed.  If no
 * timestamp is specified, an entry is added with a timestamp of 'now'
 * where 'now' is the servers's System.currentTimeMillis().
 * Specifying a timestamp to the deleteColumn method will
 * delete versions only with a timestamp equal to that specified.
 * If no timestamp is passed to deleteColumn, internally, it figures the
 * most recent cell's timestamp and adds a delete at that timestamp; i.e.
 * it deletes the most recently added cell.
 * <p>The timestamp passed to the constructor is used ONLY for delete of
 * rows.  For anything less -- a deleteColumn, deleteColumns or
 * deleteFamily -- then you need to use the method overrides that take a
 * timestamp.  The constructor timestamp is not referenced.
 */
public class Delete implements Writable, Row, Comparable<Row> {
  private static final byte DELETE_VERSION = (byte)1;
  
  private final Map<byte[], List<KeyValue>> mFamilyMap =
    new TreeMap<byte[], List<KeyValue>>(Bytes.BYTES_COMPARATOR);
  
  private byte[] mRow = null;
  // This ts is only used when doing a deleteRow.  Anything less,
  private long mTs;
  private long mLockId = -1L;

  /** Constructor for Writable.  DO NOT USE */
  public Delete() {
    this((byte[])null);
  }

  /**
   * Create a Delete operation for the specified row.
   * <p>
   * If no further operations are done, this will delete everything
   * associated with the specified row (all versions of all columns in all
   * families).
   * @param row row key
   */
  public Delete(byte[] row) {
    this(row, DBConstants.LATEST_TIMESTAMP, null);
  }

  /**
   * Create a Delete operation for the specified row and timestamp, using
   * an optional row lock.<p>
   *
   * If no further operations are done, this will delete all columns in all
   * families of the specified row with a timestamp less than or equal to the
   * specified timestamp.<p>
   *
   * This timestamp is ONLY used for a delete row operation.  If specifying
   * families or columns, you must specify each timestamp individually.
   * @param row row key
   * @param timestamp maximum version timestamp (only for delete row)
   * @param rowLock previously acquired row lock, or null
   */
  public Delete(byte[] row, long timestamp, RowLock rowLock) {
    this.mRow = row;
    this.mTs = timestamp;
    if (rowLock != null) {
    	this.mLockId = rowLock.getLockId();
    }
  }

  /**
   * @param d Delete to clone.
   */
  public Delete(final Delete d) {
    this.mRow = d.getRow();
    this.mTs = d.getTimeStamp();
    this.mLockId = d.getLockId();
    this.mFamilyMap.putAll(d.getFamilyMap());
  }

  @Override
  public int compareTo(final Row d) {
    return Bytes.compareTo(this.getRow(), d.getRow());
  }

  /**
   * Method to check if the familyMap is empty
   * @return true if empty, false otherwise
   */
  public boolean isEmpty() {
    return mFamilyMap.isEmpty();
  }

  /**
   * Delete all versions of all columns of the specified family.
   * <p>
   * Overrides previous calls to deleteColumn and deleteColumns for the
   * specified family.
   * @param family family name
   * @return this for invocation chaining
   */
  public Delete deleteFamily(byte[] family) {
    this.deleteFamily(family, DBConstants.LATEST_TIMESTAMP);
    return this;
  }

  /**
   * Delete all columns of the specified family with a timestamp less than
   * or equal to the specified timestamp.
   * <p>
   * Overrides previous calls to deleteColumn and deleteColumns for the
   * specified family.
   * @param family family name
   * @param timestamp maximum version timestamp
   * @return this for invocation chaining
   */
  public Delete deleteFamily(byte[] family, long timestamp) {
    List<KeyValue> list = mFamilyMap.get(family);
    if (list == null) {
      list = new ArrayList<KeyValue>();
    } else if(!list.isEmpty()) {
      list.clear();
    }
    list.add(new KeyValue(mRow, family, null, timestamp, KeyValue.Type.DeleteFamily));
    mFamilyMap.put(family, list);
    return this;
  }

  /**
   * Delete all versions of the specified column.
   * @param family family name
   * @param qualifier column qualifier
   * @return this for invocation chaining
   */
  public Delete deleteColumns(byte[] family, byte[] qualifier) {
    this.deleteColumns(family, qualifier, DBConstants.LATEST_TIMESTAMP);
    return this;
  }

  /**
   * Delete all versions of the specified column with a timestamp less than
   * or equal to the specified timestamp.
   * @param family family name
   * @param qualifier column qualifier
   * @param timestamp maximum version timestamp
   * @return this for invocation chaining
   */
  public Delete deleteColumns(byte[] family, byte[] qualifier, long timestamp) {
    List<KeyValue> list = mFamilyMap.get(family);
    if (list == null) {
      list = new ArrayList<KeyValue>();
    }
    list.add(new KeyValue(this.mRow, family, qualifier, timestamp,
      KeyValue.Type.DeleteColumn));
    mFamilyMap.put(family, list);
    return this;
  }

  /**
   * Delete the latest version of the specified column.
   * This is an expensive call in that on the server-side, it first does a
   * get to find the latest versions timestamp.  Then it adds a delete using
   * the fetched cells timestamp.
   * @param family family name
   * @param qualifier column qualifier
   * @return this for invocation chaining
   */
  public Delete deleteColumn(byte[] family, byte[] qualifier) {
    this.deleteColumn(family, qualifier, DBConstants.LATEST_TIMESTAMP);
    return this;
  }

  /**
   * Delete the specified version of the specified column.
   * @param family family name
   * @param qualifier column qualifier
   * @param timestamp version timestamp
   * @return this for invocation chaining
   */
  public Delete deleteColumn(byte[] family, byte[] qualifier, long timestamp) {
    List<KeyValue> list = mFamilyMap.get(family);
    if (list == null) {
      list = new ArrayList<KeyValue>();
    }
    list.add(new KeyValue(this.mRow, family, qualifier, timestamp, 
      KeyValue.Type.Delete));
    mFamilyMap.put(family, list);
    return this;
  }

  /**
   * Method for retrieving the delete's familyMap
   * @return familyMap
   */
  public Map<byte[], List<KeyValue>> getFamilyMap() {
    return this.mFamilyMap;
  }

  /**
   *  Method for retrieving the delete's row
   * @return row
   */
  public byte[] getRow() {
    return this.mRow;
  }

  /**
   * Method for retrieving the delete's RowLock
   * @return RowLock
   */
  public RowLock getRowLock() {
    return new RowLock(this.mRow, this.mLockId);
  }

  /**
   * Method for retrieving the delete's lock ID.
   *
   * @return The lock ID.
   */
  public long getLockId() {
	return this.mLockId;
  }

  /**
   * Method for retrieving the delete's timestamp
   * @return timestamp
   */
  public long getTimeStamp() {
    return this.mTs;
  }

  /**
   * @return string
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("row=");
    sb.append(Bytes.toString(this.mRow));
    sb.append(", ts=");
    sb.append(this.mTs);
    sb.append(", families={");
    
    boolean moreThanOne = false;
    for (Map.Entry<byte[], List<KeyValue>> entry : this.mFamilyMap.entrySet()) {
      if (moreThanOne) {
        sb.append(", ");
      } else {
        moreThanOne = true;
      }
      sb.append("(family=");
      sb.append(Bytes.toString(entry.getKey()));
      sb.append(", keyvalues=(");
      
      boolean moreThanOneB = false;
      for (KeyValue kv : entry.getValue()) {
        if(moreThanOneB) {
          sb.append(", ");
        } else {
          moreThanOneB = true;
        }
        sb.append(kv.toString());
      }
      sb.append(")");
    }
    
    sb.append("}");
    return sb.toString();
  }

  //Writable
  @Override
  public void readFields(final DataInput in) throws IOException {
    int version = in.readByte();
    if (version > DELETE_VERSION) 
      throw new IOException("version not supported");
    
    this.mRow = Bytes.readByteArray(in);
    this.mTs = in.readLong();
    this.mLockId = in.readLong();
    this.mFamilyMap.clear();
    
    int numFamilies = in.readInt();
    
    for (int i=0; i < numFamilies; i++) {
      byte[] family = Bytes.readByteArray(in);
      int numColumns = in.readInt();
      List<KeyValue> list = new ArrayList<KeyValue>(numColumns);
      
      for (int j=0; j < numColumns; j++) {
    	KeyValue kv = new KeyValue();
    	kv.readFields(in);
    	list.add(kv);
      }
      
      this.mFamilyMap.put(family, list);
    }
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeByte(DELETE_VERSION);
    Bytes.writeByteArray(out, this.mRow);
    out.writeLong(this.mTs);
    out.writeLong(this.mLockId);
    out.writeInt(mFamilyMap.size());
    
    for (Map.Entry<byte[], List<KeyValue>> entry : mFamilyMap.entrySet()) {
      Bytes.writeByteArray(out, entry.getKey());
      List<KeyValue> list = entry.getValue();
      
      out.writeInt(list.size());
      for (KeyValue kv : list) {
        kv.write(out);
      }
    }
  }

  /**
   * Delete all versions of the specified column, given in
   * <code>family:qualifier</code> notation, and with a timestamp less than
   * or equal to the specified timestamp.
   * @param column colon-delimited family and qualifier
   * @param timestamp maximum version timestamp
   * @deprecated use {@link #deleteColumn(byte[], byte[], long)} instead
   * @return this for invocation chaining
   */
  public Delete deleteColumns(byte[] column, long timestamp) {
    byte[][] parts = KeyValue.parseColumn(column);
    this.deleteColumns(parts[0], parts[1], timestamp);
    return this;
  }

  /**
   * Delete the latest version of the specified column, given in
   * <code>family:qualifier</code> notation.
   * @param column colon-delimited family and qualifier
   * @deprecated use {@link #deleteColumn(byte[], byte[])} instead
   * @return this for invocation chaining
   */
  public Delete deleteColumn(byte[] column) {
    byte[][] parts = KeyValue.parseColumn(column);
    this.deleteColumn(parts[0], parts[1], DBConstants.LATEST_TIMESTAMP);
    return this;
  }

}
