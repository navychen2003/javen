package org.javenstudio.falcon.search.stats;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * Abstract implementation of {@link StatsValues} that provides the default behavior
 * for most StatsValues implementations.
 *
 * There are very few requirements placed on what statistics concrete 
 * implementations should collect, with the only required
 * statistics being the minimum and maximum values.
 */
public abstract class AbstractStatsValues<T> implements StatsValues {
	
	public static final String FACETS = "facets";
	
	// facetField   facetValue
	protected Map<String, Map<String, StatsValues>> mFacets = 
			new HashMap<String, Map<String, StatsValues>>();
	
	protected final SchemaField mSchemaField;
	protected final SchemaFieldType mFieldType;
	
	protected long mMissing;
	protected long mCount;
	
	protected T mMax;
	protected T mMin;
  
	protected AbstractStatsValues(SchemaField sf) {
		mSchemaField = sf;
		mFieldType = sf.getType();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void accumulate(NamedList<?> stv) throws ErrorException {
		mCount += (Long) stv.get("count");
		mMissing += (Long) stv.get("missing");

		updateMinMax((T) stv.get("min"), (T) stv.get("max"));
		updateTypeSpecificStats(stv);

		NamedList<?> f = (NamedList<?>) stv.get(FACETS);
		if (f == null) 
			return;

		for (int i = 0; i < f.size(); i++) {
			String field = f.getName(i);
			
			NamedList<?> vals = (NamedList<?>) f.getVal(i);
			Map<String, StatsValues> addTo = mFacets.get(field);
			
			if (addTo == null) {
				addTo = new HashMap<String, StatsValues>();
				mFacets.put(field, addTo);
			}
			
			for (int j = 0; j < vals.size(); j++) {
				String val = vals.getName(j);
				StatsValues vvals = addTo.get(val);
				
				if (vvals == null) {
					vvals = StatsValuesFactory.createStatsValues(mSchemaField);
					addTo.put(val, vvals);
				}
				
				vvals.accumulate((NamedList<?>) vals.getVal(j));
			}
		}
	}
  
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void accumulate(BytesRef value) throws ErrorException {
		mCount ++;
		
		T typedValue = (T) mFieldType.toObject(mSchemaField, value);
		
		updateMinMax(typedValue, typedValue);
		updateTypeSpecificStats(typedValue);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void accumulate(BytesRef value, int count) throws ErrorException {
		mCount += count;
		
		T typedValue = (T) mFieldType.toObject(mSchemaField, value);
		
		updateMinMax(typedValue, typedValue);
		updateTypeSpecificStats(typedValue, count);
	}
  
	/**
	 * {@inheritDoc}
	 */
	public void missing() {
		mMissing ++;
	}
   
	/**
	 * {@inheritDoc}
	 */
	public void addMissing(int count) {
		mMissing += count;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addFacet(String facetName, Map<String, StatsValues> facetValues) {
		mFacets.put(facetName, facetValues);
	}

	/**
	 * {@inheritDoc}
	 */
	public NamedList<?> getStatsValues() throws ErrorException {
		NamedList<Object> res = new NamedMap<Object>();

		res.add("min", mMin);
		res.add("max", mMax);
		res.add("count", mCount);
		res.add("missing", mMissing);
		
		addTypeSpecificStats(res);

		// add the facet stats
		NamedList<NamedList<?>> nl = new NamedMap<NamedList<?>>();
		
		for (Map.Entry<String, Map<String, StatsValues>> entry : mFacets.entrySet()) {
			NamedList<NamedList<?>> nl2 = new NamedMap<NamedList<?>>();
			nl.add(entry.getKey(), nl2);
			
			for (Map.Entry<String, StatsValues> e2 : entry.getValue().entrySet()) {
				nl2.add(e2.getKey(), e2.getValue().getStatsValues());
			}
		}
		
		res.add(FACETS, nl);
		
		return res;
	}

	/**
	 * Updates the minimum and maximum statistics based on the given values
	 *
	 * @param min Value that the current minimum should be updated against
	 * @param max Value that the current maximum should be updated against
	 */
	protected abstract void updateMinMax(T min, T max);

	/**
	 * Updates the type specific statistics based on the given value
	 *
	 * @param value Value the statistics should be updated against
	 */
	protected abstract void updateTypeSpecificStats(T value);

	/**
	 * Updates the type specific statistics based on the given value
	 *
	 * @param value Value the statistics should be updated against
	 * @param count Number of times the value is being accumulated
	 */
	protected abstract void updateTypeSpecificStats(T value, int count);

	/**
	 * Updates the type specific statistics based on the values in the given list
	 *
	 * @param stv List containing values the current statistics should be updated against
	 */
	protected abstract void updateTypeSpecificStats(NamedList<?> stv);

	/**
	 * Add any type specific statistics to the given NamedList
	 *
	 * @param res NamedList to add the type specific statistics too
	 */
	protected abstract void addTypeSpecificStats(NamedList<Object> res);
	
}
