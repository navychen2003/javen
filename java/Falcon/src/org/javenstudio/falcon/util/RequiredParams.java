package org.javenstudio.falcon.util;

import java.util.Iterator;

import org.javenstudio.falcon.ErrorException;

/**
 * This is a simple wrapper to Params that will throw a 400
 * exception if you ask for a parameter that does not exist.  Fields
 * specified with
 * 
 * In short, any value you for from a <code>RequiredParams</code> 
 * will return a valid non-null value or throw a 400 exception.  
 * (If you pass in <code>null</code> as the default value, you can 
 * get a null return value)
 * 
 *
 * @since 1.2
 */
public class RequiredParams extends Params {
  protected final Params params;
  
  public RequiredParams(Params params) {
    this.params = params;
  }

  /** get the param from params, fail if not found **/
  @Override
  public String get(String param) throws ErrorException {
    String val = params.get(param);
    if( val == null )  {
      throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST, "Missing required parameter: "+param );
    }
    return val;
  }
  
  @Override
  public String getFieldParam(final String field, final String param) throws ErrorException {
    final String fpname = fpname(field,param);
    String val = params.get(fpname);
    if (null == val) {
      // don't call this.get, we want a specified exception message
      val = params.get(param);
      if (null == val)  {
        throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST,
                                 "Missing required parameter: "+fpname+
                                 " (or default: "+param+")" );
      }
    }
    return val;
  }

  @Override
  public String[] getFieldParams(final String field, final String param) throws ErrorException {
    final String fpname = fpname(field,param);
    String[] val = params.getParams(fpname);
    if (null == val) {
      // don't call this.getParams, we want a specified exception message
      val = params.getParams(param);
      if (null == val)  {
        throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST,
                                 "Missing required parameter: "+fpname+
                                 " (or default: "+param+")" );
      }
    }
    return val;
  }

  
  @Override
  public String[] getParams(String param) throws ErrorException {
    String[] vals = params.getParams(param);
    if( vals == null || vals.length == 0 ) {
      throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST, "Missing required parameter: "+param );
    }
    return vals;
  }
  
  /** returns an Iterator over the parameter names */
  @Override
  public Iterator<String> getParameterNamesIterator() throws ErrorException {
    return params.getParameterNamesIterator();
  }

  @Override
  public String toString() {
    return "{required("+params+")}";  
  }    

  //----------------------------------------------------------
  // Functions with a default value - pass directly to the
  // wrapped Params (they won't return null - unless its the default)
  //----------------------------------------------------------

  @Override
  public String get(String param, String def) throws ErrorException {
    return params.get(param, def);
  }

  @Override
  public int getInt(String param, int def) throws ErrorException {
    return params.getInt(param, def);
  }

  @Override
  public float getFloat(String param, float def) throws ErrorException {
    return params.getFloat(param, def);
  }
  
  @Override
  public boolean getBool(String param, boolean def) throws ErrorException {
    return params.getBool(param, def);
  }

  @Override
  public int getFieldInt(String field, String param, int def) throws ErrorException {
    return params.getFieldInt(field, param, def);
  }
  
  @Override
  public boolean getFieldBool(String field, String param, boolean def) throws ErrorException {
    return params.getFieldBool(field, param, def);
  }

  @Override
  public float getFieldFloat(String field, String param, float def) throws ErrorException {
    return params.getFieldFloat(field, param, def);
  }

  @Override
  public String getFieldParam(String field, String param, String def) throws ErrorException {
    return params.getFieldParam(field, param, def);
  }
}
