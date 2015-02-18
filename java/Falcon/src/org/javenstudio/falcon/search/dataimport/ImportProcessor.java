package org.javenstudio.falcon.search.dataimport;

import org.javenstudio.falcon.ErrorException;

/**
 * <p>
 * An instance of entity processor serves an entity. It is reused throughout the
 * import process.
 * </p>
 * <p>
 * Implementations of this abstract class must provide a public no-args constructor.
 * </p>
 * <p/>
 * <b>This API is experimental and may change in the future.</b>
 *
 * @since 1.3
 */
public abstract class ImportProcessor {
	
	public static interface Factory { 
		public ImportProcessor[] createProcessors(ImportContext context) throws ErrorException;
	}
	
	public abstract String getEntityName();
	
	/**
	 * This method is called when it starts processing an entity. When it comes
	 * back to the entity it is called again. So it can reset anything at that point.
	 * For a rootmost entity this is called only once for an ingestion. For sub-entities , this
	 * is called multiple once for each row from its parent entity
	 *
	 * @param req The current request
	 */
	public abstract void init(ImportRequest req) throws ErrorException;
	
	/**
	 * This method helps streaming the data for each row . The implementation
	 * would fetch as many rows as needed and gives one 'row' at a time. Only this
	 * method is used during a full import
	 *
	 * @return A 'row'.  The 'key' for the map is the column name and the 'value'
	 *         is the value of that column. If there are no more rows to be
	 *         returned, return 'null'
	 */
	public abstract ImportRow nextRow() throws ErrorException;

	/**
	 * This is used for delta-import. It gives the pks of the changed rows in this
	 * entity
	 *
	 * @return the pk vs value of all changed rows
	 */
	public abstract ImportRow nextModifiedRow() throws ErrorException;

	/**
	 * This is used during delta-import. It gives the primary keys of the rows
	 * that are deleted from this entity. If this entity is the root entity, solr
	 * document is deleted. If this is a sub-entity, the Solr document is
	 * considered as 'changed' and will be recreated
	 *
	 * @return the pk vs value of all changed rows
	 */
	public abstract ImportRow nextDeletedRow() throws ErrorException;

	/**
	 * This is used during delta-import. This gives the primary keys and their
	 * values of all the rows changed in a parent entity due to changes in this
	 * entity.
	 *
	 * @return the pk vs value of all changed rows in the parent entity
	 */
	//public abstract ImportRow nextModifiedParentRow() throws ErrorException;

	/**
	 * Invoked for each entity at the very end of the import to do any needed 
	 * cleanup tasks.
	 * 
	 */
	public abstract void destroy() throws ErrorException;

	/**
	 * Invoked after the transformers are invoked. EntityProcessors can add, 
	 * remove or modify values added by Transformers in this method.
	 *
	 * @param r The transformed row
	 * @since 1.4
	 */
	public void postTransform(ImportRow row) throws ErrorException {
		// do nothing
	}

	/**
	 * Invoked when the Entity processor is destroyed towards the end of import.
	 *
	 * @since 1.4
	 */
	public void close() throws ErrorException {
		// do nothing
	}
	
}
