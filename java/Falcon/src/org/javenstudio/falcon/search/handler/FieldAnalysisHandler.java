package org.javenstudio.falcon.search.handler;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Set;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.params.AnalysisParams;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * Provides the ability to specify multiple field types and field names 
 * in the same request. Expected parameters:
 * <table border="1">
 * <tr>
 * <th align="left">Name</th>
 * <th align="left">Type</th>
 * <th align="left">required</th>
 * <th align="left">Description</th>
 * <th align="left">Multi-valued</th>
 * </tr>
 * <tr>
 * <td>analysis.fieldname</td>
 * <td>string</td>
 * <td>no</td>
 * <td>When present, the text will be analyzed based on the type of this field name.</td>
 * <td>Yes, this parameter may hold a comma-separated list of values and the analysis will 
 * be performed for each of the specified fields</td>
 * </tr>
 * <tr>
 * <td>analysis.fieldtype</td>
 * <td>string</td>
 * <td>no</td>
 * <td>When present, the text will be analyzed based on the specified type</td>
 * <td>Yes, this parameter may hold a comma-separated list of values and the analysis will 
 * be performed for each of the specified field types</td>
 * </tr>
 * <tr>
 * <td>analysis.fieldvalue</td>
 * <td>string</td>
 * <td>yes</td>
 * <td>The text that will be analyzed. The analysis will mimic the index-time analysis.</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@code analysis.query} OR {@code q}</td>
 * <td>string</td>
 * <td>no</td>
 * <td>When present, the text that will be analyzed. The analysis will mimic the query-time analysis. 
 * Note that the {@code analysis.query} parameter as precedes the {@code q} parameters.</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>analysis.showmatch</td>
 * <td>boolean</td>
 * <td>no</td>
 * <td>When set to {@code true} and when query analysis is performed, the produced tokens of 
 * the field value analysis will be marked as "matched" for every token that is produces by 
 * the query analysis</td>
 * <td>No</td>
 * </tr>
 * </table>
 * <p>Note that if neither analysis.fieldname and analysis.fieldtype is specified, 
 * then the default search field's analyzer is used.</p>
 *
 */
public class FieldAnalysisHandler extends AnalysisHandlerBase {

	private final ISearchCore mCore;
	
	public FieldAnalysisHandler(ISearchCore core) { 
		mCore = core;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected NamedList<?> doAnalysis(ISearchRequest req) throws ErrorException {
		try {
			IndexSchema indexSchema = mCore.getSchema();
			FieldAnalysisRequest analysisRequest = resolveAnalysisRequest(indexSchema, req);
			return handleAnalysisRequest(analysisRequest, indexSchema);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
		}
	}
	
	/**
	 * Resolves the AnalysisRequest based on the parameters in the given Params.
	 *
	 * @param req the request
	 * @return AnalysisRequest containing all the information about 
	 * what needs to be analyzed, and using what fields/types
	 */
	protected FieldAnalysisRequest resolveAnalysisRequest(IndexSchema schema, 
			ISearchRequest req) throws ErrorException {
		Params reqParams = req.getParams();
		FieldAnalysisRequest analysisRequest = new FieldAnalysisRequest(
				req, reqParams);

		boolean useDefaultSearchField = true;
		if (reqParams.get(AnalysisParams.FIELD_TYPE) != null) {
			analysisRequest.setFieldTypes(Arrays.asList(
					reqParams.get(AnalysisParams.FIELD_TYPE).split(",")));
			useDefaultSearchField = false;
		}
		
		if (reqParams.get(AnalysisParams.FIELD_NAME) != null) {
			analysisRequest.setFieldNames(Arrays.asList(
					reqParams.get(AnalysisParams.FIELD_NAME).split(",")));
			useDefaultSearchField = false;
		}
		
		if (useDefaultSearchField) 
			analysisRequest.addFieldName(schema.getDefaultSearchFieldName());
		
		analysisRequest.setQuery(reqParams.get(
				AnalysisParams.QUERY, reqParams.get(CommonParams.Q)));

		String value = reqParams.get(AnalysisParams.FIELD_VALUE);

		Iterable<ContentStream> streams = req.getContentStreams();
		if (streams != null) {
			// NOTE: Only the first content stream is currently processed
			for (ContentStream stream : streams) {
				Reader reader = null;
				try {
					reader = stream.getReader();
					value = IOUtils.toString(reader);
				} catch (IOException e) {
					// do nothing, leave value set to the request parameter
				} finally {
					IOUtils.closeQuietly(reader);
				}
				break;
			}
		}

		analysisRequest.setFieldValue(value);
		analysisRequest.setShowMatch(reqParams.getBool(AnalysisParams.SHOW_MATCH, false));
		
		return analysisRequest;
	}

	/**
	 * Handles the resolved analysis request and returns the analysis 
	 * breakdown response as a named list.
	 *
	 * @param request The request to handle.
	 * @param schema  The index schema.
	 *
	 * @return The analysis breakdown as a named list.
	 */
	protected NamedList<NamedList<?>> handleAnalysisRequest(
			FieldAnalysisRequest request, IndexSchema schema) throws IOException, ErrorException {
		NamedList<NamedList<?>> analysisResults = new NamedMap<NamedList<?>>();

		NamedList<NamedList<?>> fieldTypeAnalysisResults = new NamedMap<NamedList<?>>();
		if (request.getFieldTypes() != null)  {
			for (String fieldTypeName : request.getFieldTypes()) {
				SchemaFieldType fieldType = schema.getFieldTypes().get(fieldTypeName);
				fieldTypeAnalysisResults.add(fieldTypeName, 
						analyzeValues(request, fieldType, null));
			}
		}

		NamedList<NamedList<?>> fieldNameAnalysisResults = new NamedMap<NamedList<?>>();
		if (request.getFieldNames() != null)  {
			for (String fieldName : request.getFieldNames()) {
				SchemaFieldType fieldType = schema.getFieldType(fieldName);
				fieldNameAnalysisResults.add(fieldName, 
						analyzeValues(request, fieldType, fieldName));
			}
		}

		analysisResults.add("field_types", fieldTypeAnalysisResults);
		analysisResults.add("field_names", fieldNameAnalysisResults);

		return analysisResults;
	}

	/**
	 * Analyzes the index value (if it exists) and the query value (if it exists) 
	 * in the given AnalysisRequest, using the Analyzers of the given field type.
	 *
	 * @param analysisRequest AnalysisRequest from where the index and query values will be taken
	 * @param fieldType       Type of field whose analyzers will be used
	 * @param fieldName       Name of the field to be analyzed.  Can be {@code null}
	 *
	 * @return NamedList containing the tokens produced by the analyzers of the given field, 
	 * separated into an index and a query group
	 */
	private NamedList<NamedList<?>> analyzeValues(FieldAnalysisRequest analysisRequest, 
			SchemaFieldType fieldType, String fieldName) throws IOException, ErrorException {

		final String queryValue = analysisRequest.getQuery();
		final Set<BytesRef> termsToMatch = (queryValue != null && analysisRequest.isShowMatch())
				? getQueryTokenSet(queryValue, fieldType.getQueryAnalyzer())
						: AnalysisContext.EMPTY_BYTES_SET;

		NamedList<NamedList<?>> analyzeResults = new NamedMap<NamedList<?>>();
		if (analysisRequest.getFieldValue() != null) {
			AnalysisContext context = new AnalysisContext(fieldName, fieldType, 
					fieldType.getAnalyzer(), termsToMatch);
			NamedList<?> analyzedTokens = analyzeValue(analysisRequest.getFieldValue(), context);
			analyzeResults.add("index", analyzedTokens);
		}
		
		if (analysisRequest.getQuery() != null) {
			AnalysisContext context = new AnalysisContext(fieldName, fieldType, fieldType.getQueryAnalyzer());
			NamedList<?> analyzedTokens = analyzeValue(analysisRequest.getQuery(), context);
			analyzeResults.add("query", analyzedTokens);
		}

		return analyzeResults;
	}
  
}
