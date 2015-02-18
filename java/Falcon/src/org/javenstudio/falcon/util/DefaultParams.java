package org.javenstudio.falcon.util;

import java.util.Iterator;

import org.javenstudio.falcon.ErrorException;

public class DefaultParams extends Params {
	
	protected final Params mParams;
	protected final Params mDefaults;

	/**
	 * @deprecated (3.6) Use {@link Params#wrapDefaults(Params, Params)} instead.
	 */
	@Deprecated
	public DefaultParams(Params params, Params defaults) {
		assert params != null && defaults != null;
		mParams = params;
		mDefaults = defaults;
	}

	@Override
	public String get(String param) throws ErrorException {
		String val = mParams.get(param);
		return val != null ? val : mDefaults.get(param);
	}

	@Override
	public String[] getParams(String param) throws ErrorException {
		String[] vals = mParams.getParams(param);
		return vals != null ? vals : mDefaults.getParams(param);
	}

	@Override
	public Iterator<String> getParameterNamesIterator() throws ErrorException {
		final IteratorChain<String> c = new IteratorChain<String>();
		c.addIterator(mDefaults.getParameterNamesIterator());
		c.addIterator(mParams.getParameterNamesIterator());
		return c;
	}

	@Override
  	public String toString() {
		return "DefaultParams{params=" + mParams + ",defaults=" + mDefaults + "}";
	}
	
}
