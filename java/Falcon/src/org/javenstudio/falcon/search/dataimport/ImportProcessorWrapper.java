package org.javenstudio.falcon.search.dataimport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

/**
 * A Wrapper over {@link ImportProcessor} instance which performs transforms 
 * and handles multi-row outputs correctly.
 *
 * @since 1.4
 */
public class ImportProcessorWrapper extends ImportProcessor {
	static final Logger LOG = Logger.getLogger(ImportProcessorWrapper.class);
	
	private List<ImportTransformer> mTransformers;
	private List<ImportRow> mRowCache;
	
	private final ImportBuilder mDocBuilder;
	private final ImportProcessor mDelegate;
	
	private String mOnError;
	//private String mEntityName;
	
	public ImportProcessorWrapper(ImportBuilder docBuilder, 
			ImportProcessor delegate) throws ErrorException {
		mDelegate = delegate;
		mDocBuilder = docBuilder;
		mRowCache = null;
		
		if (docBuilder == null) 
			throw new NullPointerException("ImportBuilder is null");
		
		if (delegate == null) 
			throw new NullPointerException("ImportProcessor is null");
		
		init(); 
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("ImportProcessorWrapper: entityName=" + delegate.getEntityName() 
					+ " processor=" + delegate);
		}
	}
	
	private void init() throws ErrorException {
		//if (mEntityName == null) {
			//mOnError = getVariableResolver().replaceTokens(
			//		getContext().getAttribute(ImportContext.ON_ERROR));
			
			if (mOnError == null) 
				mOnError = ImportContext.ABORT;
			
		//	mEntityName = getContext().getEntityName();
		//}
	}

	public ImportContext getContext() { 
		return mDocBuilder.getContext(); 
	}
	
	@Override
	public String getEntityName() { 
		return mDelegate.getEntityName();
	}
	
	@Override
	public void init(ImportRequest req) throws ErrorException { 
    	mDelegate.init(req);
    }
	
	public VariableResolver getVariableResolver() { 
		return mDocBuilder.getVariableResolver(); 
	}
	
	@SuppressWarnings("unchecked")
	private void loadTransformers() throws ErrorException {
		String transClasses = getContext().getAttributes().getTransformerImplName();
		if (transClasses == null) {
			mTransformers = Collections.EMPTY_LIST;
			return;
		}

		mTransformers = new ArrayList<ImportTransformer>();
		String[] transArr = transClasses.split(",");
		
		for (String aTransArr : transArr) {
			String trans = aTransArr.trim();
			if (trans.startsWith("script:")) {
				String functionName = trans.substring("script:".length());
				
				ScriptTransformer scriptTransformer = new ScriptTransformer(
						getContext(), getEntityName(), functionName);
				
				mTransformers.add(scriptTransformer);
				continue;
			}
			
			try {
				Class<?> clazz = getContext().getSearchCore()
						.getContextLoader().findClass(trans, Class.class); 
				
				if (ImportTransformer.class.isAssignableFrom(clazz)) {
					mTransformers.add((ImportTransformer) clazz.newInstance());
					
				} else {
					Method meth = clazz.getMethod(ImportContext.TRANSFORM_ROW, Map.class);
					mTransformers.add(new ReflectionTransformer(meth, clazz, trans));
				}
				
			} catch (NoSuchMethodException e){
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Transformer: " + trans + " does not implement Transformer interface or " + 
						"does not have a transformRow(Map<String.Object> m) method", e);
				
			} catch (Throwable e) {
				if (e instanceof ErrorException) {
					throw (ErrorException)e; 
				} else {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							e.toString(), e);
				}
			}
		}
	}

	protected ImportRow getFromRowCache() {
		ImportRow r = mRowCache.remove(0);
		if (mRowCache.isEmpty())
			mRowCache = null;
		
		return r;
	}

	@SuppressWarnings("unchecked")
	protected ImportRow applyTransformer(ImportRow row) 
			throws ErrorException {
		if (row == null) return null;
		if (mTransformers == null)
			loadTransformers();
		if (mTransformers == Collections.EMPTY_LIST)
			return row;
		
		ImportRow transformedRow = row;
		List<ImportRow> rows = null;
		boolean stopTransform = checkStopTransform(row);
		
		VariableResolver resolver = getVariableResolver();
		
		for (ImportTransformer t : mTransformers) {
			if (stopTransform) break;
			try {
				if (rows != null) {
					List<ImportRow> tmpRows = new ArrayList<ImportRow>();
					for (ImportRow map : rows) {
						resolver.addNamespace(getEntityName(), map.toMap());
						
						Object o = t.transformRow(map);
						if (o == null)
							continue;
						
						if (o instanceof ImportRow) {
							ImportRow oMap = (ImportRow) o;
							stopTransform = checkStopTransform(oMap);
							tmpRows.add(oMap);
							
						} else if (o instanceof List) {
							tmpRows.addAll((List<ImportRow>) o);
							
						} else {
							if (LOG.isErrorEnabled()) {
								LOG.error("Transformer must return ImportRow or " 
										+ "a List<ImportRow>");
							}
						}
					}
					
					rows = tmpRows;
				} else {
					resolver.addNamespace(getEntityName(), transformedRow.toMap());
					Object o = t.transformRow(transformedRow);
					if (o == null)
						return null;
					
					if (o instanceof ImportRow) {
						ImportRow oMap = (ImportRow) o;
						stopTransform = checkStopTransform(oMap);
						transformedRow = oMap;
						
					} else if (o instanceof List) {
						rows = (List<ImportRow>) o;
						
					} else {
						if (LOG.isErrorEnabled()) {
							LOG.error("Transformer must return ImportRow or " 
									+ "a List<ImportRow>");
						}
					}
				}
			} catch (Throwable e) {
				if (LOG.isWarnEnabled())
					LOG.warn("transformer threw error", e);
				
				if (ImportContext.ABORT.equals(mOnError) || ImportContext.SKIP.equals(mOnError)) 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				
				// onError = continue
			}
		}
		
		if (rows == null) {
			return transformedRow;
			
		} else {
			mRowCache = rows;
			return getFromRowCache();
		}
	}

	private boolean checkStopTransform(ImportRow row) {
		return row.get("$stopTransform") != null && 
				Boolean.parseBoolean(row.get("$stopTransform").toString());
	}

	@Override
	public ImportRow nextRow() throws ErrorException {
		if (mRowCache != null) 
			return getFromRowCache();
		
		while (true) {
			ImportRow arow = null;
			try {
				arow = mDelegate.nextRow();
			} catch (Throwable e) {
				if (ImportContext.ABORT.equals(mOnError)) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
					
				} else {
					// SKIP is not really possible. If this calls the nextRow() again 
					// the Entityprocessor would be in an inconisttent state 
					if (LOG.isErrorEnabled())
						LOG.error("Exception in entity: "+ getEntityName(), e);
					
					return null;
				}
			}
			
			if (arow == null) 
				return null;
				
			arow = applyTransformer(arow);
			if (arow != null) {
				mDelegate.postTransform(arow);
				return arow;
			}
		}
	}

	@Override
	public ImportRow nextModifiedRow() throws ErrorException {
		ImportRow row = mDelegate.nextModifiedRow();
		row = applyTransformer(row);
		mRowCache = null;
		return row;
	}

	@Override
	public ImportRow nextDeletedRow() throws ErrorException {
		ImportRow row = mDelegate.nextDeletedRow();
		row = applyTransformer(row);
		mRowCache = null;
		return row;
	}

	//@Override
	//public ImportRow nextModifiedParentRow() throws ErrorException {
	//	return mDelegate.nextModifiedParentRow();
	//}

	@Override
	public void destroy() throws ErrorException {
		mDelegate.destroy();
	}

	@Override
	public void close() throws ErrorException {
		mDelegate.close();
	}
	
}
