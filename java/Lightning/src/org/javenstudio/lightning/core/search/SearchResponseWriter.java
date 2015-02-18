package org.javenstudio.lightning.core.search;

import java.io.IOException;
import java.io.Writer;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.SearchResponseHelper;
import org.javenstudio.falcon.search.SearchTextWriter;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.writer.JSONResponseWriter;
import org.javenstudio.lightning.response.writer.JSONWriter;
import org.javenstudio.lightning.response.writer.XMLResponseWriter;
import org.javenstudio.lightning.response.writer.XMLWriter;

public class SearchResponseWriter {
	static Logger LOG = Logger.getLogger(SearchResponseWriter.class);

	public static class XMLWriterImpl extends XMLResponseWriter { 
		@Override
		protected XMLWriter newXMLWriter(Writer writer, 
				Request req, Response rsp) throws ErrorException { 
			return new XMLWriter2(writer, req, rsp);
		}
	}
	
	public static class JSONWriterImpl extends JSONResponseWriter { 
		@Override
		protected JSONWriter newJSONWriter(Writer writer, 
				Request req, Response rsp) throws ErrorException { 
			return new JSONWriter2(writer, req, rsp);
		}
	}
	
	static class XMLWriter2 extends XMLWriter implements SearchTextWriter { 
		public XMLWriter2(Writer writer, Request req, Response rsp) 
	  			throws ErrorException {
			super(writer, req, rsp);
		}
		
		@Override
		public ISearchRequest getSearchRequest() { 
			return (ISearchRequest)getRequest(); 
		}
		
		@Override
		public ISearchResponse getSearchResponse() { 
			return (ISearchResponse)getResponse(); 
		}
		
		@Override
		public TextWriter getTextWriter() { 
			return this;
		}
		
		@Override
		protected boolean writeValKnown(String name, Object val) 
				throws IOException { 
			return SearchResponseHelper.writeKnownType(this, name, val);
		}
	}
	
	static class JSONWriter2 extends JSONWriter implements SearchTextWriter { 
		public JSONWriter2(Writer writer, Request req, Response rsp) 
	  			throws ErrorException {
			super(writer, req, rsp);
		}
		
		@Override
		public ISearchRequest getSearchRequest() { 
			return (ISearchRequest)getRequest(); 
		}
		
		@Override
		public ISearchResponse getSearchResponse() { 
			return (ISearchResponse)getResponse(); 
		}
		
		@Override
		public TextWriter getTextWriter() { 
			return this;
		}
		
		@Override
		protected boolean writeValKnown(String name, Object val) 
				throws IOException { 
			return SearchResponseHelper.writeKnownType(this, name, val);
		}
		
		@Override
		protected boolean isMultiValuedField(String fname) throws IOException { 
			try {
				// if multivalued field, write single value as an array
				ISearchRequest req = getSearchRequest();
				SchemaField sf = req.getSearchCore().getSchema().getFieldOrNull(fname);
				if (sf != null && sf.isMultiValued()) 
					return true; 
				else 
					return false;
				
			} catch (Throwable ex) { 
				throw new IOException("get schema field: " + fname + " error", ex);
			}
		}
	}
	
}
