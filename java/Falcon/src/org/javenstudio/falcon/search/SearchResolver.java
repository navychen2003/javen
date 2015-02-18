package org.javenstudio.falcon.search;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.component.QueryData;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.schema.type.BinaryFieldType;
import org.javenstudio.falcon.search.schema.type.BoolFieldType;
import org.javenstudio.falcon.search.schema.type.ByteFieldType;
import org.javenstudio.falcon.search.schema.type.DateFieldType;
import org.javenstudio.falcon.search.schema.type.DoubleFieldType;
import org.javenstudio.falcon.search.schema.type.FloatFieldType;
import org.javenstudio.falcon.search.schema.type.IntFieldType;
import org.javenstudio.falcon.search.schema.type.LongFieldType;
import org.javenstudio.falcon.search.schema.type.ShortFieldType;
import org.javenstudio.falcon.search.schema.type.StringFieldType;
import org.javenstudio.falcon.search.schema.type.TextFieldType;
import org.javenstudio.falcon.search.schema.type.TrieDateFieldType;
import org.javenstudio.falcon.search.schema.type.TrieDoubleFieldType;
import org.javenstudio.falcon.search.schema.type.TrieFloatFieldType;
import org.javenstudio.falcon.search.schema.type.TrieIntFieldType;
import org.javenstudio.falcon.search.schema.type.TrieLongFieldType;
import org.javenstudio.falcon.search.transformer.DocTransformer;
import org.javenstudio.falcon.search.transformer.TransformContext;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.JavaBinCodec;
import org.javenstudio.falcon.util.ResultItem;

public class SearchResolver implements JavaBinCodec.ObjectResolver {
	static final Logger LOG = Logger.getLogger(SearchResolver.class);

	/**
	 * TODO -- there may be a way to do this without marshal at all...
	 *
	 * @return a response object equivalent to what you get from 
	 * 		the XML/JSON/javabin parser. Documents become
	 *         ResultItem, DocList becomes ResultList etc.
	 *
	 * @since 1.4
	 */
	@SuppressWarnings("unchecked")
	public static NamedList<Object> getParsedResponse(ISearchRequest req, 
			ISearchResponse rsp) throws ErrorException {
		try {
			SearchResolver resolver = new SearchResolver(req, rsp.getReturnFields());

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			new JavaBinCodec(resolver).marshal(rsp.getValues(), out);

			InputStream in = new ByteArrayInputStream(out.toByteArray());
			return (NamedList<Object>) new JavaBinCodec(resolver).unmarshal(in);
			
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	
	private final ISearchRequest mRequest;
	private final SearchReturnFields mReturnFields;
	
	private final Searcher mSearcher;
	private final IndexSchema mSchema;
	
    // transmit field values using FieldType.toObject()
    // rather than the String from FieldType.toExternal()
    private boolean mUseFieldObjects = true;
	
	public SearchResolver(ISearchRequest req, SearchReturnFields fields) 
			throws ErrorException { 
		mRequest = req;
		mReturnFields = fields;
		mSearcher = mRequest.getSearcher();
		mSchema = mSearcher.getSchema(); 
	}
	
	@Override
	public Object resolve(Object o, JavaBinCodec codec) throws IOException { 
		if (o instanceof QueryData) {
			writeResults((QueryData) o, codec);
			return null; // null means we completely handled it
		}
		
        if (o instanceof DocList) {
        	QueryData ctx = new QueryData((DocList) o);
        	writeResults(ctx, codec);
        	return null; // null means we completely handled it
        }
        
        if (o instanceof Fieldable) {
        	Fieldable f = (Fieldable)o;
        	try {
        		SchemaField sf = mSchema.getFieldOrNull(f.getName());
        		o = getValue(sf, f);
        	} catch (Throwable e) {
        		if (LOG.isWarnEnabled())
        			LOG.warn("Error reading a field: " + o, e);
        	}
        }
        
        if (o instanceof ResultItem) {
        	// Remove any fields that were not requested.
        	// This typically happens when distributed search adds 
        	// extra fields to an internal request
        	ResultItem doc = (ResultItem)o;
        	Iterator<Map.Entry<String, Object>> it = doc.iterator();
        	
        	while (it.hasNext()) {
        		String fname = it.next().getKey();
        		if (!mReturnFields.wantsField(fname)) 
        			it.remove();
        	}
        	
        	return doc;
        }
        
        return o;
	}
	
    protected void writeResultsBody(QueryData res, JavaBinCodec codec) 
    		throws IOException {
    	DocList ids = res.getDocList();
    	
    	int sz = ids.size();
    	codec.writeTag(JavaBinCodec.ARR, sz);
    	
    	DocTransformer transformer = mReturnFields.getTransformer();
    	
    	TransformContext context = new TransformContext();
    	context.setQuery(res.getQuery());
    	context.setWantsScores(mReturnFields.wantsScore() && ids.hasScores());
    	context.setRequest(mRequest);
    	context.setSearcher(mSearcher);
    	
    	try {
	    	if (transformer != null) 
	    		transformer.setContext(context);
	      
	    	Set<String> fnames = mReturnFields.getIndexFieldNames();
	    	context.setDocIterator(ids.iterator());
	    	
	    	for (int i = 0; i < sz; i++) {
	    		int id = context.getDocIterator().nextDoc();
	    		IDocument doc = mSearcher.getDocument(id, fnames);
	    		
	    		ResultItem sdoc = getResultItem(doc);
	    		if (transformer != null) 
	    			transformer.transform(sdoc, id);
	    		
	    		codec.writeResultItem(sdoc);
	    	}
	    	
	    	if (transformer != null) 
	    		transformer.setContext(null);
	    	
    	} catch (ErrorException ex) { 
    		throw new IOException(ex.toString(), ex);
    	}
	}
    
    public void writeResults(QueryData ctx, JavaBinCodec codec) throws IOException {
    	codec.writeTag(JavaBinCodec.RESULTLIST);
    	boolean wantsScores = mReturnFields.wantsScore() && ctx.getDocList().hasScores();
    	
    	List<Number> list = new ArrayList<Number>(3);
    	list.add((long) ctx.getDocList().matches());
    	list.add((long) ctx.getDocList().offset());
      
    	Float maxScore = null;
    	if (wantsScores) 
    		maxScore = ctx.getDocList().maxScore();
      
    	list.add(maxScore);
    	codec.writeArray(list);
      
    	// this is a seprate function so that streaming responses can use just that part
    	writeResultsBody(ctx, codec);
    }

    public ResultItem getResultItem(IDocument doc) {
    	ResultItem resDoc = new ResultItem();
    	
    	for (IField field : doc) {
    		Fieldable f = (Fieldable)field;
    		String fieldName = f.getName();
    		
    		if (!mReturnFields.wantsField(fieldName)) 
    			continue;
        
    		Object val = null;
    		SchemaField sf = null;
    		try {
    			sf = mSchema.getFieldOrNull(fieldName);
    			val = getValue(sf, f);
    			
    		} catch (Throwable e) {
    			// There is a chance of the underlying field not really matching the
    			// actual field type . So ,it can throw exception
    			if (LOG.isWarnEnabled())
    				LOG.warn("Error reading a field from document: " + resDoc, e);
    			
    			//if it happens log it and continue
    			continue;
    		}
          
    		if (sf != null && sf.isMultiValued() && !resDoc.containsKey(fieldName)){
    			ArrayList<Object> lst = new ArrayList<Object>();
    			lst.add(val);
    			resDoc.addField(fieldName, lst);
    			
    		} else {
    			resDoc.addField(fieldName, val);
    		}
    	}
    	
    	return resDoc;
	}
    
    private Object getValue(SchemaField sf, Fieldable f) throws Exception {
    	SchemaFieldType ft = null;
    	if (sf != null) ft = sf.getType();
      
    	if (ft == null) {  // handle fields not in the schema
    		BytesRef bytesRef = f.getBinaryValue();
    		
	        if (bytesRef != null) {
	        	if (bytesRef.getOffset() == 0 && 
	        		bytesRef.getLength() == bytesRef.getBytes().length) {
	        		return bytesRef.getBytes();
	        		
	        	} else {
	        		final byte[] bytes = new byte[bytesRef.getLength()];
	        		System.arraycopy(bytesRef.getBytes(), bytesRef.getOffset(), 
	        				bytes, 0, bytesRef.getLength());
	        		return bytes;
	        	}
	        } else 
	        	return f.getStringValue();
	        
    	} else {
    		if (mUseFieldObjects && KNOWN_TYPES.contains(ft.getClass())) 
    			return ft.toObject(f);
    		else 
    			return ft.toExternal(f);
    	}
    }
    
    static final Set<Class<?>> KNOWN_TYPES = new HashSet<Class<?>>();
    
    static {
        KNOWN_TYPES.add(BoolFieldType.class);
        //KNOWN_TYPES.add(BCDIntFieldType.class);
        //KNOWN_TYPES.add(BCDLongFieldType.class);
        //KNOWN_TYPES.add(BCDStrFieldType.class);
        KNOWN_TYPES.add(ByteFieldType.class);
        KNOWN_TYPES.add(DateFieldType.class);
        KNOWN_TYPES.add(DoubleFieldType.class);
        KNOWN_TYPES.add(FloatFieldType.class);
        KNOWN_TYPES.add(ShortFieldType.class);
        KNOWN_TYPES.add(IntFieldType.class);
        KNOWN_TYPES.add(LongFieldType.class);
        //KNOWN_TYPES.add(SortableLongFieldType.class);
        //KNOWN_TYPES.add(SortableIntFieldType.class);
        //KNOWN_TYPES.add(SortableFloatFieldType.class);
        //KNOWN_TYPES.add(SortableDoubleFieldType.class);
        KNOWN_TYPES.add(StringFieldType.class);
        KNOWN_TYPES.add(TextFieldType.class);
        KNOWN_TYPES.add(TrieFieldType.class);
        KNOWN_TYPES.add(TrieIntFieldType.class);
        KNOWN_TYPES.add(TrieLongFieldType.class);
        KNOWN_TYPES.add(TrieFloatFieldType.class);
        KNOWN_TYPES.add(TrieDoubleFieldType.class);
        KNOWN_TYPES.add(TrieDateFieldType.class);
        KNOWN_TYPES.add(BinaryFieldType.class);
        
        // We do not add UUIDField because UUID object 
        // is not a supported type in JavaBinCodec
        // and if we write UUIDField.toObject, we wouldn't 
        // know how to handle it in the client side
	}
	
}
