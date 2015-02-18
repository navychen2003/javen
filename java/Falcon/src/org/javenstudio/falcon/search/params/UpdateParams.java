package org.javenstudio.falcon.search.params;

/**
 * A collection of standard params used by Update handlers
 *
 */
public interface UpdateParams {

  /** Open up a new searcher as part of a commit */
  public static String OPEN_SEARCHER = "openSearcher";

  /** wait for the searcher to be registered/visible */
  public static String WAIT_SEARCHER = "waitSearcher";

  public static String SOFT_COMMIT = "softCommit";
  
  /** overwrite indexing fields */
  public static String OVERWRITE = "overwrite";
  
  /** Commit everything after the command completes */
  public static String COMMIT = "commit";

  /** Commit within a certain time period (in ms) */
  public static String COMMIT_WITHIN = "commitWithin";

  /** Optimize the index and commit everything after the command completes */
  public static String OPTIMIZE = "optimize";

  /** expert: calls IndexWriter.prepareCommit */
  public static String PREPARE_COMMIT = "prepareCommit";

  /** Rollback update commands */
  public static String ROLLBACK = "rollback";

  /** 
   * Select the update processor chain to use. 
   * A RequestHandler may or may not respect this parameter 
   */
  public static final String UPDATE_CHAIN = "update.chain";

  /** Override the content type used for UpdateLoader **/
  public static final String ASSUME_CONTENT_TYPE = "update.contentType";
  
  /**
   * If optimizing, set the maximum number of segments left 
   * in the index after optimization. 
   * 1 is the default (and is equivalent to calling 
   * IndexWriter.optimize() in Index).
   */
  public static final String MAX_OPTIMIZE_SEGMENTS = "maxSegments";

  public static final String EXPUNGE_DELETES = "expungeDeletes";

  /** Return versions of updates? */
  public static final String VERSIONS = "versions";
  
}
