package org.javenstudio.falcon.search.transformer;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ResponseBuilder;

/**
 * Responsible for transforming the grouped result into 
 * the final format for displaying purposes.
 *
 */
public interface EndResultTransformer {

	/**
	 * Transforms the specified result into its final form and puts it 
	 * into the specified response.
	 *
	 * @param result The map containing the grouping result 
	 * (for grouping by field and query)
	 * @param rb The response builder containing the response used to 
	 * render the result and the grouping specification
	 * @param documentSource The source of {@link ResultItem} instances
	 */
	public void transform(Map<String, ?> result, ResponseBuilder rb, 
			ResultSource documentSource) throws ErrorException;

}
