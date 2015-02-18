package org.javenstudio.falcon.search.dataimport;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * <p> {@link ImportBuilder} is responsible for creating documents out of 
 * the given configuration. It also maintains
 * statistics information. It depends on the {@link ImportProcessor} 
 * implementations to fetch data. </p>
 * <p/>
 * <b>This API is experimental and subject to change</b>
 *
 * @since 1.3
 */
public class ImportBuilder {
	static final Logger LOG = Logger.getLogger(ImportBuilder.class);

	static final SimpleDateFormat sFormater = 
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
	
	private final ImportStatistics mStatistics = new ImportStatistics();
	private final Map<String,Object> mStatusMessages = 
			Collections.synchronizedMap(new LinkedHashMap<String,Object>());
	
	private final AtomicBoolean mStop = new AtomicBoolean(false);
	
	private final Importer mImporter;
	private final ImportWriter mWriter;
	private final ImportRequest mRequest;
	private final VariableResolver mResolver;
	
	private boolean mCommited = false;
	
	public ImportBuilder(Importer importer, ImportWriter writer, 
			ImportRequest req) throws ErrorException { 
		mImporter = importer;
		mWriter = writer;
		mRequest = req;
		mResolver = loadVariableResolver(importer, req);
	}
	
	public Importer getImporter() { 
		if (mImporter == null) 
			throw new NullPointerException("Importer is null");
		
		return mImporter;
	}
	
	public ImportContext getContext() { 
		return getImporter().getContext();
	}
	
	public IndexSchema getSchema() { 
		return getContext().getSearchCore().getSchema();
	}
	
	public VariableResolver getVariableResolver() { 
		if (mResolver == null) 
			throw new NullPointerException("VariableResolver is null");
		
		return mResolver;
	}
	
	public ImportStatistics getStatistics() { 
		return mStatistics;
	}
	
	public ImportRequest getRequest() { 
		if (mRequest == null) 
			throw new NullPointerException("ImportRequest is null");
		
		return mRequest; 
	}
	
	public ImportWriter getWriter() { 
		if (mWriter == null) 
			throw new NullPointerException("ImportWriter is null");
		
		return mWriter;
	}
	
	public void addStatusMessage(String msg) { 
		addStatusMessage(msg, sFormater.format(new Date()));
	}
	
	public void addStatusMessage(String msg, Object val) { 
		mStatusMessages.put(msg, val);
	}
	
	public void removeStatusMessage(String msg) { 
		mStatusMessages.remove(msg);
	}
	
	public Map<String,Object> getStatusMessages() { 
		return mStatusMessages; 
	}
	
	public void execute() throws ErrorException { 
		ImportProcessorWrapper epw = null;
		
		try { 
			mCommited = false;
			getImporter().storeProps(ImportContext.STATUS_MSGS, mStatusMessages);
			
			final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
			addStatusMessage(ImportContext.TIME_ELAPSED, new Object() {
					@Override
					public String toString() {
						return Importer.getTimeElapsedSince(startTime.get());
					}
				});
			
			addStatusMessage(ImportContext.MSG.TOTAL_QUERIES_EXECUTED,
					getStatistics().getQueryCountRef());
			addStatusMessage(ImportContext.MSG.TOTAL_ROWS_EXECUTED,
					getStatistics().getRowsCountRef());
			addStatusMessage(ImportContext.MSG.TOTAL_DOC_PROCESSED,
					getStatistics().getDocCountRef());
			addStatusMessage(ImportContext.MSG.TOTAL_DOCS_SKIPPED,
					getStatistics().getSkipDocCountRef());
			
			AtomicBoolean fullCleanDone = new AtomicBoolean(false);
			epw = new ImportProcessorWrapper(this, 
					getContext().getProcessor(mRequest.getEntityName()));
			
			epw.init(mRequest);
			
			//Notification.getSystemNotifier().addNotification(
			//		Importer.toMessage(mRequest, this, "Indexing \"%1$s\" started."));
			
	        String delQuery = getContext().getAttributes().getPreImportDeleteQuery();
	        if (mImporter.getStatus() == Importer.Status.RUNNING_DELTA_DUMP) {
	        	cleanByQuery(delQuery, fullCleanDone);
	        	doDelta(epw);
	        	
	        	delQuery = getContext().getAttributes().getPostImportDeleteQuery();
	        	if (delQuery != null) {
	        		fullCleanDone.set(false);
	        		cleanByQuery(delQuery, fullCleanDone);
	        	}
	        	
	        } else {
	        	cleanByQuery(delQuery, fullCleanDone);
	        	doFullDump(epw);
	        	
	        	delQuery = getContext().getAttributes().getPostImportDeleteQuery();
	        	if (delQuery != null) {
	        		fullCleanDone.set(false);
	        		cleanByQuery(delQuery, fullCleanDone);
	        	}
	        }
	        
	        removeStatusMessage(ImportContext.MSG.TOTAL_DOC_PROCESSED);
			
	        if (mStop.get()) {
	            // Dont commit if aborted using command=abort
	        	addStatusMessage("Aborted");
	            rollback("Aborted");
	            
	        } else {
	            // Do not commit unnecessarily if this is a delta-import and no documents 
	        	// were created or deleted
	            if (!getRequest().isClean()) {
	            	if (getStatistics().getDocCount() > 0 || 
	            		getStatistics().getDeletedDocCount() > 0) {
	            		finish();
	            	}
	            } else {
	            	// Finished operation normally, commit now
	            	finish();
	            }
	          }

	          removeStatusMessage(ImportContext.TIME_ELAPSED);
	          addStatusMessage(ImportContext.MSG.TOTAL_DOC_PROCESSED, 
	        		  "" + getStatistics().getDocCount());
	          
	          if (getStatistics().getFailedDocCount() > 0) {
	        	  addStatusMessage(ImportContext.MSG.TOTAL_FAILED_DOCS, 
	        			  "" + getStatistics().getFailedDocCount());
	          }
	          
	          String timeTaken = Importer.getTimeElapsedSince(startTime.get());
	          addStatusMessage("Time taken", timeTaken);
	          
	          if (LOG.isDebugEnabled())
	        	  LOG.debug("Done. " + getStatusMessages());
	        
		} catch (Throwable ex) { 
			if (ex instanceof ErrorException) 
				throw (ErrorException)ex;
			else
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			
		} finally { 
			getWriter().close();
			
			if (epw != null) 
				epw.close();
		}
	}
	
	public void abort() { 
		if (LOG.isDebugEnabled())
			LOG.debug("abort");
		
		mStop.set(true); 
	}
	
	public boolean isAborted() { 
		return mStop.get(); 
	}
	
	public void rollback(String message) { 
		if (message == null)
			message = "";
		
		if (LOG.isInfoEnabled())
			LOG.info("rollback: " + message);
		
		String actionName = null;
		try {
			if (getWriter().isRollbackSupported()) {
				actionName = "Rollback";
				getWriter().rollback();
				addStatusMessage("Rolledback");
				
			} else if (mCommited) { 
				actionName = "Commit";
				doCommit();
				
			} else { 
				actionName = "Ignore";
			}
			
		} catch (Throwable ex) { 
			message += " " + actionName + " failed: " + ex.toString();
			addStatusMessage(actionName + " failed", Importer.wrapException(ex));
			
			if (LOG.isErrorEnabled())
				LOG.error(actionName + " failed: " + ex.toString(), ex);
			
		} finally {
			addStatusMessage("", "Indexing failed. " + actionName 
					+ " all changes. " + message);
		}
	}
	
	private void finish() throws ErrorException {
		if (LOG.isInfoEnabled())
			LOG.info("Import completed successfully, commit=" + getRequest().isCommit());
		
    	addStatusMessage("", "Indexing completed. Added/Updated: "
    			+ getStatistics().getDocCount() + " documents. Deleted "
    			+ getStatistics().getDeletedDocCount() + " documents.");
    	
    	doCommit();
	}
	
	private void doCommit() throws ErrorException {
    	if (getRequest().isCommit()) {
    		getWriter().commit(getRequest().isOptimize());
    		addStatusMessage("Committed");
    		
    		if (getRequest().isOptimize())
    			addStatusMessage("Optimized");
    		
    		mCommited = true;
    	}
	}
	
	private void cleanByQuery(String delQuery, AtomicBoolean completeCleanDone) 
			throws ErrorException {
	    delQuery = getVariableResolver().replaceTokens(delQuery);
	    if (getRequest().isClean()) {
	    	if (delQuery == null && !completeCleanDone.get()) {
	    		getWriter().deleteAll();
	    		completeCleanDone.set(true);
	    	} else if (delQuery != null) {
	    		getWriter().deleteByQuery(delQuery);
	    	}
	    }
	}
	
	private void doFullDump(ImportProcessorWrapper epw) throws ErrorException {
		addStatusMessage("Full Dump Started");
		buildDocument(epw, false);
	}
	
	private void doDelta(ImportProcessorWrapper epw) throws ErrorException {
		addStatusMessage("Delta Dump Started");
		buildDocument(epw, true);
		addStatusMessage("Deleting documents");
		deleteDocument(epw);
	}
	
	private void deleteDocument(ImportProcessorWrapper epw) throws ErrorException { 
		while (true) {
	        if (mStop.get()) return;
	        
	        try { 
	            ImportRow arow = epw.nextDeletedRow();
	            if (arow == null) 
	            	break;
	            
	            getStatistics().increaseRowsCount();
	            handleSpecialCommands(arow, null);
	            
	            SchemaField sf = getSchema().getUniqueKeyField();
	            if (sf != null) {
	            	Object value = arow.get(sf.getName());
	            	if (value != null) { 
	            		getWriter().deleteDoc(value);
	            		getStatistics().increaseDeletedDocCount();
	            	}
	            }
	            
                arow.done();
	        } catch (Throwable ex) { 
	        	if (ex instanceof ErrorException) 
					throw (ErrorException)ex;
				else
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	        }
		}
	}
	
	private void buildDocument(ImportProcessorWrapper epw, boolean delta) throws ErrorException { 
		int seenDocCount = 0;
		
		while (true) {
	        if (mStop.get()) return;
	        
	        long maxCount = getRequest().getStart() + getRequest().getRows();
	        if (getStatistics().getDocCount() > maxCount) 
	        	break;
	        
	        try { 
	        	seenDocCount ++;
	        	
	            ImportRow arow = delta ? epw.nextModifiedRow() : epw.nextRow();
	            if (arow == null) 
	            	break;
	            
	            // Support for start parameter in debug mode
            	if (seenDocCount <= getRequest().getStart())
            		continue;
            	
            	if (seenDocCount > maxCount) {
            		if (LOG.isInfoEnabled())
            			LOG.info("Indexing stopped at docCount = " + getStatistics().getDocCount());
            		
            		break;
            	}
	            
	            getStatistics().increaseRowsCount();
	            
	            ImportDoc doc = new ImportDoc();
	            handleSpecialCommands(arow, doc);
	            addFields(doc, arow);
	            
                if (!doc.isEmpty()) {
                	try { 
                		mCommited = false;
                		getWriter().upload(doc);
                		getStatistics().increaseDocCount();
                		
                	} catch (Throwable e) { 
                		getStatistics().increaseFailedDocCount();
                		throw e;
                	}
                }
                
                arow.done();
	        } catch (Throwable ex) { 
	        	if (ex instanceof ErrorException) 
					throw (ErrorException)ex;
				else
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
	        }
		}
	}
	
	private void handleSpecialCommands(ImportRow arow, ImportDoc doc) 
			throws ErrorException {
	    Object value = arow.get(ImportRow.DELETE_DOC_BY_ID);
	    if (value != null) {
	    	if (value instanceof Collection) {
	    		Collection<?> collection = (Collection<?>) value;
	    		for (Object o : collection) {
	    			getWriter().deleteDoc(o.toString());
	    			getStatistics().increaseDeletedDocCount();
	    		}
	    	} else {
	    		getWriter().deleteDoc(value);
	    		getStatistics().increaseDeletedDocCount();
	    	}
	    }
	    
	    value = arow.get(ImportRow.DELETE_DOC_BY_QUERY);
	    if (value != null) {
	    	if (value instanceof Collection) {
	    		Collection<?> collection = (Collection<?>) value;
	    		for (Object o : collection) {
	    			getWriter().deleteByQuery(o.toString());
	    			getStatistics().increaseDeletedDocCount();
	    		}
	    	} else {
	    		getWriter().deleteByQuery(value.toString());
	    		getStatistics().increaseDeletedDocCount();
	    	}
	    }
	    
	    value = arow.getBoost();
	    if (value != null) {
	    	float value1 = 1.0f;
	    	if (value instanceof Number) 
	    		value1 = ((Number) value).floatValue();
	    	else 
	    		value1 = Float.parseFloat(value.toString());
	    	
	    	if (doc != null)
	    		doc.setDocumentBoost(value1);
	    }
	}
	
	private void addFields(ImportDoc doc, ImportRow arow) throws ErrorException {
		for (String fieldName : arow.nameSet()) {
			Collection<ImportField> fields = arow.getFields(fieldName);
			if (fields == null) 
				continue;
			
			for (ImportField field : fields) {
				if (field == null) continue;
				if (fieldName.startsWith("$")) continue;
				
				// This can be a dynamic field or a field which does not have an entry 
				// in data-config ( an implicit field)
				SchemaField sf = getSchema().getFieldOrNull(fieldName);
				if (sf != null) {
					addFieldToDoc(doc, sf.getName(), 
							field.getValue(), field.getBoost(), 
							sf.isMultiValued());
					
				} else {
					//else do nothing. if we add it it may fail
					if (LOG.isDebugEnabled())
						LOG.debug("No field-type found for field: " + fieldName);
				}
			}
		}
	}
	
	private void addFieldToDoc(ImportDoc doc, String name, Object value, 
			float boost, boolean multiValued) {
	    if (value instanceof Collection) {
	    	Collection<?> collection = (Collection<?>) value;
	    	if (multiValued) {
	    		for (Object o : collection) {
	    			if (o != null)
	    				doc.addField(name, o, boost);
	    		}
	    	} else {
	    		if (doc.getField(name) == null) {
	    			for (Object o : collection) {
	    				if (o != null)  {
	    					doc.addField(name, o, boost);
	    					break;
	    				}
	    			}
	    		}
	    	}
	    } else if (multiValued) {
	    	if (value != null)  
	    		doc.addField(name, value, boost);
	    	
	    } else {
	    	if (doc.getField(name) == null && value != null)
	    		doc.addField(name, value, boost);
	    }
	}
	
	private VariableResolver loadVariableResolver(Importer importer, 
			ImportRequest req) throws ErrorException { 
		ImportContext context = importer.getContext();
		VariableResolver resolver = new VariableResolver(context);
		//resolver.setEvaluators(mImporter.getEvaluators());
		
		Map<String, Object> indexerNamespace = new HashMap<String, Object>();
		//if (persistedProperties.get(LAST_INDEX_TIME) != null) {
		//	indexerNamespace.put(LAST_INDEX_TIME, persistedProperties.get(LAST_INDEX_TIME));
		//} else  {
		//	// set epoch
		//	indexerNamespace.put(LAST_INDEX_TIME, EPOCH);
		//}
      
		indexerNamespace.put(ImportContext.INDEX_START_TIME, importer.getIndexStartTime());
		//indexerNamespace.put("request", req.getRawParams());
		
		//for (Entity entity : dataImporter.getConfig().getEntities()) {
		//	String key = entity.getName() + "." + SolrWriter.LAST_INDEX_KEY;
		//	Object lastIndex = persistedProperties.get(key);
		//	if (lastIndex != null && lastIndex instanceof Date) {
		//		indexerNamespace.put(key, lastIndex);
		//	} else  {
		//		indexerNamespace.put(key, EPOCH);
		//	}
		//}
		
		resolver.addNamespace(ImportContext.IMPORTER_NS_SHORT, indexerNamespace);
		//resolver.addNamespace(ImportContext.IMPORTER_NS, indexerNamespace);
		
		return resolver;
	}
	
}
