package org.javenstudio.falcon.search.stats;

import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * Implementation of StatsValues that supports String values
 */
public class StringStatsValues extends AbstractStatsValues<String> {
	
	public StringStatsValues(SchemaField sf) {
		super(sf);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateTypeSpecificStats(NamedList<?> stv) {
		// No type specific stats
	}

	/**
	 * {@inheritDoc}
	 */
	protected void updateTypeSpecificStats(String value) {
		// No type specific stats
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateTypeSpecificStats(String value, int count) {
		// No type specific stats
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateMinMax(String min, String max) {
		mMax = max(mMax, max);
		mMin = min(mMin, min);
	}

	/**
	 * Adds no type specific statistics
	 */
	@Override
	protected void addTypeSpecificStats(NamedList<Object> res) {
		// Add no statistics
	}

	/**
	 * Determines which of the given Strings is the maximum, as computed by {@link String#compareTo(String)}
	 *
	 * @param str1 String to compare against b
	 * @param str2 String compared against a
	 * @return str1 if it is considered greater by {@link String#compareTo(String)}, str2 otherwise
	 */
	private static String max(String str1, String str2) {
		if (str1 == null) 
			return str2;
		else if (str2 == null) 
			return str1;
		
		return (str1.compareTo(str2) > 0) ? str1 : str2;
	}

	/**
	 * Determines which of the given Strings is the minimum, as computed by {@link String#compareTo(String)}
	 *
	 * @param str1 String to compare against b
	 * @param str2 String compared against a
	 * @return str1 if it is considered less by {@link String#compareTo(String)}, str2 otherwise
	 */
	private static String min(String str1, String str2) {
		if (str1 == null) 
			return str2;
		else if (str2 == null) 
			return str1;
		
		return (str1.compareTo(str2) < 0) ? str1 : str2;
	}
	
}
