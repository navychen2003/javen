package org.javenstudio.falcon.search.cache;

import org.javenstudio.falcon.ErrorException;

/**
 * Decides how many things to autowarm based on the size of another cache
 */
public class AutoWarmCountRef {

    private final int mAutoWarmCount;
    private final int mAutoWarmPercentage;
    
    private final boolean mAutoWarmByPercentage;
    private final boolean mDoAutoWarming;
    
    private final String mStrVal;
    
    public AutoWarmCountRef(final String configValue) throws ErrorException {
    	try {
    		String input = (null == configValue) ? "0" : configValue.trim();

    		// odd undocumented legacy behavior, -1 meant "all" (now "100%")
    		mStrVal = ("-1".equals(input)) ? "100%" : input;

    		if (mStrVal.indexOf("%") == (mStrVal.length() - 1)) {
    			mAutoWarmCount = 0;
    			mAutoWarmPercentage = Integer.parseInt(mStrVal.substring(0, mStrVal.length() - 1));
    			mAutoWarmByPercentage = true;
    			mDoAutoWarming = (0 < mAutoWarmPercentage);
    			
    		} else {
    			mAutoWarmCount = Integer.parseInt(mStrVal);
    			mAutoWarmPercentage = 0;
    			mAutoWarmByPercentage = false;
    			mDoAutoWarming = (0 < mAutoWarmCount);
    		}
    	} catch (Exception e) {
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
    				"Can't parse autoWarm value: " + configValue, e);
    	}
	}
    
    public boolean isAutoWarmingOn() {
    	return mDoAutoWarming;
    }
    
    public int getWarmCount(final int previousCacheSize) {
    	return mAutoWarmByPercentage ? 
    			(previousCacheSize * mAutoWarmPercentage)/100 :
    				Math.min(previousCacheSize, mAutoWarmCount);
    }
    
    @Override
    public String toString() {
    	return mStrVal;
    }
    
}
