package org.javenstudio.falcon.search.stats;

import java.util.Map;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

/**
 * StatsValue defines the interface for the collection of 
 * statistical values about fields and facets.
 */
public interface StatsValues {

	/**
	 * Accumulate the values based on those in the given NamedList
	 *
	 * @param stv NamedList whose values will be used to accumulate the current values
	 */
	public void accumulate(NamedList<?> stv) throws ErrorException;

	/**
	 * Accumulate the values based on the given value
	 *
	 * @param value Value to use to accumulate the current values
	 */
	public void accumulate(BytesRef value) throws ErrorException;

	/**
	 * Accumulate the values based on the given value
	 *
	 * @param value Value to use to accumulate the current values
	 * @param count number of times to accumulate this value
	 */
	public void accumulate(BytesRef value, int count) throws ErrorException;

	/**
	 * Updates the statistics when a document is missing a value
	 */
	public void missing() throws ErrorException;

	/**
	 * Updates the statistics when multiple documents are missing a value
	 *
	 * @param count number of times to count a missing value
	 */
	public void addMissing(int count) throws ErrorException;

	/**
	 * Adds the facet statistics for the facet with the given name
	 *
	 * @param facetName Name of the facet
	 * @param facetValues Facet statistics on a per facet value basis
	 */
	public void addFacet(String facetName, Map<String, 
			StatsValues> facetValues) throws ErrorException;

	/**
	 * Translates the values into a NamedList representation
	 *
	 * @return NamedList representation of the current values
	 */
	public NamedList<?> getStatsValues() throws ErrorException;
	
}
