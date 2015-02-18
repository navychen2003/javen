package org.javenstudio.common.indexdb;

import java.util.Locale;

/**
 * Use by certain classes to match version compatibility
 * across releases of Indexdb.
 * 
 * <p><b>WARNING</b>: When changing the version parameter
 * that you supply to components in Indexdb, do not simply
 * change the version at search-time, but instead also adjust
 * your indexing code to match, and re-index.
 */
// remove me when java 5 is no longer supported
// this is a workaround for a JDK bug that wrongly emits a warning.
@SuppressWarnings("dep-ann")
public enum Version {
	
	/** 
	 * Match settings and bugs in Indexdb's 4.0 release. 
	 *  <p>
	 *  Use this to get the latest &amp; greatest settings, bug
	 *  fixes, etc, for Indexdb.
	 */
	INDEXDB_40;

	public boolean onOrAfter(Version other) {
		return compareTo(other) >= 0;
	}
  
	public static Version parseLeniently(String version) {
		String parsedMatchVersion = version.toUpperCase(Locale.ENGLISH);
		return Version.valueOf(parsedMatchVersion.replaceFirst("^(\\d)\\.(\\d)$", "INDEXDB_$1$2"));
	}
	
}