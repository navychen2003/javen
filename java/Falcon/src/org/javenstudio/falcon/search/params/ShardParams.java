package org.javenstudio.falcon.search.params;

/**
 * Parameters used for distributed search.
 */
public interface ShardParams {
	
  /** the shards to use (distributed configuration) */
  public static final String SHARDS = "shards";
  
  /** per-shard start and rows */
  public static final String SHARDS_ROWS = "shards.rows";
  public static final String SHARDS_START = "shards.start";
  
  /** IDs of the shard documents */
  public static final String IDS = "ids";
  
  /** whether the request goes to a shard */
  public static final String IS_SHARD = "isShard";
  
  /** The requested URL for this shard */
  public static final String SHARD_URL = "shard.url";
  
  /** The Request Handler for shard requests */
  public static final String SHARDS_QT = "shards.qt";
  
  /** Request detailed match info for each shard (true/false) */
  public static final String SHARDS_INFO = "shards.info";

  /** Should things fail if there is an error? (true/false) */
  public static final String SHARDS_TOLERANT = "shards.tolerant";
  
}
