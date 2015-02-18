package org.javenstudio.falcon.util;

import org.javenstudio.falcon.ErrorException;

/**
 * Params wrapper which acts similar to DefaultParams except that
 * it "appends" the values of multi-value params from both sub instances, so
 * that all of the values are returned. 
 */
public class AppendedParams extends DefaultParams {

	/**
	 * @deprecated (3.6) Use {@link Params#wrapAppended(Params, Params)} instead.
	 */
	@Deprecated
	public AppendedParams(Params main, Params extra) {
		super(main, extra);
	}

	@Override
	public String[] getParams(String param) throws ErrorException {
		String[] main = mParams.getParams(param);
		String[] extra = mDefaults.getParams(param);
		
		if (null == extra || 0 == extra.length) 
			return main;
		
		if (null == main || 0 == main.length) 
			return extra;
		
		String[] result = new String[main.length + extra.length];
		System.arraycopy(main, 0, result, 0, main.length);
		System.arraycopy(extra, 0, result, main.length, extra.length);
		
		return result;
	}

	@Override
	public String toString() {
		return "AppendedParams{main=" + mParams + ",extra=" + mDefaults + "}";
	}
	
}
