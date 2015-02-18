package org.javenstudio.falcon.search.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.Base64Utils;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.SearchWriter;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.analysis.TokenizerChain;
import org.javenstudio.falcon.search.schema.CopyField;
import org.javenstudio.falcon.search.schema.FieldFlag;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.hornet.index.field.MultiFields;
import org.javenstudio.panda.analysis.CharFilterFactory;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.analysis.TokenizerFactory;

/**
 * This handler exposes the internal index.  It is inspired by and 
 * modeled on Luke, the Index Browser by Andrzej Bialecki.
 *   http://www.getopt.org/luke/
 *
 */
public class LukeRequestHandler {
	//private static final Logger LOG = Logger.getLogger(LukeRequestHandler.class);

	public static final String NUMTERMS = "numTerms";
	public static final String DOC_ID = "docId";
	public static final String ID = "id";
	
	public static final int DEFAULT_COUNT = 10;
	public static final int HIST_ARRAY_SIZE = 33;

	static enum ShowStyle {
		ALL,
		DOC,
		SCHEMA,
		INDEX;

		public static ShowStyle get(String v) throws ErrorException {
			if (v == null) return null;
			
			if ("schema".equalsIgnoreCase(v)) return SCHEMA;
			if ("index".equalsIgnoreCase(v))  return INDEX;
			if ("doc".equalsIgnoreCase(v))    return DOC;
			if ("all".equalsIgnoreCase(v))    return ALL;
			
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unknown Show Style: " + v);
		}
	};
	
	private final ISearchCore mCore;
	
	public LukeRequestHandler(ISearchCore core) { 
		mCore = core;
	}
	
	public void handleRequestBody(ISearchRequest req, ISearchResponse rsp)
			throws ErrorException {
		final ISearchCore core = mCore;
	    final IndexSchema schema = core.getSchema();
	    final IDirectoryReader reader = req.getSearcher().getDirectoryReader();
	    
	    doHandlerRequestBody(req, rsp, schema, reader);
	}
	
	@SuppressWarnings("unused")
	protected void doHandlerRequestBody(ISearchRequest req, ISearchResponse rsp, 
			IndexSchema schema, IDirectoryReader reader) throws ErrorException {
	    Params params = req.getParams();
	    ShowStyle style = ShowStyle.get(params.get("show"));

	    // If no doc is given, show all fields and top terms

	    rsp.add("index", getIndexInfo(reader));

	    if (ShowStyle.INDEX == style) 
	    	return; // that's all we need

	    Integer docId = params.getInt(DOC_ID);
	    if (docId == null && params.get(ID) != null) {
	    	// Look for something with a given ID
	    	SchemaField uniqueKey = schema.getUniqueKeyField();
	    	String v = uniqueKey.getType().toInternal(params.get(ID));
	    	//Term t = new Term(uniqueKey.getName(), v);
	    	docId = 0; //searcher.getFirstMatch( t );
	    	if (docId < 0) {
	    		throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
	    				"Can't find document: " + params.get(ID));
	    	}
	    }

	    // Read the document from the index
	    if (docId != null) {
	    	if (style != null && style != ShowStyle.DOC) {
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    				"missing doc param for doc style");
	    	}
	    	
	    	IDocument doc = null;
	    	try {
	    		doc = null; //reader.document( docId );
	    	} catch (Exception ex) {}
	    	if (doc == null) {
	    		throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
	    				"Can't find document: " + docId);
	    	}

	    	NamedMap<Object> info = getDocumentFieldsInfo(doc, docId, reader, schema);
	    	NamedMap<Object> docinfo = new NamedMap<Object>();
	    	docinfo.add("docId", docId);
	    	docinfo.add("indexdb", info);
	    	docinfo.add("lightning", doc);
	    	
	    	rsp.add("doc", docinfo);
	    	
	    } else if (ShowStyle.SCHEMA == style) {
	    	rsp.add("schema", getSchemaInfo(schema));
	    	
	    } else {
	    	rsp.add("fields", getIndexedFieldsInfo(req)) ;
	    }

	    // Add some generally helpful information
	    NamedList<Object> info = new NamedMap<Object>();
	    info.add("key", getFieldFlagsKey());
	    info.add("NOTE", "Document Frequency (df) is not updated when a document is marked for deletion. " 
	    		+ "df values include deleted documents.");
	    
	    rsp.add("info", info );
	    //rsp.setHttpCaching(false);
	}

	/**
	 * @return a string representing a IndexableField's flags.  
	 */
	@SuppressWarnings("unused")
	static String getFieldFlags(Fieldable f) {
		IndexOptions opts = (f == null) ? null : f.getFieldType().getIndexOptions();

		StringBuilder flags = new StringBuilder();

		flags.append((f != null && f.getFieldType().isIndexed()) 
				? FieldFlag.INDEXED.getAbbreviation() : '-' );
		flags.append((f != null && f.getFieldType().isTokenized()) 
				? FieldFlag.TOKENIZED.getAbbreviation() : '-' );
		flags.append((f != null && f.getFieldType().isStored()) 
				? FieldFlag.STORED.getAbbreviation() : '-' );
		
		flags.append((false) 
				? FieldFlag.MULTI_VALUED.getAbbreviation() : '-' ); // SchemaField Specific
		
		flags.append((f != null && f.getFieldType().isStoreTermVectors()) 
				? FieldFlag.TERM_VECTOR_STORED.getAbbreviation() : '-' );
		flags.append((f != null && f.getFieldType().isStoreTermVectorOffsets()) 
				? FieldFlag.TERM_VECTOR_OFFSET.getAbbreviation() : '-' );
		flags.append((f != null && f.getFieldType().isStoreTermVectorPositions()) 
				? FieldFlag.TERM_VECTOR_POSITION.getAbbreviation() : '-' );
		
		flags.append((f != null && f.getFieldType().isOmitNorms()) 
				? FieldFlag.OMIT_NORMS.getAbbreviation() : '-' );
		flags.append( (f != null && IndexOptions.DOCS_ONLY == opts ) 
				? FieldFlag.OMIT_TF.getAbbreviation() : '-' );
		flags.append((f != null && IndexOptions.DOCS_AND_FREQS == opts) 
				? FieldFlag.OMIT_POSITIONS.getAbbreviation() : '-');

		flags.append((f != null && f.getClass().getSimpleName().equals("LazyField")) 
				? FieldFlag.LAZY.getAbbreviation() : '-' );
		flags.append((f != null && f.getBinaryValue() != null) 
				? FieldFlag.BINARY.getAbbreviation() : '-' );
		
		flags.append((false) 
				? FieldFlag.SORT_MISSING_FIRST.getAbbreviation() : '-' ); // SchemaField Specific
		flags.append((false) 
				? FieldFlag.SORT_MISSING_LAST.getAbbreviation() : '-' ); // SchemaField Specific
		
		return flags.toString();
	}
	
	/**
	 * @return a string representing a SchemaField's flags.  
	 */
	static String getFieldFlags(SchemaField f) {
		SchemaFieldType t = (f == null) ? null : f.getType();

		// see: http://www.nabble.com/schema-field-properties-tf3437753.html#a9585549
		boolean lazy = false; // "lazy" is purely a property of reading fields
		boolean binary = false; // Currently not possible

		StringBuilder flags = new StringBuilder();
		
		flags.append((f != null && f.isIndexed()) 
				? FieldFlag.INDEXED.getAbbreviation() : '-' );
		flags.append((t != null && t.isTokenized()) 
				? FieldFlag.TOKENIZED.getAbbreviation() : '-' );
		flags.append((f != null && f.isStored()) 
				? FieldFlag.STORED.getAbbreviation() : '-' );
		flags.append((f != null && f.isMultiValued()) 
				? FieldFlag.MULTI_VALUED.getAbbreviation() : '-' );
		flags.append((f != null && f.isStoreTermVector()) 
				? FieldFlag.TERM_VECTOR_STORED.getAbbreviation() : '-' );
		flags.append((f != null && f.isStoreTermOffsets()) 
				? FieldFlag.TERM_VECTOR_OFFSET.getAbbreviation() : '-' );
		flags.append((f != null && f.isStoreTermPositions()) 
				? FieldFlag.TERM_VECTOR_POSITION.getAbbreviation() : '-' );
		flags.append((f != null && f.isOmitNorms()) 
				? FieldFlag.OMIT_NORMS.getAbbreviation() : '-' );
		flags.append((f != null && f.isOmitTermFreqAndPositions()) 
				? FieldFlag.OMIT_TF.getAbbreviation() : '-' );
		flags.append((f != null && f.isOmitPositions()) 
				? FieldFlag.OMIT_POSITIONS.getAbbreviation() : '-' );
		flags.append((lazy) 
				? FieldFlag.LAZY.getAbbreviation() : '-' );
		flags.append((binary) 
				? FieldFlag.BINARY.getAbbreviation() : '-' );
		flags.append((f != null && f.isSortMissingFirst()) 
				? FieldFlag.SORT_MISSING_FIRST.getAbbreviation() : '-' );
		flags.append((f != null && f.isSortMissingLast()) 
				? FieldFlag.SORT_MISSING_LAST.getAbbreviation() : '-' );
		
		return flags.toString();
	}
	
	/**
	 * @return a key to what each character means
	 */
	public static NamedMap<String> getFieldFlagsKey() {
		NamedMap<String> key = new NamedMap<String>();
		for (FieldFlag f : FieldFlag.values()) {
			key.add(String.valueOf(f.getAbbreviation()), f.getDisplayText());
		}
		return key;
	}
	
	public static NamedMap<Object> getIndexInfo(IDirectoryReader reader) throws ErrorException {
	    NamedMap<Object> indexInfo = new NamedMap<Object>();
	    
	    try {
		    if (reader != null) {
			    IDirectory dir = reader.getDirectory();
			    
			    indexInfo.add("numDocs", reader.getNumDocs());
			    indexInfo.add("maxDoc", reader.getMaxDoc());
			    indexInfo.add("deletedDocs", reader.getMaxDoc() - reader.getNumDocs());
		
			    // TODO? Is this different then: IndexReader.getCurrentVersion( dir )?
			    indexInfo.add("version", reader.getVersion()); 
			    indexInfo.add("segmentCount", reader.getReaderContext().getLeaves().size());
			    indexInfo.add("current", reader.isCurrent() );
			    indexInfo.add("hasDeletions", reader.hasDeletions());
			    indexInfo.add("directory", dir);
			    indexInfo.add("userData", reader.getIndexCommit().getUserData());
			    
			    String s = reader.getIndexCommit().getUserData().get(SearchWriter.COMMIT_TIME_MSEC_KEY);
			    if (s != null) 
			    	indexInfo.add("lastModified", new Date(Long.parseLong(s)));
		    }
	    } catch (Exception ex) { 
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    			ex.toString(), ex);
	    }
	    
	    return indexInfo;
	}
	
	/**
	 * Return info from the index
	 */
	static NamedMap<Object> getSchemaInfo(IndexSchema schema) throws ErrorException {
	    Map<String, List<String>> typeusemap = new TreeMap<String, List<String>>();
	    Map<String, Object> fields = new TreeMap<String, Object>();
	    
	    SchemaField uniqueField = schema.getUniqueKeyField();
	    for (SchemaField f : schema.getFields().values()) {
	    	populateFieldInfo(schema, typeusemap, fields, uniqueField, f);
	    }

	    Map<String, Object> dynamicFields = new TreeMap<String, Object>();
	    for (SchemaField f : schema.getDynamicFieldPrototypes()) {
	    	populateFieldInfo(schema, typeusemap, dynamicFields, uniqueField, f);
	    }
	    
	    NamedMap<Object> types = new NamedMap<Object>();
	    Map<String, SchemaFieldType> sortedTypes = 
	    		new TreeMap<String, SchemaFieldType>(schema.getFieldTypes());
	    
	    for (SchemaFieldType ft : sortedTypes.values()) {
	    	NamedMap<Object> field = new NamedMap<Object>();
	    	field.add("fields", typeusemap.get(ft.getTypeName()));
	    	field.add("tokenized", ft.isTokenized());
	    	field.add("className", ft.getClass().getName());
	    	field.add("indexAnalyzer", getAnalyzerInfo(ft.getAnalyzer()));
	    	field.add("queryAnalyzer", getAnalyzerInfo(ft.getQueryAnalyzer()));
	    	field.add("similarity", getSimilarityInfo(ft.getSimilarity()));
	    	
	    	types.add(ft.getTypeName(), field);
	    }

	    // Must go through this to maintain binary compatbility. 
	    // Putting a TreeMap into a resp leads to casting errors
	    NamedMap<Object> finfo = new NamedMap<Object>();

	    NamedMap<Object> fieldsSimple = new NamedMap<Object>();
	    for (Map.Entry<String, Object> ent : fields.entrySet()) {
	    	fieldsSimple.add(ent.getKey(), ent.getValue());
	    }
	    finfo.add("fields", fieldsSimple);

	    NamedMap<Object> dynamicSimple = new NamedMap<Object>();
	    for (Map.Entry<String, Object> ent : dynamicFields.entrySet()) {
	    	dynamicSimple.add(ent.getKey(), ent.getValue());
	    }
	    finfo.add("dynamicFields", dynamicSimple);

	    finfo.add("uniqueKeyField", (uniqueField == null) ? null : uniqueField.getName());
	    finfo.add("defaultSearchField", schema.getDefaultSearchFieldName());
	    finfo.add("types", types);
	    
	    return finfo;
	}
	
	static NamedMap<Object> getSimilarityInfo(ISimilarity similarity) {
		NamedMap<Object> toReturn = new NamedMap<Object>();
	    if (similarity != null) {
	    	toReturn.add("className", similarity.getClass().getName());
	    	toReturn.add("details", similarity.toString());
	    }
	    return toReturn;
	}
	
	static NamedMap<Object> getAnalyzerInfo(IAnalyzer analyzer) {
	    NamedMap<Object> aninfo = new NamedMap<Object>();
	    aninfo.add("className", analyzer.getClass().getName());
	    
	    if (analyzer instanceof TokenizerChain) {
	    	TokenizerChain tchain = (TokenizerChain)analyzer;

	    	CharFilterFactory[] cfiltfacs = tchain.getCharFilterFactories();
	    	NamedMap<Map<String, Object>> cfilters = 
	    			new NamedMap<Map<String, Object>>();
	    	
	    	for (CharFilterFactory cfiltfac : cfiltfacs) {
	    		Map<String, Object> tok = new HashMap<String, Object>();
	    		String className = cfiltfac.getClass().getName();
	    		tok.put("className", className);
	    		tok.put("args", cfiltfac.getArgs());
	    		
	    		cfilters.add(className.substring(
	    				className.lastIndexOf('.')+1), tok);
	    	}
	    	
	    	if (cfilters.size() > 0) 
	    		aninfo.add("charFilters", cfilters);

	    	NamedMap<Object> tokenizer = new NamedMap<Object>();
	    	TokenizerFactory tfac = tchain.getTokenizerFactory();
	    	tokenizer.add("className", tfac.getClass().getName());
	    	tokenizer.add("args", tfac.getArgs());
	    	aninfo.add("tokenizer", tokenizer);

	    	TokenFilterFactory[] filtfacs = tchain.getTokenFilterFactories();
	    	NamedMap<Map<String, Object>> filters = 
	    			new NamedMap<Map<String, Object>>();
	    	
	    	for (TokenFilterFactory filtfac : filtfacs) {
	    		Map<String, Object> tok = new HashMap<String, Object>();
	    		String className = filtfac.getClass().getName();
	    		tok.put("className", className);
	    		tok.put("args", filtfac.getArgs());
	    		
	    		filters.add(className.substring(
	    				className.lastIndexOf('.')+1), tok);
	    	}
	    	
	    	if (filters.size() > 0) 
	    		aninfo.add("filters", filters);
	    }
	    
	    return aninfo;
	}
	
	static void populateFieldInfo(IndexSchema schema,
              Map<String, List<String>> typeuseMap, Map<String, Object> fields,
              SchemaField uniqueField, SchemaField f) throws ErrorException {
		try {
			SchemaFieldType ft = f.getType();
			NamedMap<Object> field = new NamedMap<Object>();
			
			field.add("type", ft.getTypeName());
			field.add("flags", getFieldFlags(f));
			
			if (f.isRequired()) 
				field.add("required", f.isRequired());
			
			if (f.getDefaultValue() != null) 
				field.add("default", f.getDefaultValue());
			
			if (f == uniqueField) 
				field.add("uniqueKey", true);
			
			if (ft.getAnalyzer().getPositionIncrementGap(f.getName()) != 0) {
				field.add("positionIncrementGap", 
						ft.getAnalyzer().getPositionIncrementGap(f.getName()));
			}
			
			field.add("copyDests", toListOfStringDests(schema.getCopyFieldsList(f.getName())));
			field.add("copySources", toListOfStrings(schema.getCopySources(f.getName())));
	
			fields.put(f.getName(), field);
	
			List<String> v = typeuseMap.get(ft.getTypeName());
			if (v == null) 
				v = new ArrayList<String>();
			
			v.add(f.getName());
			typeuseMap.put(ft.getTypeName(), v);
			
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	static List<String> toListOfStrings(SchemaField[] raw) {
	    List<String> result = new ArrayList<String>(raw.length);
	    for (SchemaField f : raw) {
	    	result.add(f.getName());
	    }
	    return result;
	}
	
	static List<String> toListOfStringDests(List<CopyField> raw) {
	    List<String> result = new ArrayList<String>(raw.size());
	    for (CopyField f : raw) {
	    	result.add(f.getDestination().getName());
	    }
	    return result;
	}
	
	static NamedMap<Object> getIndexedFieldsInfo(ISearchRequest req) 
			throws ErrorException {
	    Searcher searcher = req.getSearcher();
	    Params params = req.getParams();

	    Set<String> fields = null;
	    String fl = params.get(CommonParams.FL);
	    if (fl != null) 
	    	fields = new TreeSet<String>(Arrays.asList(fl.split( "[,\\s]+" )));

	    IAtomicReader reader = searcher.getAtomicReader();
	    IndexSchema schema = searcher.getSchema();

	    // Don't be tempted to put this in the loop below, the whole point here 
	    // is to alphabetize the fields!
	    Set<String> fieldNames = new TreeSet<String>();
	    for (IFieldInfo fieldInfo : reader.getFieldInfos()) {
	    	fieldNames.add(fieldInfo.getName());
	    }

	    // Walk the term enum and keep a priority queue for each map in our set
	    NamedMap<Object> finfo = new NamedMap<Object>();

	    for (String fieldName : fieldNames) {
	    	if (fields != null && ! fields.contains(fieldName) && ! fields.contains("*")) {
	    		continue; //we're not interested in this field Still an issue here
	    	}

	    	NamedMap<Object> fieldMap = new NamedMap<Object>();

	    	SchemaField sfield = schema.getFieldOrNull(fieldName);
	    	SchemaFieldType ftype = (sfield == null) ? null : sfield.getType();

	    	fieldMap.add("type", (ftype == null) ? null : ftype.getTypeName());
	    	fieldMap.add("schema", getFieldFlags(sfield));
	    	
	    	if (sfield != null && schema.isDynamicField(sfield.getName()) && 
	    		schema.getDynamicPattern(sfield.getName()) != null) {
	    		fieldMap.add("dynamicBase", schema.getDynamicPattern(sfield.getName()));
	    	}
	    	
	    	try {
		    	ITerms terms = reader.getFields().getTerms(fieldName);
		    	if (terms == null) { 
		    		// Not indexed, so we need to report what we can (it made it 
		    		// through the fl param if specified)
		    		finfo.add(fieldName, fieldMap);
		    		continue;
		    	}
	
		    	if (sfield != null && sfield.isIndexed()) {
		    		// In the pre-4.0 days, this did a veeeery expensive range query. 
		    		// But we can be much faster now, so just do this all the time.
		    		IDocument doc = getFirstLiveDoc(terms, reader);
	
		    		if (doc != null) {
		    			// Found a document with this field
	    				Fieldable fld = (Fieldable)doc.getField(fieldName);
	    				if (fld != null) {
	    					fieldMap.add("index", getFieldFlags(fld));
	    				} else {
	    					// it is a non-stored field...
	    					fieldMap.add("index", "(unstored field)");
	    				}
		    		}
		    		
		    		fieldMap.add("docs", terms.getDocCount());
		    	}
		    	
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    	}
	    	
	    	if (fields != null && (fields.contains(fieldName) || fields.contains("*"))) 
	    		getDetailedFieldInfo(req, fieldName, fieldMap);
	    	
	    	// Add the field
	    	finfo.add(fieldName, fieldMap);
	    }
	    
	    return finfo;
	}
	
	static NamedMap<Object> getDocumentFieldsInfo(IDocument doc, int docId, 
			IIndexReader reader, IndexSchema schema) throws ErrorException { 
	    final CharsRef spare = new CharsRef();
	    NamedMap<Object> finfo = new NamedMap<Object>();
	    
	    for (IField ifield : doc.getFields()) {
	    	Fieldable field = (Fieldable)ifield;
	    	NamedMap<Object> f = new NamedMap<Object>();

	    	SchemaField sfield = schema.getFieldOrNull(field.getName());
	    	SchemaFieldType ftype = (sfield == null) ? null : sfield.getType();

	    	try { 
		    	f.add("type", (ftype == null) ? null : ftype.getTypeName());
		    	f.add("schema", getFieldFlags(sfield));
		    	f.add("flags", getFieldFlags(field));
	
		    	Term t = new Term(field.getName(), (ftype != null) ? 
		    			ftype.storedToIndexed(field) : field.getStringValue());
	
		    	f.add("value", (ftype == null) ? null : ftype.toExternal(field));
	
		    	// TODO: this really should be "stored"
		    	f.add("internal", field.getStringValue());  // may be a binary number
	
		    	BytesRef bytes = field.getBinaryValue();
		    	if (bytes != null) {
		    		f.add("binary", Base64Utils.byteArrayToBase64(
		    				bytes.getBytes(), bytes.getOffset(), bytes.getLength()));
		    	}
		    	
		    	f.add("boost", field.getBoost());
		    	// this can be 0 for non-indexed fields
		    	f.add("docFreq", t.getText() == null ? 0 : reader.getDocFreq(t)); 
	
		    	// If we have a term vector, return that
		    	if (field.getFieldType().isStoreTermVectors()) {
		    		ITerms v = reader.getTermVector(docId, field.getName());
		    		if (v != null) {
		    			NamedMap<Integer> tfv = new NamedMap<Integer>();
		    			final ITermsEnum termsEnum = v.iterator(null);
		    			
		    			BytesRef text;
		    			while((text = termsEnum.next()) != null) {
		    				final int freq = (int) termsEnum.getTotalTermFreq();
		    				UnicodeUtil.UTF8toUTF16(text, spare);
		    				tfv.add(spare.toString(), freq);
		    			}
		    			
		    			f.add("termVector", tfv);
		    		}
		    	}
		    	
	    	} catch (IOException ex) { 
	    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	    	}
	    	
	    	finfo.add(field.getName(), f);
	    }
	    
	    return finfo;
	}
	
	// Just get a document with the term in it, the first one will do!
	// Is there a better way to do this? Shouldn't actually be very costly
	// to do it this way.
	static IDocument getFirstLiveDoc(ITerms terms, IAtomicReader reader) 
			throws ErrorException {
		try {
			IDocsEnum docsEnum = null;
			ITermsEnum termsEnum = terms.iterator(null);
			BytesRef text;
			
			// Deal with the chance that the first bunch of terms are in deleted documents. 
			// Is there a better way?
			for (int idx = 0; idx < 1000 && docsEnum == null; ++idx) {
				text = termsEnum.next();
				if (text == null) { 
					// Ran off the end of the terms enum without finding 
					// any live docs with that field in them.
					return null;
				}
				
				docsEnum = termsEnum.getDocs(reader.getLiveDocs(), docsEnum, 0);
				
				if (docsEnum.nextDoc() != IDocIdSetIterator.NO_MORE_DOCS) 
					return reader.getDocument(docsEnum.getDocID());
			}
		} catch (IOException ex) { 
			// TODO: something wrong, need to fix it
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			
			//if (LOG.isErrorEnabled()) 
			//	LOG.error("getFirstLiveDoc error: " + ex.toString(), ex);
		}
		
		return null;
	}
	
	// Get terribly detailed information about a particular field. 
	// This is a very expensive call, use it with caution
	// especially on large indexes!
	static void getDetailedFieldInfo(ISearchRequest req, String field, 
			NamedMap<Object> fieldMap) throws ErrorException {
		try {
	    	Params params = req.getParams();
	    	int numTerms = params.getInt(NUMTERMS, DEFAULT_COUNT);
	
	    	// Something to collect the top N terms in.
	    	TopTermQueue tiq = new TopTermQueue(numTerms + 1);
	    	final CharsRef spare = new CharsRef();
	
	    	IFields fields = MultiFields.getFields(req.getSearcher().getIndexReader());
	    	if (fields == null) // No indexed fields
	    		return;
	    
	    	ITerms terms = fields.getTerms(field);
	    	if (terms == null) // No terms in the field.
	    		return;
	    
	    	ITermsEnum termsEnum = terms.iterator(null);
	    	int[] buckets = new int[HIST_ARRAY_SIZE];
	    	BytesRef text;
	    	
	    	while ((text = termsEnum.next()) != null) {
	    		// This calculation seems odd, but it gives the same results as it used to.
	    		int freq = termsEnum.getDocFreq(); 
	    		int slot = 32 - Integer.numberOfLeadingZeros(Math.max(0, freq - 1));
	    		
	    		buckets[slot] = buckets[slot] + 1;
	    		
	    		if (freq > tiq.getMinFreq()) {
	    			UnicodeUtil.UTF8toUTF16(text, spare);
	    			String t = spare.toString();
	    			
	    			tiq.setDistinctTerms(new Long(terms.size()).intValue());
	    			tiq.add(new TopTermQueue.TermInfo(new Term(field, t), 
	    					termsEnum.getDocFreq()));
	    			
	    			if (tiq.size() > numTerms) { // if tiq full
	    				tiq.pop(); // remove lowest in tiq
	    				tiq.setMinFreq(tiq.getTopTermInfo().getDocFreq());
	    			}
	    		}
	    	}
	    	
	    	tiq.getHistogram().add(buckets);
	    	fieldMap.add("distinct", tiq.getDistinctTerms());
	
	    	// Include top terms
	    	fieldMap.add("topTerms", tiq.toNamedList(req.getSearcher().getSchema()));
	
	    	// Add a histogram
	    	fieldMap.add("histogram", tiq.getHistogram().toNamedList());
	    	
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
}
