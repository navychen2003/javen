package org.javenstudio.falcon.search.params;

/**
 * Group parameters
 */
public interface GroupParams {

  public static final String GROUP = "group";

  public static final String GROUP_QUERY = GROUP + ".query";
  public static final String GROUP_FIELD = GROUP + ".field";
  public static final String GROUP_FUNC = GROUP + ".func";
  public static final String GROUP_SORT = GROUP + ".sort";

  /** the limit for the number of documents in each group */
  public static final String GROUP_LIMIT = GROUP + ".limit";
  
  /** the offset for the doclist of each group */
  public static final String GROUP_OFFSET = GROUP + ".offset";

  /** treat the first group result as the main result.  true/false */
  public static final String GROUP_MAIN = GROUP + ".main";

  /** treat the first group result as the main result.  true/false */
  public static final String GROUP_FORMAT = GROUP + ".format";

  /**
   * Whether to cache the first pass search (doc ids and score) for the second pass search.
   * Also defines the maximum size of the group cache relative to maxdoc in a percentage.
   * Values can be a positive integer, from 0 till 100. A value of 0 will disable the group cache.
   * The default is 0.
   */
  public static final String GROUP_CACHE_PERCENTAGE = GROUP + ".cache.percent";

  // Note: Since you can supply multiple fields to group on, but only have 
  // a facets for the whole result. It only makes
  // sense to me to support these parameters for the first group.
  /** 
   * Whether the docSet (for example for faceting) should be based on 
   * plain documents (a.k.a UNGROUPED) or on the groups (a.k.a GROUPED).
   * The docSet will only the most relevant documents per group. 
   * It is if you query for everything with group.limit=1  
   */
  public static final String GROUP_TRUNCATE = GROUP + ".truncate";

  /** Whether the group count should be included in the response. */
  public static final String GROUP_TOTAL_COUNT = GROUP + ".ngroups";

  /** Whether to compute grouped facets based on the first specified group. */
  public static final String GROUP_FACET = GROUP + ".facet";

  /** Retrieve the top search groups (top group values) from the shards being queried.  */
  public static final String GROUP_DISTRIBUTED_FIRST = GROUP + ".distributed.first";

  /** 
   * Retrieve the top groups from the shards being queries based on 
   * the specified search groups in
   * the {@link #GROUP_DISTRIBUTED_TOPGROUPS_PREFIX} parameters.
   */
  public static final String GROUP_DISTRIBUTED_SECOND = GROUP + ".distributed.second";

  public static final String GROUP_DISTRIBUTED_TOPGROUPS_PREFIX = GROUP + ".topgroups.";
  
}

