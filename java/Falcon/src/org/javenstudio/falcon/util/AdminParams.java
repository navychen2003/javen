package org.javenstudio.falcon.util;

public interface AdminParams {

  /** What Core are we talking about **/
  public final static String CORE = "core";

  /** Should the STATUS request include index info **/
  public final static String INDEX_INFO = "indexInfo";
  
  /** Persistent -- should it save the cores state? **/
  public final static String PERSISTENT = "persistent";
  
  /** If you rename something, what is the new name **/
  public final static String NAME = "name";

  /** If you rename something, what is the new name **/
  public final static String DATA_DIR = "dataDir";

  /** Name of the other core in actions involving 2 cores **/
  public final static String OTHER = "other";

  /** What action **/
  public final static String ACTION = "action";
  
  /** If you specify a schema, what is its name **/
  public final static String SCHEMA = "schema";
  
  /** If you specify a config, what is its name **/
  public final static String CONFIG = "config";
  
  /** Specifies a core instance dir. */
  public final static String INSTANCE_DIR = "instanceDir";

  /** If you specify a file, what is its name **/
  public final static String FILE = "file";
  
  /** 
   * If you merge indexes, what are the index directories.
   * The directories are specified by multiple indexDir parameters. 
   */
  public final static String INDEX_DIR = "indexDir";

  /** 
   * If you merge indexes, what is the source core's name
   * More than one source core can be specified by multiple srcCore parameters 
   */
  public final static String SRC_CORE = "srcCore";

  /** The collection name in cloud */
  public final static String COLLECTION = "collection";

  /** The shard id in cloud */
  public final static String SHARD = "shard";
  
  public static final String ROLES = "roles";
  
  /** Prefix for core property name=value pair **/
  public final static String PROPERTY_PREFIX = "property.";

  /** If you unload a core, delete the index too */
  public final static String DELETE_INDEX = "deleteIndex";

  public static final String DELETE_DATA_DIR = "deleteDataDir";

  public static final String DELETE_INSTANCE_DIR = "deleteInstanceDir";
	
}
