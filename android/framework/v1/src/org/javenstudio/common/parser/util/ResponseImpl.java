package org.javenstudio.common.parser.util;

import java.util.HashMap;
import java.util.Map;

public final class ResponseImpl extends Node implements Response {
	public final static String ROOTNAME = "response"; 

	public static class HeadersImpl extends Node implements Response.Headers { 
		public final static String NAME = "headers"; 
		
		private final Map<String, HeaderImpl> mHeaders; 
		
		HeadersImpl(Node parent) { 
			super(parent, NAME); 
			mHeaders = new HashMap<String, HeaderImpl>(); 
		}
		
		@Override 
		public synchronized Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			if (name.equals(HeaderImpl.NAME)) 
				return new HeaderImpl(this); 
			
			return null; 
		}
		
		public String getCommand() { return getAttribute("command"); } 
		public String[] getHeaderNames() { return mHeaders.keySet().toArray(new String[0]); } 
		public Response.Header getHeader(String name) { return mHeaders.get(name); } 
		
		protected void addHeader(String name, HeaderImpl header) { 
			if (name != null && header != null) 
				mHeaders.put(name, header); 
		}
		
		public String toString() { 
			StringBuilder sbuf = new StringBuilder();
			sbuf.append(" "+getClass().getSimpleName()+":command="+getCommand()); 
			sbuf.append("{\n"); 
			String[] names = getHeaderNames(); 
			for (int i=0; names != null && i < names.length; i++) { 
				String name = names[i]; 
				Response.Header header = getHeader(name); 
				if (i > 0) sbuf.append(",\n"); 
				sbuf.append("  "+header.toString());
			}
			sbuf.append("\n }"); 
			return sbuf.toString(); 
		}
	}
	
	public static class HeaderImpl extends Node implements Response.Header { 
		public final static String NAME = "header"; 
		
		HeaderImpl(Node parent) { 
			super(parent, NAME); 
		}
		
		public String getHeaderName() { return getAttribute("name"); } 
		public String getHeaderValue() { return getValue(); } 
		
		@Override 
		public void onNodeEnded() { 
			Node parent = getParent(); 
			if (parent != null && parent instanceof HeadersImpl) { 
				HeadersImpl headers = (HeadersImpl)parent; 
				headers.addHeader(getHeaderName(), this); 
			}
		}
		
		public String toString() { 
			return getClass().getSimpleName()+":"+getHeaderName()+"="+getHeaderValue(); 
		}
	}
	
	private static int toInt(String text) { 
		try { 
			if (text != null && text.length() > 0) 
				return Integer.valueOf(text).intValue(); 
		} catch (Exception e) { 
			// ignore
		}
		return 0; 
	}
	
	public static class ResultSetImpl extends Node implements Response.ResultSet { 
		public final static String NAME = "resultset"; 
		
		private final Map<Integer, Response.Result> mResults; 
		
		ResultSetImpl(Node parent) { 
			super(parent, NAME); 
			mResults = new HashMap<Integer, Response.Result>(); 
		}
		
		@Override 
		public synchronized Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			if (name.equals(ResultImpl.NAME)) 
				return new ResultImpl(this); 
			
			return null; 
		}
		
		public String getType() { return getAttribute("type"); } 
		public int getCount() { return toInt(getAttribute("count")); } 
		public int getPositionFrom() { return toInt(getAttribute("from")); } 
		public int getPositionTo() { return toInt(getAttribute("to")); } 
		
		public Response.Result getResult(int position) { 
			return mResults.get(position); 
		}
		
		protected void addResult(int position, ResultImpl result) { 
			if (result != null) 
				mResults.put(position, result); 
		}
		
		public String toString() { 
			StringBuilder sbuf = new StringBuilder();
			sbuf.append(" "+getClass().getSimpleName()+":type="+getType()+",count="+getCount()+",from="+getPositionFrom()+",to="+getPositionTo()); 
			sbuf.append("{\n"); 
			for (int i=getPositionFrom(); i < getPositionTo(); i++) { 
				Response.Result result = getResult(i); 
				if (i > getPositionFrom()) sbuf.append(",\n"); 
				sbuf.append("  "+(result != null ? result.toString() : "<null>")); 
			}
			sbuf.append("\n }"); 
			return sbuf.toString(); 
		}
	}
	
	public static class ResultImpl extends Node implements Response.Result { 
		public final static String NAME = "result"; 
		
		private final Map<String, Response.Field> mFields; 
		
		ResultImpl(Node parent) { 
			super(parent, NAME); 
			mFields = new HashMap<String, Response.Field>(); 
		}
		
		public int getPosition() { return toInt(getAttribute("index")); } 
		public String[] getFieldNames() { return mFields.keySet().toArray(new String[0]); } 
		public Response.Field getField(String name) { return mFields.get(name); } 
		
		public String getFieldValue(String name) { 
			Response.Field f = getField(name); 
			if (f != null) 
				return f.getFieldValue(); 
			else
				return null; 
		}
		
		protected void addField(String name, FieldImpl field) { 
			if (name != null && name.length() > 0 && field != null) 
				mFields.put(name, field); 
		}
		
		@Override 
		public void onNodeEnded() { 
			Node parent = getParent(); 
			if (parent != null && parent instanceof ResultSetImpl) { 
				ResultSetImpl resultSet = (ResultSetImpl)parent; 
				resultSet.addResult(getPosition(), this); 
			}
		}
		
		@Override 
		public synchronized Node newChildNode(String name) { 
			if (name == null || name.length() == 0) 
				return null; 
			
			if (name.equals(FieldImpl.NAME)) 
				return new FieldImpl(this); 
			
			return null; 
		}
		
		public String toString() { 
			StringBuilder sbuf = new StringBuilder();
			sbuf.append(""+getClass().getSimpleName()+":index="+getPosition()); 
			sbuf.append("{\n"); 
			String[] names = getFieldNames(); 
			for (int i=0; names != null && i < names.length; i++) { 
				String name = names[i]; 
				Response.Field field = getField(name); 
				if (i > 0) sbuf.append(",\n"); 
				sbuf.append("   "+field.toString());
			}
			sbuf.append("\n  }"); 
			return sbuf.toString(); 
		}
	}
	
	public static class FieldImpl extends Node implements Response.Field { 
		public final static String NAME = "field"; 
		
		FieldImpl(Node parent) { 
			super(parent, NAME); 
		}
		
		public String getFieldName() { return getAttribute("name"); } 
		public String getFieldValue() { return getValue(); } 
		
		@Override 
		public void onNodeEnded() { 
			Node parent = getParent(); 
			if (parent != null && parent instanceof ResultImpl) { 
				ResultImpl result = (ResultImpl)parent; 
				result.addField(getFieldName(), this); 
			}
		}
		
		public String toString() { 
			return getClass().getSimpleName()+":"+getFieldName()+"="+getFieldValue(); 
		}
	}
	
	private final HeadersImpl mHeaders; 
	private final ResultSetImpl mResultSet; 
	
	public ResponseImpl() { 
		super(null, ROOTNAME); 
		mHeaders = new HeadersImpl(this); 
		mResultSet = new ResultSetImpl(this); 
	}
	
	@Override 
	public synchronized Node newChildNode(String name) { 
		if (name == null || name.length() == 0) 
			return null; 
		
		if (name.equals(HeadersImpl.NAME)) 
			return mHeaders; 
		
		if (name.equals(ResultSetImpl.NAME)) 
			return mResultSet; 
		
		return null; 
	}
	
	public String getServiceName() { return getAttribute("service"); } 
	public String getServiceVersion() { return getAttribute("version"); } 
	public String getCharacterEncoding() { return "UTF-8"; } 
	public Response.Headers getHeaders() { return mHeaders; } 
	public Response.ResultSet getResultSet() { return mResultSet; } 
	
	public String toString() { 
		StringBuilder sbuf = new StringBuilder();
		sbuf.append(getClass().getSimpleName()+":service="+getServiceName()+",version="+getServiceVersion()+",encoding="+getCharacterEncoding()); 
		sbuf.append("{\n"); 
		sbuf.append(mHeaders.toString()); 
		sbuf.append(",\n"); 
		sbuf.append(mResultSet.toString()); 
		sbuf.append("\n}"); 
		return sbuf.toString(); 
	}
}
