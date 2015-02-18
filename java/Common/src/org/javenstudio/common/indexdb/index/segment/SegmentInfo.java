package org.javenstudio.common.indexdb.index.segment;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.store.TrackingDirectoryWrapper;

/**
 * Information about a segment such as it's name, directory, and files related
 * to the segment.
 */
public final class SegmentInfo implements ISegmentInfo {
  
	// TODO: remove these from this class, for now this is the representation
	public static final int NO = -1;          	// e.g. no norms; no deletes;
	public static final int YES = 1;          	// e.g. have norms; have deletes;

	private final String mName;					// unique name in dir
	private final IDirectory mDir;				// where segment resides
	private int mDocCount;						// number of docs in seg

	private boolean mIsCompoundFile;
	private volatile long mSizeInBytes = -1;  	// total byte size of all files (computed on demand)
	private Map<String,String> mDiagnostics;
	private Map<String,String> mAttributes;

	// Tracks the Indexdb version this segment was created with, since 3.1. Null
	// indicates an older than 3.0 index, and it's used to detect a too old index.
	// The format expected is "x.y" - "2.x" for pre-3.0 indexes (or null), and
	// specific versions afterwards ("3.0", "3.1" etc.).
	// see Constants.LUCENE_MAIN_VERSION.
	private String mVersion;

	private Set<String> mFileNames;
	
	/**
	 * Construct a new complete SegmentInfo instance from input.
	 * <p>Note: this is public only to allow access from
	 * the codecs package.</p>
	 */
	public SegmentInfo(IDirectory dir, String version, 
			String name, int docCount, boolean isCompoundFile, 
			Map<String,String> diagnostics, 
			Map<String,String> attributes) {
		assert !(dir instanceof TrackingDirectoryWrapper);
		mDir = dir;
		mVersion = version;
		mName = name;
		mDocCount = docCount;
		mIsCompoundFile = isCompoundFile;
		mDiagnostics = diagnostics;
		mAttributes = attributes;
	}

	@Override
	public final String getName() { return mName; }
	
	@Override
	public final IDirectory getDirectory() { return mDir; }
  
	@Override
	public void setDiagnostics(Map<String, String> diagnostics) {
		mDiagnostics = diagnostics;
	}

	@Override
	public Map<String, String> getDiagnostics() {
		return mDiagnostics;
	}
  
	/**
	 * Returns total size in bytes of all of files used by
	 * this segment.  Note that this will not include any live
	 * docs for the segment; to include that use {@link
	 * SegmentCommitInfo#sizeInBytes()} instead.
	 */
	@Override
	public long getSizeInBytes() throws IOException {
		if (mSizeInBytes == -1) {
			long sum = 0;
			for (final String fileName : getFileNames()) {
				sum += mDir.getFileLength(fileName);
			}
			mSizeInBytes = sum;
		}
		return mSizeInBytes;
	}

	/**
	 * Mark whether this segment is stored as a compound file.
	 *
	 * @param isCompoundFile true if this is a compound file;
	 * else, false
	 */
	@Override
	public void setUseCompoundFile(boolean isCompoundFile) {
		mIsCompoundFile = isCompoundFile;
	}
  
	/**
	 * Returns true if this segment is stored as a compound
	 * file; else, false.
	 */
	@Override
	public boolean getUseCompoundFile() {
		return mIsCompoundFile;
	}

	@Override
	public int getDocCount() {
		if (mDocCount == -1) 
			throw new IllegalStateException("docCount isn't set yet");
		
		return mDocCount;
	}

	// NOTE: leave package private
	@Override
	public void setDocCount(int docCount) {
		if (mDocCount != -1) 
			throw new IllegalStateException("docCount was already set");
		
		mDocCount = docCount;
	}

	/**
	 * Return all files referenced by this SegmentInfo.  The
	 * returns List is a locally cached List so you should not
	 * modify it.
	 */
	@Override
	public Set<String> getFileNames() throws IOException {
		if (mFileNames == null) 
			throw new IllegalStateException("files were not computed yet");
		
		return Collections.unmodifiableSet(mFileNames);
	}

	/** 
	 * We consider another SegmentInfo instance equal if it
	 *  has the same dir and same name. 
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof SegmentInfo) {
			final SegmentInfo other = (SegmentInfo) obj;
			return other.mDir == mDir && other.mName.equals(mName);
		} else 
			return false;
	}

	@Override
	public int hashCode() {
		return mDir.hashCode() + mName.hashCode();
	}

	/**
	 * Used by DefaultSegmentInfosReader to upgrade a 3.0 segment to record its
	 * version is "3.0". This method can be removed when we're not required to
	 * support 3x indexes anymore, e.g. in 5.0.
	 * <p>
	 * <b>NOTE:</b> this method is used for internal purposes only - you should
	 * not modify the version of a SegmentInfo, or it may result in unexpected
	 * exceptions thrown when you attempt to open the index.
	 */
	public void setVersion(String version) {
		mVersion = version;
	}

	/** Returns the version of the code which wrote the segment. */
	@Override
	public String getVersion() {
		return mVersion;
	}

	@Override
	public void setFileNames(Set<String> files) {
		mFileNames = files;
		mSizeInBytes = -1;
	}

	public void addFileNames(Collection<String> files) {
		mFileNames.addAll(files);
	}

	@Override
	public void addFileName(String file) {
		mFileNames.add(file);
	}
    
	/**
	 * Get a codec attribute value, or null if it does not exist
	 */
	@Override
	public String getAttribute(String key) {
		if (mAttributes == null) 
			return null;
		else 
			return mAttributes.get(key);
	}
  
	/**
	 * Puts a codec attribute value.
	 * <p>
	 * This is a key-value mapping for the field that the codec can use
	 * to store additional metadata, and will be available to the codec
	 * when reading the segment via {@link #getAttribute(String)}
	 * <p>
	 * If a value already exists for the field, it will be replaced with 
	 * the new value.
	 */
	public String putAttribute(String key, String value) {
		if (mAttributes == null) 
			mAttributes = new HashMap<String,String>();
		
		return mAttributes.put(key, value);
	}
  
	/**
	 * @return internal codec attributes map. May be null if no mappings exist.
	 */
	@Override
	public Map<String,String> getAttributes() {
		return mAttributes;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		return toString(mDir, 0);
	}

	/** 
	 * Used for debugging.  Format may suddenly change.
	 *
	 *  <p>Current format looks like
	 *  <code>_a(3.1):c45/4</code>, which means the segment's
	 *  name is <code>_a</code>; it was created with Indexdb 3.1 (or
	 *  '?' if it's unknown); it's using compound file
	 *  format (would be <code>C</code> if not compound); it
	 *  has 45 documents; it has 4 deletions (this part is
	 *  left off when there are no deletions).</p>
	 */
	@Override
	public String toString(IDirectory dir, int delCount) {
		StringBuilder s = new StringBuilder();
		s.append(mName).append('(').append(mVersion == null ? "?" : mVersion).append(')').append(':');
		char cfs = getUseCompoundFile() ? 'c' : 'C';
		s.append(cfs);

		if (mDir != dir) s.append('x');
		s.append(mDocCount);

		if (delCount != 0) 
			s.append('/').append(delCount);

		// TODO: we could append toString of attributes() here?
		return s.toString();
	}
	
}
