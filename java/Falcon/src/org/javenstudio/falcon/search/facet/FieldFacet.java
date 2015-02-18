package org.javenstudio.falcon.search.facet;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * <b>This API is experimental and subject to change</b>
 */
public class FieldFacet extends FacetBase {
	
	// the field to facet on... "myfield" for {!key=foo}myfield
	protected String mField; 
	protected SchemaFieldType mFieldType;
	
	protected int mOffset;
	protected int mLimit;
	protected int mMinCount;
	
	protected String mSort;
	protected boolean mMissing;
	protected String mPrefix;
	protected long mMissingCount;

	public FieldFacet(ResponseBuilder rb, String facetStr) throws ErrorException {
    	super(rb, FacetParams.FACET_FIELD, facetStr);
    	fillParams(rb, rb.getRequest().getParams(), mFacetOn);
    }

	public String getFieldName() { return mField; }
	public String getSortParam() { return mSort; }
	
	public int getOffset() { return mOffset; }
	public int getLimit() { return mLimit; }
	public int getMinCount() { return mMinCount; }
	
	public boolean isMissing() { return mMissing; }
	public long getMissingCount() { return mMissingCount; }
	
    private void fillParams(ResponseBuilder rb, Params params, String field) throws ErrorException {
    	mField = field;
    	mFieldType = rb.getSearchCore().getSchema().getFieldTypeNoEx(mField);
    	mOffset = params.getFieldInt(field, FacetParams.FACET_OFFSET, 0);
    	mLimit = params.getFieldInt(field, FacetParams.FACET_LIMIT, 100);
    	
    	Integer mincount = params.getFieldInt(field, FacetParams.FACET_MINCOUNT);
    	if (mincount == null) {
    		Boolean zeros = params.getFieldBool(field, FacetParams.FACET_ZEROS);
    		// mincount = (zeros!=null && zeros) ? 0 : 1;
    		mincount = (zeros!=null && !zeros) ? 1 : 0;
    		// current default is to include zeros.
    	}
    	
      	mMinCount = mincount;
      	mMissing = params.getFieldBool(field, FacetParams.FACET_MISSING, false);
      	
      	// default to sorting by count if there is a limit.
      	mSort = params.getFieldParam(field, FacetParams.FACET_SORT, 
      			mLimit>0 ? FacetParams.FACET_SORT_COUNT : FacetParams.FACET_SORT_INDEX);
      	
      	if (mSort.equals(FacetParams.FACET_SORT_COUNT_LEGACY)) 
      		mSort = FacetParams.FACET_SORT_COUNT;
      	else if (mSort.equals(FacetParams.FACET_SORT_INDEX_LEGACY)) 
      		mSort = FacetParams.FACET_SORT_INDEX;
      	
      	mPrefix = params.getFieldParam(field, FacetParams.FACET_PREFIX);
    }
    
}
