package org.javenstudio.common.indexdb;

/**
 * This exception is thrown when Indexdb detects
 * an index that is newer than this Indexdb version.
 */
public class IndexFormatTooNewException extends CorruptIndexException {
	private static final long serialVersionUID = 1L;

	public IndexFormatTooNewException(String resourceDesc, int version, int minVersion, int maxVersion) {
		super("Format version is not supported (resource: " + resourceDesc + "): "
				+ version + " (needs to be between " + minVersion + " and " + maxVersion + ")");
		assert resourceDesc != null;
	}

	public IndexFormatTooNewException(IDataInput in, int version, int minVersion, int maxVersion) {
		this(in.toString(), version, minVersion, maxVersion);
	}

}
