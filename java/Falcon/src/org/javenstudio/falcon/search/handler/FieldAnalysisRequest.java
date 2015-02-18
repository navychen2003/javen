package org.javenstudio.falcon.search.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.params.AnalysisParams;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.Params;

/**
 * A request for the FieldAnalysisRequestHandler.
 *
 */
public class FieldAnalysisRequest implements ISearchRequest {

	private final ISearchRequest mRequest;
	private final Params mOrigParams;
	
	private List<String> mFieldNames;
	private List<String> mFieldTypes;
	
	private String mFieldValue;
	private String mQuery;
	
	private boolean mShowMatch;

	/**
	 * Constructs a new FieldAnalysisRequest with a default uri of "/fieldanalysis".
	 */
	public FieldAnalysisRequest(ISearchRequest req, Params params) { 
		mRequest = req; 
		mOrigParams = params;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Params getParams() {
		ModifiableParams params = new ModifiableParams();
		params.set(AnalysisParams.FIELD_VALUE, mFieldValue);
		if (mQuery != null) {
			params.add(AnalysisParams.QUERY, mQuery);
			params.add(AnalysisParams.SHOW_MATCH, String.valueOf(mShowMatch));
		}
		if (mFieldNames != null) {
			String fieldNameValue = listToCommaDelimitedString(mFieldNames);
			params.add(AnalysisParams.FIELD_NAME, fieldNameValue);
		}
		if (mFieldTypes != null) {
			String fieldTypeValue = listToCommaDelimitedString(mFieldTypes);
			params.add(AnalysisParams.FIELD_TYPE, fieldTypeValue);
		}
		return params;
	}

	/**
	 * Convers the given list of string to a comma-separated string.
	 *
	 * @param list The list of string.
	 * @return The comma-separated string.
	 */
	public static String listToCommaDelimitedString(List<String> list) {
		StringBuilder result = new StringBuilder();
		for (String str : list) {
			if (result.length() > 0) 
				result.append(",");
			result.append(str);
		}
		return result.toString();
	}

	/**
	 * Sets the field value to be analyzed.
	 *
	 * @param fieldValue The field value to be analyzed.
	 * @return This FieldAnalysisRequest (fluent interface support).
	 */
	public FieldAnalysisRequest setFieldValue(String fieldValue) {
		mFieldValue = fieldValue;
		return this;
	}

	/**
	 * Returns the field value that will be analyzed when this request is processed.
	 *
	 * @return The field value that will be analyzed when this request is processed.
	 */
	public String getFieldValue() {
		return mFieldValue;
	}

	/**
	 * Sets the query to be analyzed. May be {@code null} indicated 
	 * that no query analysis should take place.
	 *
	 * @param query The query to be analyzed.
	 * @return This FieldAnalysisRequest (fluent interface support).
	 */
	public FieldAnalysisRequest setQuery(String query) {
		mQuery = query;
		return this;
	}

	/**
	 * Returns the query that will be analyzed. May return {@code null} indicating 
	 * that no query analysis will be performed.
	 *
	 * @return The query that will be analyzed. May return {@code null} indicating 
	 * that no query analysis will be performed.
	 */
	public String getQuery() {
		return mQuery;
	}

	/**
	 * Sets whether index time tokens that match query time tokens should be marked 
	 * as a "match". By default this is set
	 * to {@code false}. Obviously, this flag is ignored 
	 * if when the query is set to {@code null}.
	 *
	 * @param showMatch Sets whether index time tokens that match query time tokens 
	 * should be marked as a "match".
	 * @return This FieldAnalysisRequest (fluent interface support).
	 */
	public FieldAnalysisRequest setShowMatch(boolean showMatch) {
		mShowMatch = showMatch;
		return this;
	}

	/**
	 * Returns whether index time tokens that match query time tokens should be marked as a "match".
	 *
	 * @return Whether index time tokens that match query time tokens should be marked as a "match".
	 *
	 * @see #setShowMatch(boolean)
	 */
	public boolean isShowMatch() {
		return mShowMatch;
	}

	/**
	 * Adds the given field name for analysis.
	 *
	 * @param fieldName A field name on which the analysis should be performed.
	 * @return this FieldAnalysisRequest (fluent interface support).
	 */
	public FieldAnalysisRequest addFieldName(String fieldName) {
		if (mFieldNames == null) 
			mFieldNames = new LinkedList<String>();
		
		mFieldNames.add(fieldName);
		return this;
	}

	/**
     * Sets the field names on which the analysis should be performed.
     *
     * @param fieldNames The field names on which the analysis should be performed.
     * @return this FieldAnalysisRequest (fluent interface support).
     */
	public FieldAnalysisRequest setFieldNames(List<String> fieldNames) {
		mFieldNames = fieldNames;
		return this;
	}

	/**
	 * Returns a list of field names the analysis should be performed on. 
	 * May return {@code null} indicating that no
	 * analysis will be performed on field names.
	 *
	 * @return The field names the analysis should be performed on.
	 */
	public List<String> getFieldNames() {
		return mFieldNames;
	}

	/**
	 * Adds the given field type for analysis.
	 *
	 * @param fieldTypeName A field type name on which analysis should be performed.
	 *
	 * @return This FieldAnalysisRequest (fluent interface support).
	 */
	public FieldAnalysisRequest addFieldType(String fieldTypeName) {
		if (mFieldTypes == null) 
			mFieldTypes = new LinkedList<String>();
		
		mFieldTypes.add(fieldTypeName);
		return this;
	}

	/**
	 * Sets the field types on which analysis should be performed.
	 *
	 * @param fieldTypes The field type names on which analysis should be performed.
	 *
	 * @return This FieldAnalysisRequest (fluent interface support).
	 */
	public FieldAnalysisRequest setFieldTypes(List<String> fieldTypes) {
		mFieldTypes = fieldTypes;
		return this;
	}

	/**
	 * Returns a list of field types the analysis should be performed on. 
	 * May return {@code null} indicating that no
	 * analysis will be peformed on field types.
	 *
	 * @return The field types the analysis should be performed on.
	 */
	public List<String> getFieldTypes() {
		return mFieldTypes;
	}

	@Override
	public ISearchCore getSearchCore() {
		return mRequest.getSearchCore();
	}

	@Override
	public Searcher getSearcher() throws ErrorException {
		return mRequest.getSearcher();
	}

	@Override
	public Params getOriginalParams() {
		return mOrigParams;
	}

	@Override
	public void setParams(Params params) {
		// do nothing
	}

	@Override
	public Map<Object, Object> getContextMap() {
		return null;
	}

	@Override
	public Iterable<ContentStream> getContentStreams() {
		return null;
	}

	@Override
	public long getStartTime() {
		return mRequest.getStartTime();
	}

	@Override
	public void close() throws ErrorException {
		// do nothing
	}

}
