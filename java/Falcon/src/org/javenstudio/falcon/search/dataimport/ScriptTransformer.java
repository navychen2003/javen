package org.javenstudio.falcon.search.dataimport;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.javenstudio.falcon.ErrorException;

/**
 * <p>
 * A {@link Transformer} instance capable of executing functions written in scripting
 * languages as a {@link Transformer} instance.
 * </p>
 * <p/>
 * <b>This API is experimental and may change in the future.</b>
 *
 * @since 1.3
 */
public class ScriptTransformer extends ImportTransformer {
	
	private final ImportContext mContext;
	private final Invocable mEngine;
	private final String mEntityName;
	private final String mFunctionName;
	
	public ScriptTransformer(ImportContext context, String entityName, 
			String methodName) throws ErrorException { 
		mContext = context;
		mEntityName = entityName;
		mFunctionName = methodName;
		mEngine = initEngine(context);
	}
	
	public String getEntityName() { return mEntityName; }
	public String getFunctionName() { return mFunctionName; }
	
	@Override
	public ImportRow transformRow(ImportRow row) throws ErrorException {
		try {
			if (mEngine == null)
				return row;
			
			return (ImportRow)mEngine.invokeFunction(mFunctionName, 
					new Object[]{row, mContext});
			
		} catch (Throwable e) {
			if (e instanceof ErrorException) {
				throw (ErrorException)e; 
			} else {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Error invoking script for entity " + getEntityName(), 
						e);
			}
		}
	}

	private Invocable initEngine(ImportContext context) throws ErrorException {
		Invocable engine = null;
		
		String scriptText = context.getScript();
		String scriptLang = context.getScriptLanguage();
		if (scriptText == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
					"<script> tag is not present under <dataConfig>");
		}
		
		ScriptEngineManager scriptEngineMgr = new ScriptEngineManager();
		ScriptEngine scriptEngine = scriptEngineMgr.getEngineByName(scriptLang);
		if (scriptEngine == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
					"Cannot load Script Engine for language: " + scriptLang);
		}
		
		if (scriptEngine instanceof Invocable) {
			engine = (Invocable) scriptEngine;
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
					"The installed ScriptEngine for: " + scriptLang
					+ " does not implement Invocable. Class is "
					+ scriptEngine.getClass().getName());
		}
		
		try {
			scriptEngine.eval(scriptText);
		} catch (ScriptException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"'eval' failed with language: " + scriptLang
					+ " and script: \n" + scriptText, e);
		}
		
		return engine;
	}

}
