package org.javenstudio.falcon.search.dataimport;

import java.lang.reflect.Method;

import org.javenstudio.falcon.ErrorException;

public class ReflectionTransformer extends ImportTransformer {
	
    private final Method mMethod;
    //private final Class<?> mClazz;
    //private final String mTransformer;
    private final Object mObject;

    public ReflectionTransformer(Method meth, Class<?> clazz, String trans)
    		throws ErrorException {
    	mMethod = meth;
    	//mClazz = clazz;
    	//mTransformer = trans;
    	
    	try {
    		mObject = clazz.newInstance();
    	} catch (Throwable e) {
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
    	}
    }

    @Override
    public ImportRow transformRow(ImportRow row) throws ErrorException {
    	try {
    		return (ImportRow)mMethod.invoke(mObject, row);
    	} catch (Throwable e) {
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
    	}
	}
    
}
