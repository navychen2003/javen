package org.javenstudio.hornet.index.segment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexCommit;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IndexFormatTooNewException;
import org.javenstudio.common.indexdb.IndexNotFoundException;
import org.javenstudio.common.indexdb.index.IndexFileNames;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.codec.IndexFormat;
import org.javenstudio.hornet.codec.SegmentInfosFormat;

/**
 * Utility class for executing code that needs to do
 * something with the current segments file.  This is
 * necessary with lock-less commits because from the time
 * you locate the current segments file name, until you
 * actually open it, read its contents, or check modified
 * time, etc., it could have been deleted due to a writer
 * commit finishing.
 */
public abstract class SegmentFinder {
	private static final Logger LOG = Logger.getLogger(SegmentFinder.class);

	public static void findAndRead(final SegmentInfos infos, final IndexFormat format) 
			throws CorruptIndexException, IOException {
		findAndRead(infos, format, infos.getDirectory());
	}
	
	public static void findAndRead(final SegmentInfos infos, final IndexFormat format, 
			final IDirectory directory) throws CorruptIndexException, IOException {
		infos.setGeneration(-1); 
		infos.setLastGeneration(-1);

		new SegmentFinder(format.getContext(), directory) {
				@Override
				protected Object doBody(String segmentFileName) 
						throws CorruptIndexException, IOException {
					SegmentInfosFormat.read(infos, format, segmentFileName);
					return null;
				}
			}.run();
	}
	
	private final IIndexContext mContext;
    private final IDirectory mDirectory;

    SegmentFinder(IIndexContext context, IDirectory directory) {
    	mContext = context;
    	mDirectory = directory;
    }

    public final IIndexContext getContext() { return mContext; }
    public final IDirectory getDirectory() { return mDirectory; }
    
    public Object run() throws CorruptIndexException, IOException {
    	return run(null);
    }
    
    public Object run(IIndexCommit commit) throws CorruptIndexException, IOException {
    	if (commit != null) {
    		if (mDirectory != commit.getDirectory())
    			throw new IOException("the specified commit does not match the specified Directory");
    		return doBody(commit.getSegmentsFileName());
    	}

    	String segmentFileName = null;
    	long lastGen = -1;
    	long gen = 0;
    	int genLookaheadCount = 0;
    	IOException exc = null;
    	int retryCount = 0;

    	boolean useFirstMethod = true;

    	// Loop until we succeed in calling doBody() without
    	// hitting an IOException.  An IOException most likely
    	// means a commit was in process and has finished, in
    	// the time it took us to load the now-old infos files
    	// (and segments files).  It's also possible it's a
    	// true error (corrupt index).  To distinguish these,
    	// on each retry we must see "forward progress" on
    	// which generation we are trying to load.  If we
    	// don't, then the original error is real and we throw
    	// it.
      
    	// We have three methods for determining the current
    	// generation.  We try the first two in parallel (when
    	// useFirstMethod is true), and fall back to the third
    	// when necessary.
      
    	while (true) {
    		if (useFirstMethod) {

    			// List the directory and use the highest
    			// segments_N file.  This method works well as long
    			// as there is no stale caching on the directory
    			// contents (NOTE: NFS clients often have such stale
    			// caching):
    			String[] files = mDirectory.listAll();

    			long genA = -1;
    			if (files != null) 
    				genA = IndexFileNames.getLastCommitGeneration(files);
    			
    			if (LOG.isDebugEnabled())
    				LOG.debug("directory listing genA=" + genA);
          
    			// Also open segments.gen and read its
    			// contents.  Then we take the larger of the two
    			// gens.  This way, if either approach is hitting
    			// a stale cache (NFS) we have a better chance of
    			// getting the right generation.
    			long genB = -1;
    			
    			IIndexInput genInput = null;
    			try {
    				genInput = mDirectory.openInput(mContext, IndexFileNames.SEGMENTS_GEN);
    			} catch (FileNotFoundException e) {
    				if (LOG.isDebugEnabled())
    					LOG.debug("segments.gen open: FileNotFoundException " + e);
    			} catch (IOException e) {
    				if (LOG.isDebugEnabled())
    					LOG.debug("segments.gen open: IOException " + e);
    			}
  
    			if (genInput != null) {
    				try {
    					int version = genInput.readInt();
    					if (version == SegmentInfosFormat.FORMAT_SEGMENTS_GEN_CURRENT) {
    						long gen0 = genInput.readLong();
    						long gen1 = genInput.readLong();
    						
    						if (LOG.isDebugEnabled())
    							LOG.debug("fallback check: gen0=" + gen0 + " gen1=" + gen1);
    						
    						if (gen0 == gen1) {
    							// The file is consistent.
    							genB = gen0;
    						}
    					} else {
    						throw new IndexFormatTooNewException(genInput, version, 
    								SegmentInfosFormat.FORMAT_SEGMENTS_GEN_CURRENT, 
    								SegmentInfosFormat.FORMAT_SEGMENTS_GEN_CURRENT);
    					}
    				} catch (IOException err2) {
    					// rethrow any format exception
    					if (err2 instanceof CorruptIndexException) 
    						throw err2;
    				} finally {
    					genInput.close();
    				}
    			}

    			if (LOG.isDebugEnabled())
    				LOG.debug(IndexFileNames.SEGMENTS_GEN + " check: genB=" + genB);

    			// Pick the larger of the two gen's:
    			gen = Math.max(genA, genB);

    			if (gen == -1) {
    				// Neither approach found a generation
    				throw new IndexNotFoundException("no segments* file found in " + 
    						mDirectory + ": files: " + Arrays.toString(files));
    			}
    		}

    		if (useFirstMethod && lastGen == gen && retryCount >= 2) {
    			// Give up on first method -- this is 3rd cycle on
    			// listing directory and checking gen file to
    			// attempt to locate the segments file.
    			useFirstMethod = false;
    		}

    		// Second method: since both directory cache and
    		// file contents cache seem to be stale, just
    		// advance the generation.
    		if (!useFirstMethod) {
    			if (genLookaheadCount < SegmentInfosFormat.getDefaultGenLookahedCount()) {
    				gen ++;
    				genLookaheadCount ++;
    				
    				if (LOG.isDebugEnabled())
    					LOG.debug("look ahead increment gen to " + gen);
    			} else {
    				// All attempts have failed -- throw first exc:
    				throw exc;
    			}
    		} else if (lastGen == gen) {
    			// This means we're about to try the same
    			// segments_N last tried.
    			retryCount ++;
    		} else {
    			// Segment file has advanced since our last loop
    			// (we made "progress"), so reset retryCount:
    			retryCount = 0;
    		}

    		lastGen = gen;

    		segmentFileName = IndexFileNames.getFileNameFromGeneration(
    				IndexFileNames.SEGMENTS, "", gen);

    		try {
    			Object v = doBody(segmentFileName);
    			
    			if (LOG.isDebugEnabled())
    				LOG.debug("success on " + segmentFileName);
    			
    			return v;
    		} catch (IOException err) {
    			// Save the original root cause:
    			if (exc == null) 
    				exc = err;

    			if (LOG.isDebugEnabled()) {
    				LOG.debug("primary Exception on '" + segmentFileName + "': " + err + 
    						"'; will retry: retryCount=" + retryCount + "; gen = " + gen, 
    						err);
    			}
    			
    			if (gen > 1 && useFirstMethod && retryCount == 1) {
    				// This is our second time trying this same segments
    				// file (because retryCount is 1), and, there is
    				// possibly a segments_(N-1) (because gen > 1).
    				// So, check if the segments_(N-1) exists and
    				// try it if so:
    				String prevSegmentFileName = IndexFileNames.getFileNameFromGeneration(
    						IndexFileNames.SEGMENTS, "", gen-1);

    				final boolean prevExists;
    				prevExists = mDirectory.fileExists(prevSegmentFileName);

    				if (prevExists) {
    					if (LOG.isDebugEnabled())
    						LOG.debug("fallback to prior segment file '" + prevSegmentFileName + "'");
    					
    					try {
    						Object v = doBody(prevSegmentFileName);
    						
    						if (LOG.isDebugEnabled())
    							LOG.debug("success on fallback " + prevSegmentFileName);
    						
    						return v;
    					} catch (IOException err2) {
    						if (LOG.isDebugEnabled())
    							LOG.debug("secondary Exception on '" + prevSegmentFileName + "': " + err2 + "'; will retry");
    					}
    				}
    			}
    		}
    	}
    }

    /**
     * Subclass must implement this.  The assumption is an
     * IOException will be thrown if something goes wrong
     * during the processing that could have been caused by
     * a writer committing.
     */
    protected abstract Object doBody(String segmentFileName) 
    		throws CorruptIndexException, IOException;
	
}
