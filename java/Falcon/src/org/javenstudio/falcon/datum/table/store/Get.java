package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableFactories;
import org.javenstudio.raptor.conf.ConfigurationFactory;

/**
 * Used to perform Get operations on a single row.
 * <p>
 * To get everything for a row, instantiate a Get object with the row to get.
 * To further define the scope of what to get, perform additional methods as
 * outlined below.
 * <p>
 * To get all columns from specific families, execute {@link #addFamily(byte[]) addFamily}
 * for each family to retrieve.
 * <p>
 * To get specific columns, execute {@link #addColumn(byte[], byte[]) addColumn}
 * for each column to retrieve.
 * <p>
 * To only retrieve columns within a specific range of version timestamps,
 * execute {@link #setTimeRange(long, long) setTimeRange}.
 * <p>
 * To only retrieve columns with a specific timestamp, execute
 * {@link #setTimeStamp(long) setTimestamp}.
 * <p>
 * To limit the number of versions of each column to be returned, execute
 * {@link #setMaxVersions(int) setMaxVersions}.
 * <p>
 * To add a filter, execute {@link #setFilter(Filter) setFilter}.
 */
public class Get implements Writable {
  private static final byte GET_VERSION = (byte)1;
  
  private Map<byte[], NavigableSet<byte[]>> mFamilyMap =
    new TreeMap<byte[], NavigableSet<byte[]>>(Bytes.BYTES_COMPARATOR);

  private TimeRange mTr = new TimeRange();
  
  private byte[] mRow = null;
  private long mLockId = -1L;
  private int mMaxVersions = 1;
  private Filter mFilter = null;
  
  /** Constructor for Writable.  DO NOT USE */
  public Get() {}

  /**
   * Create a Get operation for the specified row.
   * <p>
   * If no further operations are done, this will get the latest version of
   * all columns in all families of the specified row.
   * @param row row key
   */
  public Get(byte[] row) {
    this(row, null);
  }

  /**
   * Create a Get operation for the specified row, using an existing row lock.
   * <p>
   * If no further operations are done, this will get the latest version of
   * all columns in all families of the specified row.
   * @param row row key
   * @param rowLock previously acquired row lock, or null
   */
  public Get(byte[] row, RowLock rowLock) {
    this.mRow = row;
    if (rowLock != null) {
      this.mLockId = rowLock.getLockId();
    }
  }

  /**
   * Get all columns from the specified family.
   * <p>
   * Overrides previous calls to addColumn for this family.
   * @param family family name
   * @return the Get object
   */
  public Get addFamily(byte[] family) {
    mFamilyMap.remove(family);
    mFamilyMap.put(family, null);
    return this;
  }

  /**
   * Get the column from the specific family with the specified qualifier.
   * <p>
   * Overrides previous calls to addFamily for this family.
   * @param family family name
   * @param qualifier column qualifier
   * @return the Get objec
   */
  public Get addColumn(byte[] family, byte[] qualifier) {
    NavigableSet<byte[]> set = mFamilyMap.get(family);
    if (set == null) {
      set = new TreeSet<byte[]>(Bytes.BYTES_COMPARATOR);
    }
    set.add(qualifier);
    mFamilyMap.put(family, set);
    return this;
  }

  /**
   * Get versions of columns only within the specified timestamp range,
   * [minStamp, maxStamp).
   * @param minStamp minimum timestamp value, inclusive
   * @param maxStamp maximum timestamp value, exclusive
   * @throws IOException if invalid time range
   * @return this for invocation chaining
   */
  public Get setTimeRange(long minStamp, long maxStamp)
      throws IOException {
    mTr = new TimeRange(minStamp, maxStamp);
    return this;
  }

  /**
   * Get versions of columns with the specified timestamp.
   * @param timestamp version timestamp
   * @return this for invocation chaining
   */
  public Get setTimeStamp(long timestamp) {
    try {
      mTr = new TimeRange(timestamp, timestamp+1);
    } catch(IOException e) {
      // Will never happen
    }
    return this;
  }

  /**
   * Get all available versions.
   * @return this for invocation chaining
   */
  public Get setMaxVersions() {
    this.mMaxVersions = Integer.MAX_VALUE;
    return this;
  }

  /**
   * Get up to the specified number of versions of each column.
   * @param maxVersions maximum versions for each column
   * @throws IOException if invalid number of versions
   * @return this for invocation chaining
   */
  public Get setMaxVersions(int maxVersions) throws IOException {
    if (maxVersions <= 0) {
      throw new IOException("maxVersions must be positive");
    }
    this.mMaxVersions = maxVersions;
    return this;
  }

  /**
   * Apply the specified server-side filter when performing the Get.
   * Only {@link Filter#filterKeyValue(KeyValue)} is called AFTER all tests
   * for ttl, column match, deletes and max versions have been run.
   * @param filter filter to run on the server
   * @return this for invocation chaining
   */
  public Get setFilter(Filter filter) {
    this.mFilter = filter;
    return this;
  }

  /* Accessors */

  /**
   * @return Filter
   */
  public Filter getFilter() {
    return this.mFilter;
  }

  /**
   * Method for retrieving the get's row
   * @return row
   */
  public byte[] getRow() {
    return this.mRow;
  }

  /**
   * Method for retrieving the get's RowLock
   * @return RowLock
   */
  public RowLock getRowLock() {
    return new RowLock(this.mRow, this.mLockId);
  }

  /**
   * Method for retrieving the get's lockId
   * @return lockId
   */
  public long getLockId() {
    return this.mLockId;
  }

  /**
   * Method for retrieving the get's maximum number of version
   * @return the maximum number of version to fetch for this get
   */
  public int getMaxVersions() {
    return this.mMaxVersions;
  }

  /**
   * Method for retrieving the get's TimeRange
   * @return timeRange
   */
  public TimeRange getTimeRange() {
    return this.mTr;
  }

  /**
   * Method for retrieving the keys in the familyMap
   * @return keys in the current familyMap
   */
  public Set<byte[]> familySet() {
    return this.mFamilyMap.keySet();
  }

  /**
   * Method for retrieving the number of families to get from
   * @return number of families
   */
  public int numFamilies() {
    return this.mFamilyMap.size();
  }

  /**
   * Method for checking if any families have been inserted into this Get
   * @return true if familyMap is non empty false otherwise
   */
  public boolean hasFamilies() {
    return !this.mFamilyMap.isEmpty();
  }

  /**
   * Method for retrieving the get's familyMap
   * @return familyMap
   */
  public Map<byte[],NavigableSet<byte[]>> getFamilyMap() {
    return this.mFamilyMap;
  }

  /**
   * @return String
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("row=");
    sb.append(Bytes.toString(this.mRow));
    sb.append(", maxVersions=");
    sb.append("").append(this.mMaxVersions);
    sb.append(", timeRange=");
    sb.append("[").append(this.mTr.getMin()).append(",");
    sb.append(this.mTr.getMax()).append(")");
    sb.append(", families=");
    
    if (this.mFamilyMap.size() == 0) {
      sb.append("ALL");
      return sb.toString();
    }
    
    boolean moreThanOne = false;
    
    for (Map.Entry<byte[], NavigableSet<byte[]>> entry :
      this.mFamilyMap.entrySet()) {
      if (moreThanOne) {
        sb.append("), ");
      } else {
        moreThanOne = true;
        sb.append("{");
      }
      
      sb.append("(family=");
      sb.append(Bytes.toString(entry.getKey()));
      sb.append(", columns=");
      
      if (entry.getValue() == null) {
        sb.append("ALL");
      } else {
        sb.append("{");
        boolean moreThanOneB = false;
        
        for (byte[] column : entry.getValue()) {
          if (moreThanOneB) {
            sb.append(", ");
          } else {
            moreThanOneB = true;
          }
          sb.append(Bytes.toString(column));
        }
        sb.append("}");
      }
    }
    
    sb.append("}");
    return sb.toString();
  }

  //Writable
  @Override
  public void readFields(final DataInput in)
      throws IOException {
    int version = in.readByte();
    if (version > GET_VERSION) 
      throw new IOException("unsupported version");
    
    this.mRow = Bytes.readByteArray(in);
    this.mLockId = in.readLong();
    this.mMaxVersions = in.readInt();
    
    boolean hasFilter = in.readBoolean();
    if (hasFilter) {
      this.mFilter = (Filter)createForName(Bytes.toString(Bytes.readByteArray(in)));
      this.mFilter.readFields(in);
    }
    
    this.mTr = new TimeRange();
    this.mTr.readFields(in);
    
    int numFamilies = in.readInt();
    this.mFamilyMap =
      new TreeMap<byte[],NavigableSet<byte[]>>(Bytes.BYTES_COMPARATOR);
    
    for (int i=0; i < numFamilies; i++) {
      byte[] family = Bytes.readByteArray(in);
      boolean hasColumns = in.readBoolean();
      NavigableSet<byte[]> set = null;
      
      if (hasColumns) {
        int numColumns = in.readInt();
        set = new TreeSet<byte[]>(Bytes.BYTES_COMPARATOR);
        
        for (int j=0; j < numColumns; j++) {
          byte[] qualifier = Bytes.readByteArray(in);
          set.add(qualifier);
        }
      }
      this.mFamilyMap.put(family, set);
    }
  }

  @Override
  public void write(final DataOutput out)
      throws IOException {
    out.writeByte(GET_VERSION);
    Bytes.writeByteArray(out, this.mRow);
    out.writeLong(this.mLockId);
    out.writeInt(this.mMaxVersions);
    
    if (this.mFilter == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      Bytes.writeByteArray(out, Bytes.toBytes(mFilter.getClass().getName()));
      mFilter.write(out);
    }
    
    mTr.write(out);
    out.writeInt(mFamilyMap.size());
    
    for (Map.Entry<byte[], NavigableSet<byte[]>> entry : mFamilyMap.entrySet()) {
      Bytes.writeByteArray(out, entry.getKey());
      NavigableSet<byte[]> columnSet = entry.getValue();
      
      if (columnSet == null) {
        out.writeBoolean(false);
      } else {
        out.writeBoolean(true);
        out.writeInt(columnSet.size());
        
        for (byte[] qualifier : columnSet) {
          Bytes.writeByteArray(out, qualifier);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Writable createForName(String className) {
    try {
      Class<? extends Writable> clazz =
        (Class<? extends Writable>) Class.forName(className);
      return WritableFactories.newInstance(clazz, ConfigurationFactory.get());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Can't find class " + className);
    }
  }

  /**
   * Adds an array of columns specified the old format, family:qualifier.
   * <p>
   * Overrides previous calls to addFamily for any families in the input.
   * @param columns array of columns, formatted as <pre>family:qualifier</pre>
   * @deprecated issue multiple {@link #addColumn(byte[], byte[])} instead
   * @return this for invocation chaining
   */
  public Get addColumns(byte[][] columns) {
    if (columns == null) return this;
    for (byte[] column : columns) {
      try {
        addColumn(column);
      } catch (Exception ignored) {
      }
    }
    return this;
  }

  /**
   * @param column Old format column.
   * @return This.
   * @deprecated use {@link #addColumn(byte[], byte[])} instead
   */
  public Get addColumn(final byte[] column) {
    if (column == null) return this;
    byte[][] split = KeyValue.parseColumn(column);
    if (split.length > 1 && split[1] != null && split[1].length > 0) {
      addColumn(split[0], split[1]);
    } else {
      addFamily(split[0]);
    }
    return this;
  }
}
