package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.VersionedWritable;
import org.javenstudio.raptor.io.WritableComparable;

/**
 * DBRegion information.
 * Contains DBRegion id, start and end keys, a reference to this
 * DBRegions' table descriptor, etc.
 */
public class DBRegionInfo extends VersionedWritable 
		implements WritableComparable<DBRegionInfo> {
	private static final Logger LOG = Logger.getLogger(DBRegionInfo.class);
	private static final byte VERSION = 0;
	
	/**
	 * The new format for a region name contains its encodedName at the end.
	 * The encoded name also serves as the directory name for the region
	 * in the filesystem.
	 *
	 * New region name format:
	 *    &lt;tablename>,,&lt;startkey>,&lt;regionIdTimestamp>.&lt;encodedName>.
	 * where,
	 *    &lt;encodedName> is a hex version of the MD5 hash of
	 *    &lt;tablename>,&lt;startkey>,&lt;regionIdTimestamp>
	 * 
	 * The old region name format:
	 *    &lt;tablename>,&lt;startkey>,&lt;regionIdTimestamp>
	 * For region names in the old format, the encoded name is a 32-bit
	 * JenkinsHash integer value (in its decimal notation, string form). 
	 *<p>
	 * **NOTE**
	 *
	 * ROOT, the first META region, and regions created by an older
	 * version of BigDB (0.20 or prior) will continue to use the
	 * old region name format.
	 */

	/** 
	 * Separator used to demarcate the encodedName in a region name
	 * in the new format. See description on new format above. 
	 */ 
	private static final int ENC_SEPARATOR = '.';
	public  static final int MD5_HEX_LENGTH   = 32;

	/**
	 * Does region name contain its encoded name?
	 * @param regionName region name
	 * @return boolean indicating if this a new format region
	 *         name which contains its encoded name.
	 */
	private static boolean hasEncodedName(final byte[] regionName) {
		// check if region name ends in ENC_SEPARATOR
		if ((regionName.length >= 1) && (regionName[regionName.length - 1] == ENC_SEPARATOR)) {
			// region name is new format. it contains the encoded name.
			return true; 
		}
		return false;
	}
  
	/**
	 * @param regionName
	 * @return the encodedName
	 */
	public static String encodeRegionName(final byte[] regionName) {
		String encodedName;
		if (hasEncodedName(regionName)) {
			// region is in new format:
			// <tableName>,<startKey>,<regionIdTimeStamp>/encodedName/
			encodedName = Bytes.toString(regionName,
					regionName.length - MD5_HEX_LENGTH - 1,
					MD5_HEX_LENGTH);
		} else {
			// old format region name. ROOT and first META region also 
			// use this format.EncodedName is the JenkinsHash value.
			int hashVal = Math.abs(JenkinsHash.getInstance().hash(regionName,
                                                            regionName.length,
                                                            0));
			encodedName = String.valueOf(hashVal);
		}
		return encodedName;
	}

	/** delimiter used between portions of a region name */
	public static final int DELIMITER = ',';

	//TODO: Move NO_HASH to DBStoreFile which is really the only place it is used.
	public static final String NO_HASH = null;
	
	/** DBRegionInfo for root region */
	public static final DBRegionInfo ROOT_REGIONINFO =
			new DBRegionInfo(0L, DBTableDescriptor.ROOT_TABLEDESC);

	/** DBRegionInfo for first meta region */
	public static final DBRegionInfo FIRST_META_REGIONINFO =
			new DBRegionInfo(1L, DBTableDescriptor.META_TABLEDESC);

	private byte[] mEndKey = DBConstants.EMPTY_BYTE_ARRAY;
	private boolean mOffLine = false;
	private long mRegionId = -1;
	private transient byte[] mRegionName = DBConstants.EMPTY_BYTE_ARRAY;
	private String mRegionNameStr = "";
	private boolean mSplit = false;
	private byte [] mStartKey = DBConstants.EMPTY_BYTE_ARRAY;
	protected DBTableDescriptor mTableDesc = null;
	private int mHashCode = -1;
	private volatile String mEncodedName = NO_HASH;

	private void setHashCode() {
		int result = Arrays.hashCode(this.mRegionName);
		result ^= this.mRegionId;
		result ^= Arrays.hashCode(this.mStartKey);
		result ^= Arrays.hashCode(this.mEndKey);
		result ^= Boolean.valueOf(this.mOffLine).hashCode();
		result ^= this.mTableDesc.hashCode();
		this.mHashCode = result;
	}

	/**
	 * Private constructor used constructing DBRegionInfo for the catalog root and
	 * first meta regions
	 */
	public DBRegionInfo(long regionId, DBTableDescriptor tableDesc) {
		super();
		if (tableDesc == null) 
			throw new IllegalArgumentException("tableDesc cannot be null");
		
		this.mRegionId = regionId;
		this.mTableDesc = tableDesc;
    
		// Note: Root & First Meta regions names are still in old format   
		this.mRegionName = createRegionName(tableDesc.getName(), null, regionId, false);
		this.mRegionNameStr = Bytes.toStringBinary(this.mRegionName);
		
		setHashCode();
	}

	/** Default constructor - creates empty object */
	public DBRegionInfo() {
		super();
		this.mTableDesc = new DBTableDescriptor();
	}

	/**
	 * Construct DBRegionInfo with explicit parameters
	 *
	 * @param tableDesc the table descriptor
	 * @param startKey first key in region
	 * @param endKey end of key range
	 * @throws IllegalArgumentException
	 */
	public DBRegionInfo(final DBTableDescriptor tableDesc, 
			final byte[] startKey, final byte[] endKey) 
			throws IllegalArgumentException {
		this(tableDesc, startKey, endKey, false);
	}

	/**
	 * Construct DBRegionInfo with explicit parameters
	 *
	 * @param tableDesc the table descriptor
	 * @param startKey first key in region
	 * @param endKey end of key range
	 * @param split true if this region has split and we have daughter regions
	 * regions that may or may not hold references to this region.
	 * @throws IllegalArgumentException
	 */
	public DBRegionInfo(DBTableDescriptor tableDesc, 
			final byte[] startKey, final byte[] endKey, final boolean split)
			throws IllegalArgumentException {
		this(tableDesc, startKey, endKey, split, System.currentTimeMillis());
	}

	/**
	 * Construct DBRegionInfo with explicit parameters
	 *
	 * @param tableDesc the table descriptor
	 * @param startKey first key in region
	 * @param endKey end of key range
	 * @param split true if this region has split and we have daughter regions
	 * regions that may or may not hold references to this region.
	 * @param regionid Region id to use.
	 * @throws IllegalArgumentException
	 */
	public DBRegionInfo(DBTableDescriptor tableDesc, 
			final byte[] startKey, final byte[] endKey, final boolean split, 
			final long regionid) throws IllegalArgumentException {
		super();
		if (tableDesc == null) 
			throw new IllegalArgumentException("tableDesc cannot be null");
    
		this.mOffLine = false;
		this.mRegionId = regionid;
		this.mRegionName = createRegionName(tableDesc.getName(), startKey, mRegionId, true);
		this.mRegionNameStr = Bytes.toStringBinary(this.mRegionName);
		this.mSplit = split;
		this.mEndKey = (endKey == null) ? DBConstants.EMPTY_END_ROW : endKey.clone();
		this.mStartKey = (startKey == null) ? DBConstants.EMPTY_START_ROW : startKey.clone();
		this.mTableDesc = tableDesc;
		
		setHashCode();
	}

	/**
	 * Costruct a copy of another DBRegionInfo
	 *
	 * @param other
	 */
	public DBRegionInfo(DBRegionInfo other) {
		super();
		this.mEndKey = other.getEndKey();
		this.mOffLine = other.isOffline();
		this.mRegionId = other.getRegionId();
		this.mRegionName = other.getRegionName();
		this.mRegionNameStr = Bytes.toStringBinary(this.mRegionName);
		this.mSplit = other.isSplit();
		this.mStartKey = other.getStartKey();
		this.mTableDesc = other.getTableDesc();
		this.mHashCode = other.hashCode();
		this.mEncodedName = other.getEncodedName();
	}

	private static byte[] createRegionName(final byte[] tableName,
			final byte[] startKey, final long regionid, boolean newFormat) {
		return createRegionName(tableName, startKey, Long.toString(regionid), newFormat);
	}

	/**
	 * Make a region name of passed parameters.
	 * @param tableName
	 * @param startKey Can be null
	 * @param id Region id.
	 * @param newFormat should we create the region name in the new format
	 *                  (such that it contains its encoded name?).
	 * @return Region name made of passed tableName, startKey and id
	 */
	public static byte[] createRegionName(final byte[] tableName,
			final byte[] startKey, final String id, boolean newFormat) {
		return createRegionName(tableName, startKey, Bytes.toBytes(id), newFormat);
	}
	
	/**
	 * Make a region name of passed parameters.
	 * @param tableName
	 * @param startKey Can be null
	 * @param id Region id
	 * @param newFormat should we create the region name in the new format
	 *                  (such that it contains its encoded name?).
	 * @return Region name made of passed tableName, startKey and id
	 */
	public static byte[] createRegionName(final byte[] tableName,
			final byte[] startKey, final byte[] id, boolean newFormat) {
		byte[] b = new byte [tableName.length + 2 + id.length +
		                     (startKey == null ? 0 : startKey.length) +
		                     (newFormat ? (MD5_HEX_LENGTH + 2) : 0)];

		int offset = tableName.length;
		System.arraycopy(tableName, 0, b, 0, offset);
		b[offset++] = DELIMITER;
		if (startKey != null && startKey.length > 0) {
			System.arraycopy(startKey, 0, b, offset, startKey.length);
			offset += startKey.length;
		}
		b[offset++] = DELIMITER;
		System.arraycopy(id, 0, b, offset, id.length);
		offset += id.length;

		if (newFormat) {
			//
			// Encoded name should be built into the region name.
			//
			// Use the region name thus far (namely, <tablename>,<startKey>,<id>)
			// to compute a MD5 hash to be used as the encoded name, and append
			// it to the byte buffer.
			//
			String md5Hash = MD5Hash.getMD5AsHex(b, 0, offset);
			byte[] md5HashBytes = Bytes.toBytes(md5Hash);

			if (md5HashBytes.length != MD5_HEX_LENGTH) {
				if (LOG.isErrorEnabled()) {
					LOG.error("MD5-hash length mismatch: Expected=" + MD5_HEX_LENGTH +
							"; Got=" + md5HashBytes.length); 
				}
			}

			// now append the bytes '.<encodedName>.' to the end
			b[offset++] = ENC_SEPARATOR;
			System.arraycopy(md5HashBytes, 0, b, offset, MD5_HEX_LENGTH);
			offset += MD5_HEX_LENGTH;
			b[offset++] = ENC_SEPARATOR;
		}
    
		return b;
	}

	/**
	 * Separate elements of a regionName.
	 * @param regionName
	 * @return Array of byte[] containing tableName, startKey and id
	 * @throws IOException
	 */
	public static byte[][] parseRegionName(final byte[] regionName) 
			throws IOException {
		int offset = -1;
		for (int i = 0; i < regionName.length; i++) {
			if (regionName[i] == DELIMITER) {
				offset = i;
				break;
			}
		}
		
		if (offset == -1) throw new IOException("Invalid regionName format");
		
		byte[] tableName = new byte[offset];
		System.arraycopy(regionName, 0, tableName, 0, offset);
		offset = -1;
		
		for (int i = regionName.length - 1; i > 0; i--) {
			if (regionName[i] == DELIMITER) {
				offset = i;
				break;
			}
		}
		
		if (offset == -1) throw new IOException("Invalid regionName format");
		
		byte[] startKey = DBConstants.EMPTY_BYTE_ARRAY;
		if (offset != tableName.length + 1) {
			startKey = new byte[offset - tableName.length - 1];
			System.arraycopy(regionName, tableName.length + 1, startKey, 0,
					offset - tableName.length - 1);
		}
		
		byte[] id = new byte[regionName.length - offset - 1];
		System.arraycopy(regionName, offset + 1, id, 0, regionName.length - offset - 1);
		
		byte[][] elements = new byte[3][];
		elements[0] = tableName;
		elements[1] = startKey;
		elements[2] = id;
		
		return elements;
	}

	/** @return the regionId */
	public long getRegionId(){
		return mRegionId;
	}

	/**
	 * @return the regionName as an array of bytes.
	 * @see #getRegionNameAsString()
	 */
	public byte[] getRegionName(){
		return mRegionName;
	}

	/**
	 * @return Region name as a String for use in logging, etc.
	 */
	public String getRegionNameAsString() {
		if (hasEncodedName(this.mRegionName)) {
			// new format region names already have their encoded name.
			return this.mRegionNameStr;
		}

		// old format. regionNameStr doesn't have the region name.
		return this.mRegionNameStr + "." + this.getEncodedName();
	}

	/** @return the encoded region name */
	public synchronized String getEncodedName() {
		if (this.mEncodedName == NO_HASH) 
			this.mEncodedName = encodeRegionName(this.mRegionName);
		return this.mEncodedName;
	}

	/** @return the startKey */
	public byte[] getStartKey(){
		return mStartKey;
	}
  
	/** @return the endKey */
	public byte[] getEndKey(){
		return mEndKey;
	}

	/**
	 * Returns true if the given inclusive range of rows is fully contained
	 * by this region. For example, if the region is foo,a,g and this is
	 * passed ["b","c"] or ["a","c"] it will return true, but if this is passed
	 * ["b","z"] it will return false.
	 * @throws IllegalArgumentException if the range passed is invalid (ie end < start)
	 */
	public boolean containsRange(byte[] rangeStartKey, byte[] rangeEndKey) {
		if (Bytes.compareTo(rangeStartKey, rangeEndKey) > 0) {
			throw new IllegalArgumentException(
					"Invalid range: " + Bytes.toStringBinary(rangeStartKey) +
					" > " + Bytes.toStringBinary(rangeEndKey));
		}

		boolean firstKeyInRange = Bytes.compareTo(rangeStartKey, mStartKey) >= 0;
		boolean lastKeyInRange =
				Bytes.compareTo(rangeEndKey, mEndKey) < 0 ||
				Bytes.equals(mEndKey, DBConstants.EMPTY_BYTE_ARRAY);
		
		return firstKeyInRange && lastKeyInRange;
	}
  
	/**
	 * Return true if the given row falls in this region.
	 */
	public boolean containsRow(byte[] row) {
		return Bytes.compareTo(row, mStartKey) >= 0 &&
				(Bytes.compareTo(row, mEndKey) < 0 || 
						Bytes.equals(mEndKey, DBConstants.EMPTY_BYTE_ARRAY));
	}

	/** @return the tableDesc */
	public DBTableDescriptor getTableDesc(){
		return mTableDesc;
	}

	/**
	 * @param newDesc new table descriptor to use
	 */
	public void setTableDesc(DBTableDescriptor newDesc) {
		this.mTableDesc = newDesc;
	}

	/** @return true if this is the root region */
	public boolean isRootRegion() {
		return this.mTableDesc.isRootRegion();
	}

	/** @return true if this is the meta table */
	public boolean isMetaTable() {
		return this.mTableDesc.isMetaTable();
	}

	/** @return true if this region is a meta region */
	public boolean isMetaRegion() {
		return this.mTableDesc.isMetaRegion();
	}

	/** @return True if has been split and has daughters. */
	public boolean isSplit() {
		return this.mSplit;
	}

	/** @param split set split status */
	public void setSplit(boolean split) {
		this.mSplit = split;
	}

	/** @return True if this region is offline. */
	public boolean isOffline() {
		return this.mOffLine;
	}

	/** @param offLine set online - offline status */
	public void setOffline(boolean offLine) {
		this.mOffLine = offLine;
	}

	/** @see java.lang.Object#toString() */
	@Override
	public String toString() {
		return "REGION => {" + DBConstants.NAME + " => '" + this.mRegionNameStr +
				"', STARTKEY => '" + Bytes.toStringBinary(this.mStartKey) + 
				"', ENDKEY => '" + Bytes.toStringBinary(this.mEndKey) +
				"', ENCODED => " + getEncodedName() + "," +
				(isOffline() ? " OFFLINE => true," : "") +
				(isSplit() ? " SPLIT => true," : "") +
				" TABLE => {" + this.mTableDesc.toString() + "}";
	}

	/** @see java.lang.Object#equals(java.lang.Object) */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!(o instanceof DBRegionInfo)) return false;
		return this.compareTo((DBRegionInfo)o) == 0;
	}

	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode() {
		return this.mHashCode;
	}

	/** @return the object version number */
	@Override
	public byte getVersion() {
		return VERSION;
	}

	//
	// Writable
	//
	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		Bytes.writeByteArray(out, mEndKey);
		out.writeBoolean(mOffLine);
		out.writeLong(mRegionId);
		Bytes.writeByteArray(out, mRegionName);
		out.writeBoolean(mSplit);
		Bytes.writeByteArray(out, mStartKey);
		mTableDesc.write(out);
		out.writeInt(mHashCode);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		this.mEndKey = Bytes.readByteArray(in);
		this.mOffLine = in.readBoolean();
		this.mRegionId = in.readLong();
		this.mRegionName = Bytes.readByteArray(in);
		this.mRegionNameStr = Bytes.toStringBinary(this.mRegionName);
		this.mSplit = in.readBoolean();
		this.mStartKey = Bytes.readByteArray(in);
		this.mTableDesc.readFields(in);
		this.mHashCode = in.readInt();
	}

	//
	// Comparable
	//
	@Override
	public int compareTo(DBRegionInfo o) {
		if (o == null) return 1;
    
		// Are regions of same table?
		int result = this.mTableDesc.compareTo(o.mTableDesc);
		if (result != 0) return result;
    
		// Compare start keys.
		result = Bytes.compareTo(this.mStartKey, o.mStartKey);
		if (result != 0) return result;
    
		// Compare end keys.
		return Bytes.compareTo(this.mEndKey, o.mEndKey);
	}

	/**
	 * @return Comparator to use comparing {@link KeyValue}s.
	 */
	public KeyValue.KVComparator getComparator() {
		return isRootRegion() ? KeyValue.ROOT_COMPARATOR : (isMetaRegion() ?
				KeyValue.META_COMPARATOR : KeyValue.COMPARATOR);
	}
	
}
