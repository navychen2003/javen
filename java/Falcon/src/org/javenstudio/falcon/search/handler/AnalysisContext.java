package org.javenstudio.falcon.search.handler;

import java.util.Collections;
import java.util.Set;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * Serves as the context of an analysis process. 
 * This context contains the following constructs
 */
public class AnalysisContext {
	
	public static final Set<BytesRef> EMPTY_BYTES_SET = Collections.emptySet();

    private final String mFieldName;
    private final SchemaFieldType mFieldType;
    private final IAnalyzer mAnalyzer;
    private final Set<BytesRef> mTermsToMatch;

    /**
     * Constructs a new AnalysisContext with a given field tpe, analyzer and 
     * termsToMatch. By default the field name in this context will be 
     * {@code null}. During the analysis processs, The produced tokens will 
     * be compaired to the terms in the {@code termsToMatch} set. When found, 
     * these tokens will be marked as a match.
     *
     * @param fieldType    The type of the field the analysis is performed on.
     * @param analyzer     The analyzer to be used.
     * @param termsToMatch Holds all the terms that should match during the 
     *                     analysis process.
     */
    public AnalysisContext(SchemaFieldType fieldType, IAnalyzer analyzer, 
    		Set<BytesRef> termsToMatch) {
    	this(null, fieldType, analyzer, termsToMatch);
    }

    /**
     * Constructs an AnalysisContext with a given field name, field type 
     * and analyzer. By default this context will hold no terms to match
     *
     * @param fieldName The name of the field the analysis is performed on 
     *                  (may be {@code null}).
     * @param fieldType The type of the field the analysis is performed on.
     * @param analyzer  The analyzer to be used during the analysis process.
     *
     */
    public AnalysisContext(String fieldName, SchemaFieldType fieldType, 
    		IAnalyzer analyzer) {
    	this(fieldName, fieldType, analyzer, EMPTY_BYTES_SET);
    }

    /**
     * Constructs a new AnalysisContext with a given field tpe, analyzer and
     * termsToMatch. During the analysis processs, The produced tokens will be 
     * compaired to the termes in the {@code termsToMatch} set. When found, 
     * these tokens will be marked as a match.
     *
     * @param fieldName    The name of the field the analysis is performed on 
     *                     (may be {@code null}).
     * @param fieldType    The type of the field the analysis is performed on.
     * @param analyzer     The analyzer to be used.
     * @param termsToMatch Holds all the terms that should match during the 
     *                     analysis process.
     */
    public AnalysisContext(String fieldName, SchemaFieldType fieldType, 
    		IAnalyzer analyzer, Set<BytesRef> termsToMatch) {
    	mFieldName = fieldName;
    	mFieldType = fieldType;
    	mAnalyzer = analyzer;
    	mTermsToMatch = termsToMatch;
    }

    public String getFieldName() {
    	return mFieldName;
    }

    public SchemaFieldType getFieldType() {
    	return mFieldType;
    }

    public IAnalyzer getAnalyzer() {
    	return mAnalyzer;
    }

    public Set<BytesRef> getTermsToMatch() {
    	return mTermsToMatch;
    }
    
}
