package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.javenstudio.raptor.io.Writable;

/**
 * Single row result of a {@link Get} or {@link Scan} query.<p>
 *
 * Convenience methods are available that return various {@link Map}
 * structures and values directly.<p>
 *
 * To get a complete mapping of all cells in the Result, which can include
 * multiple families and multiple versions, use {@link #getMap()}.<p>
 *
 * To get a mapping of each family to its columns (qualifiers and values),
 * including only the latest version of each, use {@link #getNoVersionMap()}.
 *
 * To get a mapping of qualifiers to latest values for an individual family use
 * {@link #getFamilyMap(byte[])}.<p>
 *
 * To get the latest value for a specific family and qualifier use {@link #getValue(byte[], byte[])}.
 *
 * A Result is backed by an array of {@link KeyValue} objects, each representing
 * an HBase cell defined by the row, family, qualifier, timestamp, and value.<p>
 *
 * The underlying {@link KeyValue} objects can be accessed through the methods
 * {@link #sorted()} and {@link #list()}.  Each KeyValue can then be accessed
 * through {@link KeyValue#getRow()}, {@link KeyValue#getFamily()}, {@link KeyValue#getQualifier()},
 * {@link KeyValue#getTimestamp()}, and {@link KeyValue#getValue()}.
 */
public class Result implements Writable {
  private static final byte RESULT_VERSION = (byte)1;

  private KeyValue[] mKvs = null;
  
  private NavigableMap<byte[],
     NavigableMap<byte[], NavigableMap<Long, byte[]>>> mFamilyMap = null;
  
  // We're not using java serialization.  Transient here is just a marker to say
  // that this is where we cache row if we're ever asked for it.
  private transient byte[] mRow = null;
  private ImmutableBytesWritable mBytes = null;

  /**
   * Constructor used for Writable.
   */
  public Result() {}

  /**
   * Instantiate a Result with the specified array of KeyValues.
   * @param kvs array of KeyValues
   */
  public Result(KeyValue [] kvs) {
    if (kvs != null && kvs.length > 0) {
      this.mKvs = kvs;
    }
  }

  /**
   * Instantiate a Result with the specified List of KeyValues.
   * @param kvs List of KeyValues
   */
  public Result(List<KeyValue> kvs) {
    this(kvs.toArray(new KeyValue[0]));
  }

  /**
   * Instantiate a Result from the specified raw binary format.
   * @param bytes raw binary format of Result
   */
  public Result(ImmutableBytesWritable bytes) {
    this.mBytes = bytes;
  }

  /**
   * Method for retrieving the row that this result is for
   * @return row
   */
  public synchronized byte[] getRow() {
    if (this.mRow == null) {
      if (this.mKvs == null) 
        readFields();
      
      this.mRow = this.mKvs.length == 0 ? null : this.mKvs[0].getRow();
    }
    return this.mRow;
  }

  /**
   * Return the unsorted array of KeyValues backing this Result instance.
   * @return unsorted array of KeyValues
   */
  public KeyValue[] raw() {
    if (this.mKvs == null) 
      readFields();
    
    return mKvs;
  }

  /**
   * Create a sorted list of the KeyValue's in this result.
   *
   * @return The sorted list of KeyValue's.
   */
  public List<KeyValue> list() {
    if (this.mKvs == null) 
      readFields();
    
    return isEmpty() ? null : Arrays.asList(sorted());
  }

  /**
   * Returns a sorted array of KeyValues in this Result.
   * <p>
   * Note: Sorting is done in place, so the backing array will be sorted
   * after calling this method.
   * @return sorted array of KeyValues
   */
  public KeyValue[] sorted() {
    if (isEmpty()) return null;
    
    Arrays.sort(mKvs, KeyValue.COMPARATOR);
    return mKvs;
  }

  /**
   * Map of families to all versions of its qualifiers and values.
   * <p>
   * Returns a three level Map of the form:
   * <code>Map<family,Map&lt;qualifier,Map&lt;timestamp,value>>></code>
   * <p>
   * Note: All other map returning methods make use of this map internally.
   * @return map from families to qualifiers to versions
   */
  public NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> getMap() {
    if (this.mFamilyMap != null) 
      return this.mFamilyMap;
    
    if (isEmpty()) 
      return null;
    
    this.mFamilyMap =
      new TreeMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>>
      (Bytes.BYTES_COMPARATOR);
    
    for (KeyValue kv : this.mKvs) {
      KeyValue.SplitKeyValue splitKV = kv.split();
      byte[] family = splitKV.getFamily();
      NavigableMap<byte[], NavigableMap<Long, byte[]>> columnMap =
        mFamilyMap.get(family);
      
      if (columnMap == null) {
        columnMap = new TreeMap<byte[], NavigableMap<Long, byte[]>>
          (Bytes.BYTES_COMPARATOR);
        mFamilyMap.put(family, columnMap);
      }
      
      byte[] qualifier = splitKV.getQualifier();
      NavigableMap<Long, byte[]> versionMap = columnMap.get(qualifier);
      
      if (versionMap == null) {
        versionMap = new TreeMap<Long, byte[]>(new Comparator<Long>() {
          public int compare(Long l1, Long l2) {
            return l2.compareTo(l1);
          }
        });
        columnMap.put(qualifier, versionMap);
      }
      
      Long timestamp = Bytes.toLong(splitKV.getTimestamp());
      byte[] value = splitKV.getValue();
      versionMap.put(timestamp, value);
    }
    
    return this.mFamilyMap;
  }

  /**
   * Map of families to their most recent qualifiers and values.
   * <p>
   * Returns a two level Map of the form: <code>Map<family,Map&lt;qualifier,value>></code>
   * <p>
   * The most recent version of each qualifier will be used.
   * @return map from families to qualifiers and value
   */
  public NavigableMap<byte[], NavigableMap<byte[], byte[]>> getNoVersionMap() {
    if (this.mFamilyMap == null) 
      getMap();
    
    if (isEmpty()) 
      return null;
    
    NavigableMap<byte[], NavigableMap<byte[], byte[]>> returnMap =
      new TreeMap<byte[], NavigableMap<byte[], byte[]>>(Bytes.BYTES_COMPARATOR);
    
    for (Map.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>>
         familyEntry : mFamilyMap.entrySet()) {
      NavigableMap<byte[], byte[]> qualifierMap =
        new TreeMap<byte[], byte[]>(Bytes.BYTES_COMPARATOR);
      
      for (Map.Entry<byte[], NavigableMap<Long, byte[]>> qualifierEntry :
           familyEntry.getValue().entrySet()) {
        byte[] value = qualifierEntry.getValue().get(qualifierEntry.getValue().firstKey());
        qualifierMap.put(qualifierEntry.getKey(), value);
      }
      
      returnMap.put(familyEntry.getKey(), qualifierMap);
    }
    
    return returnMap;
  }

  /**
   * Map of qualifiers to values.
   * <p>
   * Returns a Map of the form: <code>Map&lt;qualifier,value></code>
   * @param family column family to get
   * @return map of qualifiers to values
   */
  public NavigableMap<byte[], byte[]> getFamilyMap(byte[] family) {
    if (this.mFamilyMap == null) 
      getMap();
    
    if (isEmpty()) 
      return null;
    
    NavigableMap<byte[], byte[]> returnMap =
      new TreeMap<byte[], byte[]>(Bytes.BYTES_COMPARATOR);
    NavigableMap<byte[], NavigableMap<Long, byte[]>> qualifierMap =
      mFamilyMap.get(family);
    
    if (qualifierMap == null) 
      return returnMap;
    
    for (Map.Entry<byte[], NavigableMap<Long, byte[]>> entry :
         qualifierMap.entrySet()) {
      byte[] value = entry.getValue().get(entry.getValue().firstKey());
      returnMap.put(entry.getKey(), value);
    }
    
    return returnMap;
  }

  /**
   * Get the latest version of the specified column.
   * @param family family name
   * @param qualifier column qualifier
   * @return value of latest version of column, null if none found
   */
  public byte[] getValue(byte[] family, byte[] qualifier) {
    Map.Entry<Long,byte[]> entry = getKeyValue(family, qualifier);
    return entry == null? null: entry.getValue();
  }

  private Map.Entry<Long,byte[]> getKeyValue(byte[] family, byte[] qualifier) {
    if (this.mFamilyMap == null) 
      getMap();
    
    if (isEmpty()) 
      return null;
    
    NavigableMap<byte[], NavigableMap<Long, byte[]>> qualifierMap =
      mFamilyMap.get(family);
    if (qualifierMap == null) 
      return null;
    
    NavigableMap<Long, byte[]> versionMap =
      getVersionMap(qualifierMap, qualifier);
    if (versionMap == null) 
      return null;
    
    return versionMap.firstEntry();
  }

  private NavigableMap<Long, byte[]> getVersionMap(
      NavigableMap<byte[], NavigableMap<Long, byte[]>> qualifierMap, byte[] qualifier) {
    return qualifier != null ?
      qualifierMap.get(qualifier): qualifierMap.get(new byte[0]);
  }

  /**
   * Checks for existence of the specified column.
   * @param family family name
   * @param qualifier column qualifier
   * @return true if at least one value exists in the result, false if not
   */
  public boolean containsColumn(byte[] family, byte[] qualifier) {
    if (this.mFamilyMap == null) 
      getMap();
    
    if (isEmpty()) 
      return false;
    
    NavigableMap<byte[], NavigableMap<Long, byte[]>> qualifierMap =
      mFamilyMap.get(family);
    if (qualifierMap == null) 
      return false;
    
    NavigableMap<Long, byte[]> versionMap = getVersionMap(qualifierMap, qualifier);
    return versionMap != null;
  }

  /**
   * Returns the value of the first column in the Result.
   * @return value of the first column
   */
  public byte[] value() {
    if (isEmpty()) 
      return null;
    
    return mKvs[0].getValue();
  }

  /**
   * Returns the raw binary encoding of this Result.<p>
   *
   * Please note, there may be an offset into the underlying byte array of the
   * returned ImmutableBytesWritable.  Be sure to use both
   * {@link ImmutableBytesWritable#get()} and {@link ImmutableBytesWritable#getOffset()}
   * @return pointer to raw binary of Result
   */
  public ImmutableBytesWritable getBytes() {
    return this.mBytes;
  }

  /**
   * Check if the underlying KeyValue [] is empty or not
   * @return true if empty
   */
  public boolean isEmpty() {
    if (this.mKvs == null) 
      readFields();
    
    return this.mKvs == null || this.mKvs.length == 0;
  }

  /**
   * @return the size of the underlying KeyValue []
   */
  public int size() {
    if (this.mKvs == null) 
      readFields();
    
    return this.mKvs == null? 0: this.mKvs.length;
  }

  /**
   * @return String
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("keyvalues=");
    if (isEmpty()) {
      sb.append("NONE");
      return sb.toString();
    }
    sb.append("{");
    
    boolean moreThanOne = false;
    for (KeyValue kv : this.mKvs) {
      if (moreThanOne) {
        sb.append(", ");
      } else {
        moreThanOne = true;
      }
      sb.append(kv.toString());
    }
    
    sb.append("}");
    return sb.toString();
  }

  //Writable
  @Override
  public void readFields(final DataInput in)
      throws IOException {
    mFamilyMap = null;
    mRow = null;
    mKvs = null;
    
    int totalBuffer = in.readInt();
    if (totalBuffer == 0) {
      mBytes = null;
      return;
    }
    
    byte[] raw = new byte[totalBuffer];
    in.readFully(raw, 0, totalBuffer);
    mBytes = new ImmutableBytesWritable(raw, 0, totalBuffer);
  }

  //Create KeyValue[] when needed
  private void readFields() {
    if (mBytes == null) {
      this.mKvs = new KeyValue[0];
      return;
    }
    
    byte[] buf = mBytes.get();
    int offset = mBytes.getOffset();
    int finalOffset = mBytes.getSize() + offset;
    
    List<KeyValue> kvs = new ArrayList<KeyValue>();
    
    while (offset < finalOffset) {
      int keyLength = Bytes.toInt(buf, offset);
      offset += Bytes.SIZEOF_INT;
      kvs.add(new KeyValue(buf, offset, keyLength));
      offset += keyLength;
    }
    
    this.mKvs = kvs.toArray(new KeyValue[kvs.size()]);
  }

  @Override
  public void write(final DataOutput out)
      throws IOException {
    if (isEmpty()) {
      out.writeInt(0);
      
    } else {
      int totalLen = 0;
      for (KeyValue kv : mKvs) {
        totalLen += kv.getLength() + Bytes.SIZEOF_INT;
      }
      
      out.writeInt(totalLen);
      
      for (KeyValue kv : mKvs) {
        out.writeInt(kv.getLength());
        out.write(kv.getBuffer(), kv.getOffset(), kv.getLength());
      }
    }
  }

  public static void writeArray(final DataOutput out, Result[] results)
      throws IOException {
    // Write version when writing array form.
    // This assumes that results are sent to the client as Result[], so we
    // have an opportunity to handle version differences without affecting
    // efficiency.
    out.writeByte(RESULT_VERSION);
    if (results == null || results.length == 0) {
      out.writeInt(0);
      return;
    }
    
    out.writeInt(results.length);
    int bufLen = 0;
    
    for (Result result : results) {
      bufLen += Bytes.SIZEOF_INT;
      if (result == null || result.isEmpty()) 
        continue;
      
      for (KeyValue key : result.raw()) {
        bufLen += key.getLength() + Bytes.SIZEOF_INT;
      }
    }
    
    out.writeInt(bufLen);
    
    for (Result result : results) {
      if (result == null || result.isEmpty()) {
        out.writeInt(0);
        continue;
      }
      
      out.writeInt(result.size());
      
      for (KeyValue kv : result.raw()) {
        out.writeInt(kv.getLength());
        out.write(kv.getBuffer(), kv.getOffset(), kv.getLength());
      }
    }
  }

  public static Result[] readArray(final DataInput in)
      throws IOException {
    // Read version for array form.
    // This assumes that results are sent to the client as Result[], so we
    // have an opportunity to handle version differences without affecting
    // efficiency.
    int version = in.readByte();
    if (version > RESULT_VERSION) {
      throw new IOException("version not supported");
    }
    
    int numResults = in.readInt();
    if (numResults == 0) {
      return new Result[0];
    }
    
    Result [] results = new Result[numResults];
    int bufSize = in.readInt();
    byte[] buf = new byte[bufSize];
    int offset = 0;
    
    for (int i=0; i < numResults; i++) {
      int numKeys = in.readInt();
      offset += Bytes.SIZEOF_INT;
      
      if (numKeys == 0) {
        results[i] = new Result((ImmutableBytesWritable)null);
        continue;
      }
      
      int initialOffset = offset;
      
      for (int j=0; j < numKeys; j++) {
        int keyLen = in.readInt();
        Bytes.putInt(buf, offset, keyLen);
        offset += Bytes.SIZEOF_INT;
        in.readFully(buf, offset, keyLen);
        offset += keyLen;
      }
      
      int totalLength = offset - initialOffset;
      results[i] = new Result(new ImmutableBytesWritable(buf, initialOffset,
          totalLength));
    }
    
    return results;
  }
}
