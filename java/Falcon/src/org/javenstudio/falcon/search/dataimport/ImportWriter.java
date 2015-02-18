package org.javenstudio.falcon.search.dataimport;

import java.util.Map;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.update.InputDocument;

public interface ImportWriter {

	public boolean isRollbackSupported();
	
	/**
	 *  If this writer supports transactions or commit points, 
	 *  then commit any changes,
	 *  optionally optimizing the data for read/write performance
	 */
	public void commit(boolean optimize) throws ErrorException;

	/**
	 *  Release resources used by this writer. 
	 *  After calling close, reads & updates will throw exceptions.
	 */
	public void close() throws ErrorException;

	/**
	 *  If this writer supports transactions or commit points, 
	 *  then roll back any uncommitted changes.
	 */
	public void rollback() throws ErrorException;

	/**
	 *  Delete from the writer's underlying data store based the passed-in 
	 *  writer-specific query. (Optional Operation)
	 */
	public void deleteByQuery(String q) throws ErrorException;

	/**
	 *  Delete everything from the writer's underlying data store
	 */
	public void deleteAll() throws ErrorException;

	/**
	 *  Delete from the writer's underlying data store based on 
	 *  the passed-in Primary Key
	 */
	public void deleteDoc(Object key) throws ErrorException;

	/**
	 *  Add a document to this writer's underlying data store.
	 * 
	 * @return true on success, false on failure
	 */
	public void upload(InputDocument doc) throws ErrorException;

	/**
	 *  Provide context information for this writer. 
	 *  init() should be called before using the writer.
	 */
	public void init(ImportContext context) throws ErrorException;

	/**
	 *  Specify the keys to be modified by a delta update 
	 *  (required by writers that can store duplicate keys)
	 */
	public void setDeltaKeys(Set<Map<String, Object>> deltaKeys) 
			throws ErrorException;
	
}
