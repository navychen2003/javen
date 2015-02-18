package org.javenstudio.common.indexdb;

/**
 * This exception is thrown when Indexdb detects
 * an index that is too old for this Indexdb version
 */
public class IndexFormatTooOldException extends CorruptIndexException {
	private static final long serialVersionUID = 1L;

	public IndexFormatTooOldException(String resourceDesc, String version) {
		super("Format version is not supported (resource: " + resourceDesc + "): " +
				version + ". This version of Indexdb only supports indexes created with release 1.9 and later.");
		assert resourceDesc != null;
	}
  
	public IndexFormatTooOldException(IDataInput in, String version) {
		this(in.toString(), version);
	}

	public IndexFormatTooOldException(String resourceDesc, int version, int minVersion, int maxVersion) {
		super("Format version is not supported (resource: " + resourceDesc + "): " +
				version + " (needs to be between " + minVersion + " and " + maxVersion +
				"). This version of Indexdb only supports indexes created with release 1.9 and later.");
		assert resourceDesc != null;
	}

	public IndexFormatTooOldException(IDataInput in, int version, int minVersion, int maxVersion) {
		this(in.toString(), version, minVersion, maxVersion);
	}
	
}
