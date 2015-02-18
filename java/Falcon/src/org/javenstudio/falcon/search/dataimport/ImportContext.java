package org.javenstudio.falcon.search.dataimport;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchCore;

/**
 * <p>
 * This abstract class gives access to all available objects. So any
 * component implemented by a user can have the full power of DataImportHandler
 * </p>
 * <p/>
 * <b>This API is experimental and subject to change</b>
 *
 * @since 1.3
 */
public abstract class ImportContext {
	
	public static final String INDEX_START_TIME = "index_start_time";
	public static final String TIME_ELAPSED = "Time Elapsed";
	
	public static final String STATUS_MSGS = "status-messages";
	
	public static final String FULL_IMPORT_CMD = "full-import";
	public static final String IMPORT_CMD = "import";
	public static final String DELTA_IMPORT_CMD = "delta-import";
	public static final String ABORT_CMD = "abort";
	public static final String DEBUG_MODE = "debug";
	public static final String RELOAD_CONF_CMD = "reload-config";
	public static final String SHOW_CONF_CMD = "show-config";
	
	public static final String TRANSFORMER = "transformer";
	public static final String TRANSFORM_ROW = "transformRow";
	public static final String ON_ERROR = "onError";
	public static final String ABORT = "abort";
	public static final String CONTINUE = "continue";
	public static final String SKIP = "skip";
	public static final String SKIP_DOC = "$skipDoc";
	
	public static final String IMPORTER_NS_SHORT = "dih";

	/**
	 * An object stored in entity scope is valid only for 
	 * the current entity for the current document only.
	 */
	public static final String SCOPE_ENTITY = "entity";

	/**
	 * <p>
	 * If the cache supports persistent data, set to "true" to delete any prior
	 * persisted data before running the entity.
	 * </p>
	 */
	public static final String CACHE_DELETE_PRIOR_DATA = "cacheDeletePriorData";
	
	/**
	 * <p>
	 * Specify the Foreign Key from the parent entity to join on. Use if the cache
	 * is on a child entity.
	 * </p>
	 */
	public static final String CACHE_FOREIGN_KEY = "cacheLookup";

	/**
	 * <p>
	 * Specify the Primary Key field from this Entity to map the input records
	 * with
	 * </p>
	 */
	public static final String CACHE_PRIMARY_KEY = "cacheKey";
	
	/**
	 * <p>
	 * If true, a pre-existing cache is re-opened for read-only access.
	 * </p>
	 */
	public static final String CACHE_READ_ONLY = "cacheReadOnly";
	
	
	public abstract String[] getEntityNames();
	
	/**
	 * Get the value of any attribute put into this entity
	 *
	 * @param name name of the attribute eg: 'name'
	 * @return value of named attribute in entity
	 */
	public abstract ImportAttributes getAttributes();

	/**
	 * Returns the instance of EntityProcessor used for this entity
	 *
	 * @return instance of EntityProcessor used for the current entity
	 * @see org.ImportProcessor.solr.handler.dataimport.EntityProcessor
	 */
	public abstract ImportProcessor getProcessor(String entityName) 
			throws ErrorException;

	/**
	 * Store values in a certain name and scope (entity, document,global)
	 *
	 * @param name  the key
	 * @param val   the value
	 * @param scope the scope in which the given key, value pair is to be stored
	 */
	public void setSessionAttribute(String name, Object val, String scope) { 
		throw new UnsupportedOperationException();
	}

	/**
	 * get a value by name in the given scope (entity, document,global)
	 *
	 * @param name  the key
	 * @param scope the scope from which the value is to be retrieved
	 * @return the object stored in the given scope with the given key
	 */
	public Object getSessionAttribute(String name, String scope) { 
		return null;
	}

	/**
	 * Exposing the actual SolrCore to the components
	 *
	 * @return the core
	 */
	public abstract ISearchCore getSearchCore();

	/**
	 * Returns the text specified in the script tag in the data-config.xml 
	 */
	public String getScript() { 
		return null;
	}

	/**
	 * Returns the language of the script as specified in the script tag 
	 * in data-config.xml
	 */
	public String getScriptLanguage() { 
		return null;
	}

	/**
	 * Use this directly to  resolve variable
	 * @param var the variable name 
	 * @return the resolved value
	 */
	public Object resolve(String var) throws ErrorException { 
		return null;
	}

	public String toConfigXml() { 
		StringBuilder sb = new StringBuilder();
		sb.append("<dataConfig>");
		sb.append("<document>");
		for (String name : getEntityNames()) { 
			sb.append("<entity name=\"" + name + "\">");
			sb.append("</entity>");
		}
		sb.append("</document>");
		sb.append("</dataConfig>");
		return sb.toString();
	}
	
	public static final class MSG {
		public static final String NO_CONFIG_FOUND = "Configuration not found";
		public static final String NO_INIT = "DataImportHandler started. Not Initialized. No commands can be run";
		public static final String INVALID_CONFIG = "FATAL: Could not create importer. DataImporter config invalid";
		public static final String LOAD_EXP = "Exception while loading DataImporter";
		public static final String JMX_DESC = "Manage data import from databases to Solr";
		public static final String CMD_RUNNING = "A command is still running...";
		public static final String DEBUG_NOT_ENABLED = "Debug not enabled. Add a tag <str name=\"enableDebug\">true</str> in lightning.xml";
		public static final String CONFIG_RELOADED = "Configuration Re-loaded sucessfully";
		public static final String CONFIG_NOT_RELOADED = "Configuration NOT Re-loaded...Data Importer is busy.";
		public static final String TOTAL_DOC_PROCESSED = "Total Documents Processed";
		public static final String TOTAL_FAILED_DOCS = "Total Documents Failed";
		public static final String TOTAL_QUERIES_EXECUTED = "Total Requests made to DataSource";
		public static final String TOTAL_ROWS_EXECUTED = "Total Rows Fetched";
		public static final String TOTAL_DOCS_DELETED = "Total Documents Deleted";
		public static final String TOTAL_DOCS_SKIPPED = "Total Documents Skipped";
	}
	
}
