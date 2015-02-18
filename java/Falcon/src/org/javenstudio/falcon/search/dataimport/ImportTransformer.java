package org.javenstudio.falcon.search.dataimport;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;

/**
 * <p>
 * Use this API to implement a custom transformer for any given entity
 * Implementations of this abstract class must provide a public no-args constructor.
 * </p>
 * <p/>
 * <b>This API is experimental and may change in the future.</b>
 *
 * @since 1.3
 */
public abstract class ImportTransformer {
	
	/**
	 * The input is a row of data and the output has to be a new row.
	 *
	 * @param context The current context
	 * @param row     A row of data
	 * @return The changed data. It must be a {@link Map}&lt;{@link String}, 
	 * {@link Object}&gt; if it returns only one row or if there are multiple rows 
	 * to be returned it must be a {@link java.util.List}&lt;{@link Map}&lt;{@link String}, 
	 * {@link Object}&gt;&gt;
	 */
	public abstract Object transformRow(ImportRow row) throws ErrorException;
	
}
