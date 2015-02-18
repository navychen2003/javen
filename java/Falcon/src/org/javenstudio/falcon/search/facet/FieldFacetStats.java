package org.javenstudio.falcon.search.facet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.stats.StatsValues;
import org.javenstudio.falcon.search.stats.StatsValuesFactory;

/**
 * 9/10/2009 - Moved out of StatsComponent to allow open access to UnInvertedField
 * FieldFacetStats is a utility to accumulate statistics on a set of values in one field,
 * for facet values present in another field.
 * <p/>
 * @see StatsComponent
 */
public class FieldFacetStats {
	
	private final Map<String, StatsValues> mFacetStatsValues;
	private final List<HashMap<String, Integer>> mFacetStatsTerms;
	private final BytesRef mTempBR = new BytesRef();
	
	private final String mName;
	private final IDocTermsIndex mTermsIndex;
	private final SchemaField mFacetSchemaField;
	private final SchemaField mFieldSchemaField;

	private final int mStartTermIndex;
	private final int mEndTermIndex;
	private final int mNumTerms;
	private final int mNumStatsTerms;

	public FieldFacetStats(String name, IDocTermsIndex si, 
			SchemaField field_sf, SchemaField facet_sf, int numStatsTerms) {
		mName = name;
		mTermsIndex = si;
		mFieldSchemaField = field_sf;
		mFacetSchemaField = facet_sf;
		mNumStatsTerms = numStatsTerms;

		mStartTermIndex = 1;
		mEndTermIndex = si.getNumOrd();
		mNumTerms = mEndTermIndex - mStartTermIndex;

		mFacetStatsValues = new HashMap<String, StatsValues>();

		// for mv stats field, we'll want to keep track of terms
		mFacetStatsTerms = new ArrayList<HashMap<String, Integer>>();
		
		if (numStatsTerms == 0) 
			return;
		
		int i = 0;
		for (; i < numStatsTerms; i++) {
			mFacetStatsTerms.add(new HashMap<String, Integer>());
		}
	}

	public String getName() { return mName; }
	public int getNumTerms() { return mNumTerms; }
	public int getNumStatsTerms() { return mNumStatsTerms; }
	
	public SchemaField getFacetSchemaField() { return mFacetSchemaField; }
	public SchemaField getFieldSchemaField() { return mFieldSchemaField; }
	
	public Map<String, StatsValues> getFacetStatsValues() { return mFacetStatsValues; }
	
	public BytesRef getTermText(int docID, BytesRef ret) {
		final int ord = mTermsIndex.getOrd(docID);
		if (ord == 0) 
			return null;
		
		return mTermsIndex.lookup(ord, ret);
	}

	public boolean facet(int docID, BytesRef v) throws ErrorException {
		int term = mTermsIndex.getOrd(docID);
		int arrIdx = term - mStartTermIndex;
		
		if (arrIdx >= 0 && arrIdx < mNumTerms) {
			final BytesRef br = mTermsIndex.lookup(term, mTempBR);
			String key = (br == null) ? null : 
				mFacetSchemaField.getType().indexedToReadable(br.utf8ToString());
			
			StatsValues stats = mFacetStatsValues.get(key);
			if (stats == null) {
				stats = StatsValuesFactory.createStatsValues(mFieldSchemaField);
				mFacetStatsValues.put(key, stats);
			}

			if (v != null && v.getLength()>0) {
				stats.accumulate(v);
				
			} else {
				stats.missing();
				return false;
			}
			
			return true;
		}
		
		return false;
	}

	// Function to keep track of facet counts for term number.
	// Currently only used by UnInvertedField stats
	public boolean facetTermNum(int docID, int statsTermNum) {
		int term = mTermsIndex.getOrd(docID);
		int arrIdx = term - mStartTermIndex;
		
		if (arrIdx >= 0 && arrIdx < mNumTerms) {
			final BytesRef br = mTermsIndex.lookup(term, mTempBR);
			String key = br == null ? null : br.utf8ToString();
			
			HashMap<String, Integer> statsTermCounts = mFacetStatsTerms.get(statsTermNum);
			Integer statsTermCount = statsTermCounts.get(key);
			
			if (statsTermCount == null) 
				statsTermCounts.put(key, 1);
			else 
				statsTermCounts.put(key, statsTermCount + 1);
			
			return true;
		}
		
		return false;
	}

	//function to accumulate counts for statsTermNum to specified value
	public boolean accumulateTermNum(int statsTermNum, BytesRef value) throws ErrorException {
		if (value == null) return false;
		
		for (Map.Entry<String, Integer> stringIntegerEntry : mFacetStatsTerms.get(statsTermNum).entrySet()) {
			Map.Entry<String, Integer> pairs = (Map.Entry<String, Integer>) stringIntegerEntry;
			
			String key = (String) pairs.getKey();
			StatsValues facetStats = mFacetStatsValues.get(key);
			
			if (facetStats == null) {
				facetStats = StatsValuesFactory.createStatsValues(mFieldSchemaField);
				mFacetStatsValues.put(key, facetStats);
			}
			
			Integer count = (Integer) pairs.getValue();
			if (count != null) 
				facetStats.accumulate(value, count);
		}
		
		return true;
	}

}
