package org.javenstudio.falcon.search.schema;

import org.javenstudio.falcon.ErrorException;

/**
 * An interface that can be extended to provide a callback mechanism for
 * informing an {@link IndexSchema} instance of changes to it, dynamically
 * performed at runtime.
 *
 */
public interface SchemaAware {
	
	/**
	 * Informs the {@link IndexSchema} provided by the <code>schema</code>
	 * parameter of an event (e.g., a new {@link SchemaFieldType} was added, etc.
	 *
	 * @param schema
	 *          The {@link IndexSchema} instance that inform of the update to.
	 *
	 * @since BUG-1131
	 */
	public void inform(IndexSchema schema) throws ErrorException;
  
}
