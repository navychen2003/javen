package org.javenstudio.common.indexdb.index;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.NoSuchDirectoryException;

/**
 * This class contains useful constants representing filenames and extensions
 * used by indexdb, as well as convenience methods for querying whether a file
 * name matches an extension ({@link #matchesExtension(String, String)
 * matchesExtension}), as well as generating file names from a segment name,
 * generation and extension (
 * {@link #fileNameFromGeneration(String, String, long) fileNameFromGeneration},
 * {@link #segmentFileName(String, String, String) segmentFileName}).
 *
 * <p><b>NOTE</b>: extensions used by codecs are not
 * listed here.  You must interact with the {@link Codec}
 * directly.
 */
public final class IndexFileNames {

	/**
	 * Name of the write lock in the index.
	 */
	public static final String WRITE_LOCK_NAME = "write.lock";
	
	/** Name of the index segment file */
	public static final String SEGMENTS = "segments";

	/** Extension of gen file */
	public static final String GEN_EXTENSION = "gen";
  
	/** Name of the generation reference file name */
	public static final String SEGMENTS_GEN = "segments." +  GEN_EXTENSION;

	/** Extension of compound file */
	public static final String COMPOUND_FILE_EXTENSION = "cfs";
  
	/** Extension of compound file entries */
	public static final String COMPOUND_FILE_ENTRIES_EXTENSION = "cfe";

	/**
	 * This array contains all filename extensions used by
	 * Indexdb's index files, with one exception, namely the
	 * extension made up from  <code>.s</code> + a number.
	 * Also note that Indexdb's <code>segments_N</code> files
	 * do not have any filename extension.
	 */
	public static final String INDEX_EXTENSIONS[] = new String[] {
		COMPOUND_FILE_EXTENSION,
		COMPOUND_FILE_ENTRIES_EXTENSION,
		GEN_EXTENSION,
	};

	/**
	 * Computes the full file name from base, extension and generation. If the
	 * generation is -1, the file name is null. If it's 0, the file name is
	 * &lt;base&gt;.&lt;ext&gt;. If it's > 0, the file name is
	 * &lt;base&gt;_&lt;gen&gt;.&lt;ext&gt;.<br>
	 * <b>NOTE:</b> .&lt;ext&gt; is added to the name only if <code>ext</code> is
	 * not an empty string.
	 * 
	 * @param base main part of the file name
	 * @param ext extension of the filename
	 * @param gen generation
	 */
	public static String getFileNameFromGeneration(String base, String ext, long gen) {
		if (gen == -1) {
			return null;
		} else if (gen == 0) {
			return getSegmentFileName(base, "", ext);
		} else {
			assert gen > 0;
			// The '6' part in the length is: 1 for '.', 1 for '_' and 4 as estimate
			// to the gen length as string (hopefully an upper limit so SB won't
			// expand in the middle.
			StringBuilder res = new StringBuilder(base.length() + 6 + ext.length())
				.append(base).append('_').append(Long.toString(gen, Character.MAX_RADIX));
			if (ext.length() > 0) 
				res.append('.').append(ext);
			
			return res.toString();
		}
	}

	/**
	 * Returns a file name that includes the given segment name, your own custom
	 * name and extension. The format of the filename is:
	 * &lt;segmentName&gt;(_&lt;name&gt;)(.&lt;ext&gt;).
	 * <p>
	 * <b>NOTE:</b> .&lt;ext&gt; is added to the result file name only if
	 * <code>ext</code> is not empty.
	 * <p>
	 * <b>NOTE:</b> _&lt;segmentSuffix&gt; is added to the result file name only if
	 * it's not the empty string
	 * <p>
	 * <b>NOTE:</b> all custom files should be named using this method, or
	 * otherwise some structures may fail to handle them properly (such as if they
	 * are added to compound files).
	 */
	public static String getSegmentFileName(String segmentName, String segmentSuffix, String ext) {
		if (ext.length() > 0 || segmentSuffix.length() > 0) {
			assert !ext.startsWith(".");
			StringBuilder sb = new StringBuilder(segmentName.length() + 2 + segmentSuffix.length() + ext.length());
			sb.append(segmentName);
			if (segmentSuffix.length() > 0) 
				sb.append('_').append(segmentSuffix);
			if (ext.length() > 0) 
				sb.append('.').append(ext);
			return sb.toString();
		} else {
			return segmentName;
		}
	}

	public static String getSegmentFileName(String segmentName, String ext) {
		return getSegmentFileName(segmentName, "", ext);
	}
  
	/**
	 * Returns true if the given filename ends with the given extension. One
	 * should provide a <i>pure</i> extension, without '.'.
	 */
	public static boolean matchesExtension(String filename, String ext) {
		// It doesn't make a difference whether we allocate a StringBuilder ourself
		// or not, since there's only 1 '+' operator.
		return filename.endsWith("." + ext);
	}

	/** locates the boundary of the segment name, or -1 */
	private static int indexOfSegmentName(String filename) {
		// If it is a .del file, there's an '_' after the first character
		int idx = filename.indexOf('_', 1);
		if (idx == -1) {
			// If it's not, strip everything that's before the '.'
			idx = filename.indexOf('.');
		}
		return idx;
	}
  
	/**
	 * Strips the segment name out of the given file name. If you used
	 * {@link #segmentFileName} or {@link #fileNameFromGeneration} to create your
	 * files, then this method simply removes whatever comes before the first '.',
	 * or the second '_' (excluding both).
	 * 
	 * @return the filename with the segment name removed, or the given filename
	 *         if it does not contain a '.' and '_'.
	 */
	public static String stripSegmentName(String filename) {
		int idx = indexOfSegmentName(filename);
		if (idx != -1) 
			filename = filename.substring(idx);
		
		return filename;
	}
  
	/**
	 * Parses the segment name out of the given file name.
	 * 
	 * @return the segment name only, or filename
	 *         if it does not contain a '.' and '_'.
	 */
	public static String parseSegmentName(String filename) {
		int idx = indexOfSegmentName(filename);
		if (idx != -1) 
			filename = filename.substring(0, idx);
		
		return filename;
	}
  
	public static String stripExtension(String filename) {
		int idx = filename.indexOf('.');
		if (idx != -1) 
			filename = filename.substring(0, idx);
		
		return filename;
	}
	
	/**
	 * Get the generation of the most recent commit to the
	 * list of index files (N in the segments_N file).
	 *
	 * @param files -- array of file names to check
	 */
	public static long getLastCommitGeneration(String[] files) {
		if (files == null) 
			return -1;
		
		long max = -1;
		for (String file : files) {
			if (file.startsWith(IndexFileNames.SEGMENTS) && !file.equals(IndexFileNames.SEGMENTS_GEN)) {
				long gen = generationFromSegmentsFileName(file);
				if (gen > max) 
					max = gen;
			}
		}
		return max;
	}
	
	/**
	 * Get the generation of the most recent commit to the
	 * index in this directory (N in the segments_N file).
	 *
	 * @param directory -- directory to search for the latest segments_N file
	 */
	public static long getLastCommitGeneration(IDirectory directory) throws IOException {
		try {
			return getLastCommitGeneration(directory.listAll());
		} catch (NoSuchDirectoryException nsde) {
			return -1;
		}
	}

	/**
	 * Get the filename of the segments_N file for the most
	 * recent commit in the list of index files.
	 *
	 * @param files -- array of file names to check
	 */
	public static String getLastCommitSegmentsFileName(String[] files) throws IOException {
		return IndexFileNames.getFileNameFromGeneration(
				IndexFileNames.SEGMENTS, "", getLastCommitGeneration(files));
	}

	/**
	 * Get the filename of the segments_N file for the most
	 * recent commit to the index in this Directory.
	 *
	 * @param directory -- directory to search for the latest segments_N file
	 */
	public static String getLastCommitSegmentsFileName(IDirectory directory) throws IOException {
		return IndexFileNames.getFileNameFromGeneration(
				IndexFileNames.SEGMENTS, "", getLastCommitGeneration(directory));
	}
	
	/**
	 * Parse the generation off the segments file name and
	 * return it.
	 */
	public static long generationFromSegmentsFileName(String fileName) {
		if (fileName.equals(IndexFileNames.SEGMENTS)) {
			return 0;
		} else if (fileName.startsWith(IndexFileNames.SEGMENTS)) {
			return Long.parseLong(fileName.substring(1+IndexFileNames.SEGMENTS.length()),
                            Character.MAX_RADIX);
		} else {
			throw new IllegalArgumentException("fileName \"" + fileName + "\" is not a segments file");
		}
	}
	
	public static String getCompoundFileName(String name) { 
		return IndexFileNames.getSegmentFileName(name, IndexFileNames.COMPOUND_FILE_EXTENSION);
	}
	
	public static String getCompoundEntriesFileName(String name) { 
		return IndexFileNames.getSegmentFileName(
				IndexFileNames.stripExtension(name), "", IndexFileNames.COMPOUND_FILE_ENTRIES_EXTENSION);
	}
	
}
