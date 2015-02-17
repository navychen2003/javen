package org.javenstudio.common.parser.util;

public interface Response {

	public interface Headers { 
		public String getCommand(); 
		public String[] getHeaderNames(); 
		public Header getHeader(String name); 
	}
	
	public interface Header { 
		public String getHeaderName(); 
		public String getHeaderValue(); 
	}
	
	public interface ResultSet { 
		public String getType(); 
		public int getCount(); 
		public int getPositionFrom(); 
		public int getPositionTo(); 
		public Result getResult(int position); 
	}
	
	public interface Result { 
		public int getPosition(); 
		public String[] getFieldNames(); 
		public Field getField(String name); 
		public String getFieldValue(String name); 
	}
	
	public interface Field { 
		public String getFieldName(); 
		public String getFieldValue(); 
	}
	
	public String getServiceName(); 
	public String getServiceVersion(); 
	public String getCharacterEncoding(); 
	public Headers getHeaders(); 
	public ResultSet getResultSet(); 
	
}
