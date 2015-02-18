package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.WritableFactories;

/**
 * Used to perform Scan operations.
 * <p>
 * All operations are identical to {@link Get} with the exception of
 * instantiation.  Rather than specifying a single row, an optional startRow
 * and stopRow may be defined.  If rows are not specified, the Scanner will
 * iterate over all rows.
 * <p>
 * To scan everything for each row, instantiate a Scan object.
 * <p>
 * To modify scanner caching for just this scan, use {@link #setCaching(int) setCaching}.
 * <p>
 * To further define the scope of what to get when scanning, perform additional
 * methods as outlined below.
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
 * To limit the maximum number of values returned for each call to next(),
 * execute {@link #setBatch(int) setBatch}.
 * <p>
 * To add a filter, execute {@link #setFilter(Filter) setFilter}.
 * <p>
 * Expert: To explicitly disable server-side block caching for this scan,
 * execute {@link #setCacheBlocks(boolean)}.
 */
public class Scan implements Writable {
  private static final byte SCAN_VERSION = (byte)1;
  
  private byte[] mStartRow = DBConstants.EMPTY_START_ROW;
  private byte[] mStopRow  = DBConstants.EMPTY_END_ROW;
  
  private int mMaxVersions = 1;
  private int mBatch = -1;
  private int mCaching = -1;
  private boolean mCacheBlocks = true;
  private Filter mFilter = null;
  
  private TimeRange mTr = new TimeRange();
  
  private Map<byte[], NavigableSet<byte[]>> mFamilyMap =
    new TreeMap<byte[], NavigableSet<byte[]>>(Bytes.BYTES_COMPARATOR);

  /**
   * Create a Scan operation across all rows.
   */
  public Scan() {}

  public Scan(byte[] startRow, Filter filter) {
    this(startRow);
    this.mFilter = filter;
  }

  /**
   * Create a Scan operation starting at the specified row.
   * <p>
   * If the specified row does not exist, the Scanner will start from the
   * next closest row after the specified row.
   * @param startRow row to start scanner at or after
   */
  public Scan(byte[] startRow) {
    this.mStartRow = startRow;
  }

  /**
   * Create a Scan operation for the range of rows specified.
   * @param startRow row to start scanner at or after (inclusive)
   * @param stopRow row to stop scanner before (exclusive)
   */
  public Scan(byte[] startRow, byte[] stopRow) {
    this.mStartRow = startRow;
    this.mStopRow = stopRow;
  }

  /**
   * Creates a new instance of this class while copying all values.
   *
   * @param scan  The scan instance to copy from.
   * @throws IOException When copying the values fails.
   */
  public Scan(Scan scan) throws IOException {
    mStartRow = scan.getStartRow();
    mStopRow  = scan.getStopRow();
    mMaxVersions = scan.getMaxVersions();
    mBatch = scan.getBatch();
    mCaching = scan.getCaching();
    mCacheBlocks = scan.getCacheBlocks();
    mFilter = scan.getFilter(); // clone?
    
    TimeRange ctr = scan.getTimeRange();
    mTr = new TimeRange(ctr.getMin(), ctr.getMax());
    
    Map<byte[], NavigableSet<byte[]>> fams = scan.getFamilyMap();
    
    for (Map.Entry<byte[],NavigableSet<byte[]>> entry : fams.entrySet()) {
      byte[] fam = entry.getKey();
      NavigableSet<byte[]> cols = entry.getValue();
      
      if (cols != null && cols.size() > 0) {
        for (byte[] col : cols) {
          addColumn(fam, col);
        }
      } else {
        addFamily(fam);
      }
    }
  }

  /**
   * Builds a scan object with the same specs as get.
   * @param get get to model scan after
   */
  public Scan(Get get) {
    this.mStartRow = get.getRow();
    this.mStopRow = get.getRow();
    this.mFilter = get.getFilter();
    this.mMaxVersions = get.getMaxVersions();
    this.mTr = get.getTimeRange();
    this.mFamilyMap = get.getFamilyMap();
  }

  public boolean isGetScan() {
    return this.mStartRow != null && this.mStartRow.length > 0 &&
      Bytes.equals(this.mStartRow, this.mStopRow);
  }

  /**
   * Get all columns from the specified family.
   * <p>
   * Overrides previous calls to addColumn for this family.
   * @param family family name
   * @return this
   */
  public Scan addFamily(byte[] family) {
    mFamilyMap.remove(family);
    mFamilyMap.put(family, null);
    return this;
  }

  /**
   * Get the column from the specified family with the specified qualifier.
   * <p>
   * Overrides previous calls to addFamily for this family.
   * @param family family name
   * @param qualifier column qualifier
   * @return this
   */
  public Scan addColumn(byte[] family, byte[] qualifier) {
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
   * [minStamp, maxStamp).  Note, default maximum versions to return is 1.  If
   * your time range spans more than one version and you want all versions
   * returned, up the number of versions beyond the defaut.
   * @param minStamp minimum timestamp value, inclusive
   * @param maxStamp maximum timestamp value, exclusive
   * @throws IOException if invalid time range
   * @see #setMaxVersions()
   * @see #setMaxVersions(int)
   * @return this
   */
  public Scan setTimeRange(long minStamp, long maxStamp)
      throws IOException {
    mTr = new TimeRange(minStamp, maxStamp);
    return this;
  }

  /**
   * Get versions of columns with the specified timestamp. Note, default maximum
   * versions to return is 1.  If your time range spans more than one version
   * and you want all versions returned, up the number of versions beyond the
   * defaut.
   * @param timestamp version timestamp
   * @see #setMaxVersions()
   * @see #setMaxVersions(int)
   * @return this
   */
  public Scan setTimeStamp(long timestamp) {
    try {
      mTr = new TimeRange(timestamp, timestamp+1);
    } catch(IOException e) {
      // Will never happen
    }
    return this;
  }

  /**
   * Set the start row of the scan.
   * @param startRow row to start scan on, inclusive
   * @return this
   */
  public Scan setStartRow(byte[] startRow) {
    this.mStartRow = startRow;
    return this;
  }

  /**
   * Set the stop row.
   * @param stopRow row to end at (exclusive)
   * @return this
   */
  public Scan setStopRow(byte[] stopRow) {
    this.mStopRow = stopRow;
    return this;
  }

  /**
   * Get all available versions.
   * @return this
   */
  public Scan setMaxVersions() {
    this.mMaxVersions = Integer.MAX_VALUE;
    return this;
  }

  /**
   * Get up to the specified number of versions of each column.
   * @param maxVersions maximum versions for each column
   * @return this
   */
  public Scan setMaxVersions(int maxVersions) {
    this.mMaxVersions = maxVersions;
    return this;
  }

  /**
   * Set the maximum number of values to return for each call to next()
   * @param batch the maximum number of values
   */
  public void setBatch(int batch) {
	if (this.hasFilter() && this.mFilter.hasFilterRow()) {
	  throw new IncompatibleFilterException(
        "Cannot set batch on a scan using a filter" +
        " that returns true for filter.hasFilterRow");
	}
    this.mBatch = batch;
  }

  /**
   * Set the number of rows for caching that will be passed to scanners.
   * If not set, the default setting from {@link HTable#getScannerCaching()} will apply.
   * Higher caching values will enable faster scanners but will use more memory.
   * @param caching the number of rows for caching
   */
  public void setCaching(int caching) {
    this.mCaching = caching;
  }

  /**
   * Apply the specified server-side filter when performing the Scan.
   * @param filter filter to run on the server
   * @return this
   */
  public Scan setFilter(Filter filter) {
    this.mFilter = filter;
    return this;
  }

  /**
   * Setting the familyMap
   * @param familyMap map of family to qualifier
   * @return this
   */
  public Scan setFamilyMap(Map<byte[], NavigableSet<byte[]>> familyMap) {
    this.mFamilyMap = familyMap;
    return this;
  }

  /**
   * Getting the familyMap
   * @return familyMap
   */
  public Map<byte[], NavigableSet<byte[]>> getFamilyMap() {
    return this.mFamilyMap;
  }

  /**
   * @return the number of families in familyMap
   */
  public int numFamilies() {
    if (hasFamilies()) 
      return this.mFamilyMap.size();
    
    return 0;
  }

  /**
   * @return true if familyMap is non empty, false otherwise
   */
  public boolean hasFamilies() {
    return !this.mFamilyMap.isEmpty();
  }

  /**
   * @return the keys of the familyMap
   */
  public byte[][] getFamilies() {
    if (hasFamilies()) 
      return this.mFamilyMap.keySet().toArray(new byte[0][0]);
    
    return null;
  }

  /**
   * @return the startrow
   */
  public byte[] getStartRow() {
    return this.mStartRow;
  }

  /**
   * @return the stoprow
   */
  public byte[] getStopRow() {
    return this.mStopRow;
  }

  /**
   * @return the max number of versions to fetch
   */
  public int getMaxVersions() {
    return this.mMaxVersions;
  }

  /**
   * @return maximum number of values to return for a single call to next()
   */
  public int getBatch() {
    return this.mBatch;
  }

  /**
   * @return caching the number of rows fetched when calling next on a scanner
   */
  public int getCaching() {
    return this.mCaching;
  }

  /**
   * @return TimeRange
   */
  public TimeRange getTimeRange() {
    return this.mTr;
  }

  /**
   * @return RowFilter
   */
  public Filter getFilter() {
    return mFilter;
  }

  /**
   * @return true is a filter has been specified, false if not
   */
  public boolean hasFilter() {
    return mFilter != null;
  }

  /**
   * Set whether blocks should be cached for this Scan.
   * <p>
   * This is true by default.  When true, default settings of the table and
   * family are used (this will never override caching blocks if the block
   * cache is disabled for that family or entirely).
   *
   * @param cacheBlocks if false, default settings are overridden and blocks
   * will not be cached
   */
  public void setCacheBlocks(boolean cacheBlocks) {
    this.mCacheBlocks = cacheBlocks;
  }

  /**
   * Get whether blocks should be cached for this Scan.
   * @return true if default caching should be used, false if blocks should not
   * be cached
   */
  public boolean getCacheBlocks() {
    return mCacheBlocks;
  }

  /**
   * @return String
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("startRow=");
    sb.append(Bytes.toString(this.mStartRow));
    sb.append(", stopRow=");
    sb.append(Bytes.toString(this.mStopRow));
    sb.append(", maxVersions=");
    sb.append(this.mMaxVersions);
    sb.append(", batch=");
    sb.append(this.mBatch);
    sb.append(", caching=");
    sb.append(this.mCaching);
    sb.append(", cacheBlocks=");
    sb.append(this.mCacheBlocks);
    sb.append(", timeRange=");
    sb.append("[").append(this.mTr.getMin()).append(",");
    sb.append(this.mTr.getMax()).append(")");
    sb.append(", families=");
    
    if (this.mFamilyMap.size() == 0) {
      sb.append("ALL");
      return sb.toString();
    }
    
    boolean moreThanOne = false;
    for (Map.Entry<byte[], NavigableSet<byte[]>> entry : this.mFamilyMap.entrySet()) {
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
        
        for(byte[] column : entry.getValue()) {
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

  //Writable
  @Override
  public void readFields(final DataInput in)
      throws IOException {
    int version = in.readByte();
    if (version > (int)SCAN_VERSION) 
      throw new IOException("version not supported");
    
    this.mStartRow = Bytes.readByteArray(in);
    this.mStopRow = Bytes.readByteArray(in);
    this.mMaxVersions = in.readInt();
    this.mBatch = in.readInt();
    this.mCaching = in.readInt();
    this.mCacheBlocks = in.readBoolean();
    if (in.readBoolean()) {
      this.mFilter = (Filter)createForName(Bytes.toString(Bytes.readByteArray(in)));
      this.mFilter.readFields(in);
    }
    
    this.mTr = new TimeRange();
    this.mTr.readFields(in);
    
    int numFamilies = in.readInt();
    this.mFamilyMap =
      new TreeMap<byte[], NavigableSet<byte[]>>(Bytes.BYTES_COMPARATOR);
    
    for (int i=0; i < numFamilies; i++) {
      byte[] family = Bytes.readByteArray(in);
      int numColumns = in.readInt();
      
      TreeSet<byte[]> set = new TreeSet<byte[]>(Bytes.BYTES_COMPARATOR);
      
      for (int j=0; j < numColumns; j++) {
        byte[] qualifier = Bytes.readByteArray(in);
        set.add(qualifier);
      }
      
      this.mFamilyMap.put(family, set);
    }
  }

  @Override
  public void write(final DataOutput out)
      throws IOException {
    out.writeByte(SCAN_VERSION);
    Bytes.writeByteArray(out, this.mStartRow);
    Bytes.writeByteArray(out, this.mStopRow);
    out.writeInt(this.mMaxVersions);
    out.writeInt(this.mBatch);
    out.writeInt(this.mCaching);
    out.writeBoolean(this.mCacheBlocks);
    
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
      
      if (columnSet != null){
        out.writeInt(columnSet.size());
        for (byte[] qualifier : columnSet) {
          Bytes.writeByteArray(out, qualifier);
        }
      } else {
        out.writeInt(0);
      }
    }
  }

   /**
   * Parses a combined family and qualifier and adds either both or just the
   * family in case there is not qualifier. This assumes the older colon
   * divided notation, e.g. "data:contents" or "meta:".
   * <p>
   * Note: It will through an error when the colon is missing.
   *
   * @param familyAndQualifier family and qualifier
   * @return A reference to this instance.
   * @throws IllegalArgumentException When the colon is missing.
   * @deprecated use {@link #addColumn(byte[], byte[])} instead
   */
  public Scan addColumn(byte[] familyAndQualifier) {
    byte[][] fq = KeyValue.parseColumn(familyAndQualifier);
    if (fq.length > 1 && fq[1] != null && fq[1].length > 0) {
      addColumn(fq[0], fq[1]);
    } else {
      addFamily(fq[0]);
    }
    return this;
  }

  /**
   * Adds an array of columns specified using old format, family:qualifier.
   * <p>
   * Overrides previous calls to addFamily for any families in the input.
   *
   * @param columns array of columns, formatted as <pre>family:qualifier</pre>
   * @deprecated issue multiple {@link #addColumn(byte[], byte[])} instead
   * @return this
   */
  public Scan addColumns(byte[][] columns) {
    for (byte[] column : columns) {
      addColumn(column);
    }
    return this;
  }

  /**
   * Convenience method to help parse old style (or rather user entry on the
   * command line) column definitions, e.g. "data:contents mime:". The columns
   * must be space delimited and always have a colon (":") to denote family
   * and qualifier.
   *
   * @param columns  The columns to parse.
   * @return A reference to this instance.
   * @deprecated use {@link #addColumn(byte[], byte[])} instead
   */
  public Scan addColumns(String columns) {
    String[] cols = columns.split(" ");
    for (String col : cols) {
      addColumn(Bytes.toBytes(col));
    }
    return this;
  }

  /**
   * Helps to convert the binary column families and qualifiers to a text
   * representation, e.g. "data:mimetype data:contents meta:". Binary values
   * are properly encoded using {@link Bytes#toBytesBinary(String)}.
   *
   * @return The columns in an old style string format.
   * @deprecated
   */
  public String getInputColumns() {
    StringBuilder cols = new StringBuilder("");
    
    for (Map.Entry<byte[], NavigableSet<byte[]>> e : mFamilyMap.entrySet()) {
      byte[] fam = e.getKey();
      if (cols.length() > 0) cols.append(" ");
      
      NavigableSet<byte[]> quals = e.getValue();
      
      // check if this family has qualifiers
      if (quals != null && quals.size() > 0) {
        StringBuilder cs = new StringBuilder("");
        
        for (byte[] qual : quals) {
          if (cs.length() > 0) cs.append(" ");
          
          // encode values to make parsing easier later
          cs.append(Bytes.toStringBinary(fam)).append(":")
            .append(Bytes.toStringBinary(qual));
        }
        
        cols.append(cs);
      } else {
        // only add the family but with old style delimiter
        cols.append(Bytes.toStringBinary(fam)).append(":");
      }
    }
    
    return cols.toString();
  }
}
