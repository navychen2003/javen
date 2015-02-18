package org.javenstudio.falcon.search.update;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.params.UpdateParams;
import org.javenstudio.falcon.util.MapParams;
import org.javenstudio.falcon.util.Params;

/**
 * Common helper functions for RequestHandlers
 * 
 */
public class UpdateHelper {
	
	private static Set<String> sCommitParams = new HashSet<String>(Arrays.asList(new String[]{ 
				UpdateParams.OPEN_SEARCHER, UpdateParams.WAIT_SEARCHER, 
				UpdateParams.SOFT_COMMIT, UpdateParams.EXPUNGE_DELETES, 
				UpdateParams.MAX_OPTIMIZE_SEGMENTS, UpdateParams.PREPARE_COMMIT
			}));
	
	/**
	 * A common way to mark the response format as experimental
	 */
	public static void addExperimentalFormatWarning(ISearchResponse rsp) {
		rsp.add("WARNING", "This response format is experimental. It is likely to change in the future."); 
	}

	/**
	 * Check the request parameters and decide if it should commit or optimize.
	 * If it does, it will check other related parameters such as "waitFlush" and "waitSearcher"
	 */
	public static boolean handleCommit(ISearchRequest req, UpdateProcessor processor, 
			Params params, boolean force ) throws IOException, ErrorException {
		if (params == null) 
			params = new MapParams(new HashMap<String, String>()); 
    
		boolean optimize = params.getBool(UpdateParams.OPTIMIZE, false);
		boolean commit = params.getBool(UpdateParams.COMMIT, false);
		boolean softCommit = params.getBool(UpdateParams.SOFT_COMMIT, false);
		boolean prepareCommit = params.getBool(UpdateParams.PREPARE_COMMIT, false);

		if (optimize || commit || softCommit || prepareCommit || force) {
			CommitCommand cmd = new CommitCommand(req, optimize);
			
			updateCommit(cmd, params);
			processor.processCommit(cmd);
			
			return true;
		}
    
		return false;
	}

	public static void validateCommitParams(Params params) throws ErrorException {
		Iterator<String> i = params.getParameterNamesIterator();
		while (i.hasNext()) {
			String key = i.next();
			if (!sCommitParams.contains(key)) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Unknown commit parameter '" + key + "'");
			}
		}
	}
  
	/**
	 * Modify UpdateCommand based on request parameters
	 */
	public static void updateCommit(CommitCommand cmd, Params params) 
			throws ErrorException {
		if (params == null) return;

		cmd.setOpenSearcher(params.getBool(UpdateParams.OPEN_SEARCHER, 
				cmd.isOpenSearcher()));
		cmd.setWaitSearcher(params.getBool(UpdateParams.WAIT_SEARCHER, 
				cmd.isWaitSearcher()));
		cmd.setSoftCommit(params.getBool(UpdateParams.SOFT_COMMIT, 
				cmd.isSoftCommit()));
		cmd.setExpungeDeletes(params.getBool(UpdateParams.EXPUNGE_DELETES, 
				cmd.isExpungeDeletes()));
		cmd.setMaxOptimizeSegments(params.getInt(UpdateParams.MAX_OPTIMIZE_SEGMENTS, 
				cmd.getMaxOptimizeSegments()));
		cmd.setPrepareCommit(params.getBool(UpdateParams.PREPARE_COMMIT, 
				cmd.isPrepareCommit()));
	}

	/**
	 * @since 1.4
	 */
	public static boolean handleRollback(ISearchRequest req, UpdateProcessor processor, 
			Params params, boolean force ) throws IOException, ErrorException {
		if (params == null) 
			params = new MapParams(new HashMap<String, String>()); 
    
		boolean rollback = params.getBool(UpdateParams.ROLLBACK, false);
		if (rollback || force) {
			RollbackCommand cmd = new RollbackCommand(req);
			processor.processRollback(cmd);
			
			return true;
		}
		
		return false;
	}
	
}
