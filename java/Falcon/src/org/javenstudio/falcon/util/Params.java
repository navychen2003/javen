package org.javenstudio.falcon.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;

public abstract class Params {

	/** returns the String value of a param, or null if not set */
	public abstract String get(String param) throws ErrorException;

	/** returns an array of the String values of a param, or null if none */
	public abstract String[] getParams(String param) throws ErrorException;

	/** returns an Iterator over the parameter names */
	public abstract Iterator<String> getParameterNamesIterator() throws ErrorException;

	/** returns the value of the param, or def if not set */
	public String get(String param, String def) throws ErrorException {
		String val = get(param);
		return val == null ? def : val;
	}
  
	/** returns a RequiredParams wrapping this */
	public RequiredParams toRequired() {
    	// TODO? should we want to stash a reference?
    	return new RequiredParams(this);
	}
  
	protected String fpname(String field, String param) {
		return "f." + field + '.' + param;
	}

	/** 
	 * returns the String value of the field parameter, "f.field.param", or
	 *  the value for "param" if that is not set.
	 */
	public String getFieldParam(String field, String param) throws ErrorException {
		String val = get(fpname(field, param));
		return val != null ? val : get(param);
	}

	/** 
	 * returns the String value of the field parameter, "f.field.param", or
	 *  the value for "param" if that is not set.  If that is not set, def
	 */
	public String getFieldParam(String field, String param, String def) throws ErrorException {
		String val = get(fpname(field, param));
		return val != null ? val : get(param, def);
	}
  
	/** 
	 * returns the String values of the field parameter, "f.field.param", or
	 *  the values for "param" if that is not set.
	 */
	public String[] getFieldParams(String field, String param) throws ErrorException {
		String[] val = getParams(fpname(field, param));
		return val != null ? val : getParams(param);
	}

	/** Returns the Boolean value of the param, or null if not set */
	public Boolean getBool(String param) throws ErrorException {
		String val = get(param);
		return val == null ? null : StrHelper.parseBool(val);
	}

	/** Returns the boolean value of the param, or def if not set */
	public boolean getBool(String param, boolean def) throws ErrorException {
		String val = get(param);
		return val == null ? def : StrHelper.parseBool(val);
	}
  
	/** 
	 * Returns the Boolean value of the field param, 
     * or the value for param, or null if neither is set. 
     */
	public Boolean getFieldBool(String field, String param) throws ErrorException {
		String val = getFieldParam(field, param);
		return val == null ? null : StrHelper.parseBool(val);
	}
  
	/** 
	 * Returns the boolean value of the field param, 
  	 * or the value for param, or def if neither is set. 
  	 */
	public boolean getFieldBool(String field, String param, boolean def) throws ErrorException {
		String val = getFieldParam(field, param);
		return val == null ? def : StrHelper.parseBool(val);
	}

	/** Returns the Integer value of the param, or null if not set */
	public Integer getInt(String param) throws ErrorException {
		String val = get(param);
		try {
			return val == null ? null : Integer.valueOf(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** Returns the int value of the param, or def if not set */
	public int getInt(String param, int def) throws ErrorException {
		String val = get(param);
		try {
			return val == null ? def : Integer.parseInt(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}
  
	/**
	 * @return The int value of the field param, or the value for param 
	 * or <code>null</code> if neither is set. 
	 */
	public Integer getFieldInt(String field, String param) throws ErrorException {
		String val = getFieldParam(field, param);
		try {
			return val == null ? null : Integer.valueOf(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}
  
	/** 
	 * Returns the int value of the field param, 
  	 * or the value for param, or def if neither is set. 
  	 */
	public int getFieldInt(String field, String param, int def) throws ErrorException {
		String val = getFieldParam(field, param);
		try {
			return val == null ? def : Integer.parseInt(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** Returns the Float value of the param, or null if not set */
	public Float getFloat(String param) throws ErrorException {
		String val = get(param);
		try {
			return val == null ? null : Float.valueOf(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** Returns the float value of the param, or def if not set */
	public float getFloat(String param, float def) throws ErrorException {
		String val = get(param);
		try {
			return val == null ? def : Float.parseFloat(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** Returns the Float value of the param, or null if not set */
	public Double getDouble(String param) throws ErrorException {
		String val = get(param);
		try {
			return val == null ? null : Double.valueOf(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** Returns the float value of the param, or def if not set */
	public double getDouble(String param, double def) throws ErrorException {
		String val = get(param);
		try {
			return val == null ? def : Double.parseDouble(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** Returns the float value of the field param. */
	public Float getFieldFloat(String field, String param) throws ErrorException {
		String val = getFieldParam(field, param);
		try {
			return val == null ? null : Float.valueOf(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** 
	 * Returns the float value of the field param,
  	 * or the value for param, or def if neither is set. 
  	 */
	public float getFieldFloat(String field, String param, float def) throws ErrorException {
		String val = getFieldParam(field, param);
		try {
			return val == null ? def : Float.parseFloat(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** Returns the float value of the field param. */
	public Double getFieldDouble(String field, String param) throws ErrorException {
		String val = getFieldParam(field, param);
		try {
			return val == null ? null : Double.valueOf(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	/** 
	 * Returns the float value of the field param,
  	 * or the value for param, or def if neither is set. 
  	 */
	public double getFieldDouble(String field, String param, double def) throws ErrorException {
		String val = getFieldParam(field, param);
		try {
			return val == null ? def : Double.parseDouble(val);
		} catch (Throwable ex) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					ex.getMessage(), ex);
		}
	}

	@SuppressWarnings({"deprecation"})
	public static Params wrapDefaults(Params params, Params defaults) {
		if (params == null)
			return defaults;
		if (defaults == null)
			return params;
		return new DefaultParams(params,defaults);
	}

	@SuppressWarnings({"deprecation"})
	public static Params wrapAppended(Params params, Params defaults) {
		if (params == null)
			return defaults;
		if (defaults == null)
			return params;
		return new AppendedParams(params,defaults);
	}

	/** Create a Map&lt;String,String&gt; from a NamedList given no keys are repeated */
	public static Map<String,String> toMap(NamedList<?> params) {
		HashMap<String,String> map = new HashMap<String,String>();
		for (int i=0; i < params.size(); i++) {
			map.put(params.getName(i), params.getVal(i).toString());
		}
		return map;
	}

	/** Create a Map&lt;String,String[]&gt; from a NamedList */
	public static Map<String,String[]> toMultiMap(NamedList<?> params) {
		HashMap<String,String[]> map = new HashMap<String,String[]>();
		for (int i=0; i < params.size(); i++) {
			String name = params.getName(i);
			String val = params.getVal(i).toString();
			MultiMapParams.addParam(name,val,map);
		}
		return map;
	}

	/** Create Params from NamedList. */
	public static Params toParams(NamedList<?> params) {
		// if no keys are repeated use the faster MapParams
		HashMap<String,String> map = new HashMap<String,String>();
		for (int i=0; params != null && i < params.size(); i++) {
			String prev = map.put(params.getName(i), params.getVal(i).toString());
			if (prev != null) 
				return new MultiMapParams(toMultiMap(params));
		}
		return new MapParams(map);
	}
  
	/** Convert this to a NamedList */
	public NamedList<Object> toNamedList() throws ErrorException {
		final NamedMap<Object> result = new NamedMap<Object>();
    
		for (Iterator<String> it = getParameterNamesIterator(); it.hasNext();) {
			final String name = it.next();
			final String[] values = getParams(name);
			if (values.length == 1) {
				result.add(name,values[0]);
			} else {
				// currently no reason not to use the same array
				result.add(name,values);
			}
		}
		
		return result;
	}
	
	public String toDisplayString() { 
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		
		try { 
			int count = 0;
			for (Iterator<String> it = getParameterNamesIterator(); it.hasNext();) {
				final String name = it.next();
				final String[] values = getParams(name);
				if (count++ > 0) sb.append(", ");
				if (values.length == 1) {
					sb.append(name);
					sb.append("=\"");
					sb.append(values[0]);
					sb.append("\"");
				} else {
					sb.append(name);
					sb.append("={");
					for (int i=0; i < values.length; i++) { 
						if (i > 0) sb.append(", ");
						sb.append("\"");
						sb.append(values[i]);
						sb.append("\"");
					}
					sb.append("}");
				}
			}
		} catch (Throwable ex) { 
			// ignore
		}
		
		sb.append("}");
		return sb.toString();
	}
	
}
