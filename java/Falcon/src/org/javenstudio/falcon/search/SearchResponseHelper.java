package org.javenstudio.falcon.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.search.component.QueryData;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.transformer.DocTransformer;
import org.javenstudio.falcon.search.transformer.TransformContext;
import org.javenstudio.falcon.util.ResultItem;

public class SearchResponseHelper {
	static final Logger LOG = Logger.getLogger(SearchResponseHelper.class);

	public static boolean writeKnownType(SearchTextWriter writer, 
			String name, Object val) throws IOException { 
		if (val == null) {
			writer.getTextWriter().writeNull(name);
			return true;
			
		} else if (val instanceof Fieldable) {
			Fieldable f = (Fieldable)val;
			try {
				ISearchRequest req = writer.getSearchRequest();
				IndexSchema schema = req.getSearchCore().getSchema();
				
				SchemaField sf = schema.getFieldOrNull(f.getName());
				if (sf != null) {
					//if (LOG.isDebugEnabled())
					//	LOG.debug("writeKnownType: field=" + sf.getClass().getName());
					
					sf.getType().write(writer.getTextWriter(), name, f);
				} else {
					//if (LOG.isDebugEnabled())
					//	LOG.debug("writeKnownType: stringValue=" + f.getStringValue());
					
					writer.getTextWriter().writeString(name, f.getStringValue(), true);
				}
			} catch (Throwable e) { 
				if (e instanceof IOException)
					throw (IOException)e; 
				else
					throw new IOException(e);
			}
			
			return true;
			
		} else if (val instanceof IDirectory) { 
			writer.getTextWriter().writeString(name, val.toString(), true);
			return true;
			
		} else if (val instanceof IDocument) { 
			ResultItem doc = toResultItem(writer, (IDocument)val);
			
			SearchReturnFields returnFields = (SearchReturnFields)
					writer.getSearchResponse().getReturnFields();
			DocTransformer transformer = returnFields.getTransformer();
			
			if (transformer != null) { 
				TransformContext context = new TransformContext();
				context.setRequest((ISearchRequest)writer.getSearchRequest());
				
				try {
					transformer.setContext(context);
					transformer.transform(doc, -1);
				} catch (Throwable e) { 
					if (e instanceof IOException)
						throw (IOException)e; 
					else
						throw new IOException(e);
				}
			}
			
			writer.getTextWriter().writeResultItem(name, doc, 0);
			return true;
			
		} else if (val instanceof QueryData) { 
			writeDocuments(writer, name, (QueryData)val);
			return true;
			
		} else if (val instanceof DocList) { 
			// Should not happen normally
			QueryData data = new QueryData((DocList)val);
			writeDocuments(writer, name, data);
			return true;
		}
		
		return false;
	}
	
	static ResultItem toResultItem(SearchTextWriter writer, IDocument doc) 
			throws IOException {
		ResultItem out = new ResultItem();
		
		ISearchRequest req = writer.getSearchRequest();
		IndexSchema schema = req.getSearchCore().getSchema();
		
		for (IField f : doc) {
			// Make sure multivalued fields are represented as lists
			Object existing = out.get(f.getName());
			
			try {
				if (existing == null) {
					SchemaField sf = schema.getFieldOrNull(f.getName());
					if (sf != null && sf.isMultiValued()) {
						List<Object> vals = new ArrayList<Object>();
						vals.add(f);
						out.setField(f.getName(), vals);
					} else{
						out.setField(f.getName(), f);
					}
				} else {
					out.addField(f.getName(), f);
				}
				
			} catch (Throwable e) { 
				if (e instanceof IOException)
					throw (IOException)e; 
				else
					throw new IOException(e);
			}
		}
		
		return out;
	}
	
	static void writeDocuments(SearchTextWriter writer, String name, 
			QueryData res) throws IOException {
	    DocList ids = res.getDocList();
	    ISearchRequest req = writer.getSearchRequest();
	    
	    SearchReturnFields fields = (SearchReturnFields)
				writer.getSearchResponse().getReturnFields();
	    
	    TransformContext context = new TransformContext();
	    context.setQuery(res.getQuery());
	    context.setWantsScores(fields.wantsScore() && ids.hasScores());
	    context.setRequest(req);
	    
	    writer.getTextWriter().writeStartResultList(name, 
	    		ids.offset(), ids.size(), ids.matches(), 
	    		context.wantsScores() ? new Float(ids.maxScore()) : null);
	    
	    try {
		    DocTransformer transformer = fields.getTransformer();
		    context.setSearcher(req.getSearcher());
		    context.setDocIterator(ids.iterator());
		    
		    if (transformer != null) 
		    	transformer.setContext(context);
		    
		    int sz = ids.size();
		    Set<String> fnames = fields.getIndexFieldNames();
		    
		    for (int i=0; i < sz; i++) {
		    	int id = context.getDocIterator().nextDoc();
		    	IDocument doc = context.getSearcher().getDocument(id, fnames);
		    	
		    	ResultItem sdoc = toResultItem(writer, doc);
		    	if (transformer != null) 
		    		transformer.transform(sdoc, id);
		    	
		    	writer.getTextWriter().writeResultItem(null, sdoc, i);
		    }
		    
		    if (transformer != null) 
		    	transformer.setContext(null);
		    
	    } catch (Throwable e) { 
			if (e instanceof IOException)
				throw (IOException)e; 
			else
				throw new IOException(e);
		}
	    
	    writer.getTextWriter().writeEndResultList();
	}
	
}
