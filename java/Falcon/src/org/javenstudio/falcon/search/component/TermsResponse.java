package org.javenstudio.falcon.search.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.params.TermsParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;

public class TermsResponse {
	
	static class TermItem {
		private String mTerm;
	    private long mFrequency;

	    public TermItem(String term, long frequency) {
	    	mTerm = term;
	    	mFrequency = frequency;
	    }

	    public String getTerm() { return mTerm; }
	    public void setTerm(String term) { mTerm = term; }
	    
	    public long getFrequency() { return mFrequency; }
	    public void setFrequency(long frequency) { mFrequency = frequency; }
	    
	    public void addFrequency(long frequency) {
	    	mFrequency += frequency;
	    }
	}
	
	/**
	 * Encapsulates responses from TermsComponent
	 */
	static class TermsMap { 
		private Map<String, List<TermItem>> mTermMap = 
				new HashMap<String, List<TermItem>>();
	  
		public TermsMap(NamedList<NamedList<Number>> termsInfo) {
			for (int i = 0; i < termsInfo.size(); i++) {
				String fieldName = termsInfo.getName(i);
				List<TermItem> itemList = new ArrayList<TermItem>();
				NamedList<Number> items = termsInfo.getVal(i);
	      
				for (int j = 0; j < items.size(); j++) {
					TermItem t = new TermItem(items.getName(j), items.getVal(j).longValue());
					itemList.add(t);
				}
	      
				mTermMap.put(fieldName, itemList);
			}
		}

		/**
		 * Get's the term list for a given field
		 * 
		 * @return the term list or null if no terms for the given field exist
		 */
		public List<TermItem> getTerms(String field) {
			return mTermMap.get(field);
		}
	  
		public Map<String, List<TermItem>> getTermMap() {
			return mTermMap;
		}
	}
	
    // map to store returned terms
    private Map<String, Map<String, TermItem>> mFieldmap;
    private Params mParams;

    public TermsResponse() {
    	mFieldmap = new HashMap<String, Map<String, TermItem>>(5);
    }

    public void init(Params params) throws ErrorException {
    	mParams = params;
    	
    	String[] fields = params.getParams(TermsParams.TERMS_FIELD);
    	if (fields != null) {
    		for (String field : fields) {
    			// TODO: not sure 128 is the best starting size
    			// It use it because that is what is used for facets
    			mFieldmap.put(field, new HashMap<String, TermItem>(128));
    		}
    	}
    }

    public void parse(NamedList<NamedList<Number>> terms) {
    	// exit if there is no terms
    	if (terms == null) 
    		return;

    	TermsMap termsResponse = new TermsMap(terms);
      
    	// loop though each field and add each term+freq to map
    	for (String key : mFieldmap.keySet()) {
    		Map<String, TermItem> termmap = mFieldmap.get(key);
    		List<TermItem> termlist = termsResponse.getTerms(key); 

    		// skip this field if there are no terms
    		if (termlist == null) 
    			continue;

    		// loop though each term
    		for (TermItem tc : termlist) {
    			String term = tc.getTerm();
    			if (termmap.containsKey(term)) {
    				TermItem oldtc = termmap.get(term);
    				oldtc.addFrequency(tc.getFrequency());
    				termmap.put(term, oldtc);
    				
    			} else {
    				termmap.put(term, tc);
    			}
    		}
    	}
	}

    public NamedList<?> buildResponse() throws ErrorException {
    	NamedList<Object> response = new NamedMap<Object>();

    	// determine if we are going index or count sort
    	boolean sort = !TermsParams.TERMS_SORT_INDEX.equals(
    			mParams.get(TermsParams.TERMS_SORT, TermsParams.TERMS_SORT_COUNT));

    	// init minimum frequency
    	long freqmin = 1;
    	String s = mParams.get(TermsParams.TERMS_MINCOUNT);
    	if (s != null) 
    		freqmin = Long.parseLong(s);

    	// init maximum frequency, default to max int
    	long freqmax = -1;
    	s = mParams.get(TermsParams.TERMS_MAXCOUNT);
    	if (s != null) 
    		freqmax = Long.parseLong(s);
    	if (freqmax < 0) 
    		freqmax = Long.MAX_VALUE;

    	// init limit, default to max int
    	long limit = 10;
    	s = mParams.get(TermsParams.TERMS_LIMIT);
    	if (s != null) 
    		limit = Long.parseLong(s);
    	if (limit < 0) 
    		limit = Long.MAX_VALUE;

    	// loop though each field we want terms from
    	for (String key : mFieldmap.keySet()) {
    		NamedList<Number> fieldterms = new NamedMap<Number>();
    		TermItem[] data = null;
    		
    		if (sort) 
    			data = getCountSorted(mFieldmap.get(key));
    		else 
    			data = getLexSorted(mFieldmap.get(key));
    		

    		// loop though each term until we hit limit
    		int cnt = 0;
    		
    		for (TermItem tc : data) {
    			if (tc.getFrequency() >= freqmin && tc.getFrequency() <= freqmax) {
    				fieldterms.add(tc.getTerm(), num(tc.getFrequency()));
    				cnt++;
    			}

    			if (cnt >= limit) 
    				break;
    		}

    		response.add(key, fieldterms);
    	}

    	return response;
    }

    // use <int> tags for smaller facet counts (better back compatibility)
    private Number num(long val) {
    	if (val < Integer.MAX_VALUE) 
    		return (int) val;
    	else 
    		return val;
    }

    // based on code from facets
    public TermItem[] getLexSorted(Map<String, TermItem> data) {
    	TermItem[] arr = data.values().toArray(new TermItem[data.size()]);

    	Arrays.sort(arr, new Comparator<TermItem>() {
	    		public int compare(TermItem o1, TermItem o2) {
	    			return o1.getTerm().compareTo(o2.getTerm());
	    		}
	    	});

    	return arr;
    }

    // based on code from facets
    public TermItem[] getCountSorted(Map<String, TermItem> data) {
    	TermItem[] arr = data.values().toArray(new TermItem[data.size()]);

    	Arrays.sort(arr, new Comparator<TermItem>() {
	    		public int compare(TermItem o1, TermItem o2) {
	    			long freq1 = o1.getFrequency();
	    			long freq2 = o2.getFrequency();
	          
	    			if (freq2 < freq1) 
	    				return -1;
	    			else if (freq1 < freq2) 
	    				return 1;
	    			
	    			return o1.getTerm().compareTo(o2.getTerm());
	    		}
	    	});

    	return arr;
    }
    
}
