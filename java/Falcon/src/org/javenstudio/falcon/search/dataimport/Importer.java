package org.javenstudio.falcon.search.dataimport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.job.Job;
import org.javenstudio.falcon.util.job.JobCancelListener;
import org.javenstudio.falcon.util.job.JobContext;
import org.javenstudio.falcon.util.job.JobSubmit;

/**
 * <p> Stores all configuration information for pulling and indexing data. </p>
 * <p/>
 * <b>This API is experimental and subject to change</b>
 *
 * @since 1.3
 */
public class Importer {
	static final Logger LOG = Logger.getLogger(Importer.class);
	
	public static enum Status {
		IDLE, RUNNING_FULL_DUMP, RUNNING_DELTA_DUMP, JOB_FAILED
	}
	
	private final ImportContext mContext;
	private final Properties mProperties = new Properties();
	private final ImportStatistics mCumulativeStatistics = new ImportStatistics();
	private final ReentrantLock mImportLock = new ReentrantLock();
	
	private Status mStatus = Status.IDLE;
	private ImportBuilder mDocBuilder;
	private Date mIndexStartTime;
	
	public Importer(ImportContext context) { 
		mContext = context;
		
		if (context == null) 
			throw new NullPointerException("ImportContext is null");
	}
	
	public ImportContext getContext() { return mContext; }
	public ISearchCore getSearchCore() { return mContext.getSearchCore(); }
	
	public Status getStatus() { return mStatus; }
	void setStatus(Status status) { mStatus = status; }
	
	public Date getIndexStartTime() { return mIndexStartTime; }
	void setIndexStartTime(Date time) { mIndexStartTime = time; }
	
	void storeProps(Object key, Object value) { mProperties.put(key, value); }
	Object retrieveProps(Object key) { return mProperties.get(key); }
	
	public boolean isBusy() { return mImportLock.isLocked(); }
	
	public Map<String, String> getStatusMessages() { 
	    // this map object is a Collections.synchronizedMap(new LinkedHashMap()). if we
	    // synchronize on the object it must be safe to iterate through the map
	    Map<?,?> statusMessages = (Map<?,?>) retrieveProps(ImportContext.STATUS_MSGS);
	    Map<String, String> result = new LinkedHashMap<String, String>();
	    if (statusMessages != null) {
	    	synchronized (statusMessages) {
	    		for (Object o : statusMessages.entrySet()) {
	    			Map.Entry<?,?> e = (Map.Entry<?,?>) o;
	    			// the toString is taken because some of the Objects create 
	    			// the data lazily when toString() is called
	    			result.put((String) e.getKey(), e.getValue().toString());
	    		}
	    	}
	    }
	    return result;
	}
	
	public class ImportJob implements Job<Void>, JobCancelListener {
		private final ImportRequest mRequest;
		private final ImportWriter mWriter;
		
		private ImportJob(ImportRequest req, ImportWriter writer) { 
			mRequest = req;
			mWriter = writer;
		}
		
		@Override
		public Void run(JobContext jc) {
			try {
				//Notification.addNotification(getMessage(mRequest, mDocBuilder, 
				//		"Indexing \"%1$s\" started."));
				
				runCommand(mRequest, mWriter);
			} finally { 
				ImportBuilder builder = mDocBuilder;
				
				@SuppressWarnings("unused")
				String format = builder != null && builder.isAborted() ? 
						"Indexing \"%1$s\" aborted, updated %2$s and deleted %3$s documents." : 
						"Indexing \"%1$s\" completed, updated %2$s and deleted %3$s documents.";
				
				//Notification.getSystemNotifier().addNotification(
				//		toMessage(mRequest, builder, format));
			}
			
			return null;
		}
		
		@Override
		public Map<String, String> getStatusMessages() { 
			return Importer.this.getStatusMessages();
		}
		
		@Override
		public String getName() {
			return "ImportJob";
		}
		
		@Override
		public String getUser() { 
			return IUser.SYSTEM; //"system";
		}
		
		@Override
		public String getMessage() { 
			return toMessage(mRequest, mDocBuilder, 
					"Indexing \"%1$s\", updated %2$s and deleted %3$s documents.");
		}

		@Override
		public void onCancel() {
			ImportBuilder builder = mDocBuilder;
			if (builder != null) 
				builder.abort();
		}
	}
	
	public static String toMessage(ImportRequest req, 
			ImportBuilder builder, String format) {
		StringBuilder sbuf = new StringBuilder();
		if (req != null) {
			for (String name : req.getSources()) { 
				if (name != null && name.length() > 0) { 
					if (sbuf.length() > 0) sbuf.append(',');
					sbuf.append(name);
				}
			}
		}
		String sources = sbuf.toString();
		sbuf.setLength(0);
		
		long updateCount = 0, deleteCount = 0;
		if (builder != null) {
			updateCount = builder.getStatistics().getDocCount();
			deleteCount = builder.getStatistics().getDeletedDocCount();
		}
		
		return Strings.format(format, 
				sources, ""+updateCount, ""+deleteCount);
	}
	
	public void runAsync(ImportRequest req, ImportWriter writer) { 
	    //new Thread() {
	    //    @Override
	    //    public void run() {
	    //    	runCommand(req, writer);
	    //    }
	    //}.start();
		
		ImportJob job = new ImportJob(req, writer);
		JobSubmit.JobWork<Void> work = JobSubmit.submit(job);
		if (work != null) 
			work.setCancelListener(job);
	}
	
	public void runCommand(ImportRequest req, ImportWriter writer) { 
	    String command = req.getCommand();
	    if (command.equals(ImportContext.ABORT_CMD)) {
	    	if (mDocBuilder != null) {
	    		mDocBuilder.abort();
	    		
	    		//Notification.addNotification(getMessage(req, mDocBuilder, 
	    		//		"Indexing \"%1$s\" aborted."));
	    	}
	    	
	    	return;
	    }
	    
	    if (!mImportLock.tryLock()){
	    	if (LOG.isWarnEnabled())
	    		LOG.warn("Import command failed. another import is running");
	    	
	    	return;
	    }
	    
	    try {
	    	if (ImportContext.FULL_IMPORT_CMD.equals(command) || 
	    		ImportContext.IMPORT_CMD.equals(command)) {
	    		doFullImport(req, writer);
	    	} else if (ImportContext.DELTA_IMPORT_CMD.equals(command)) {
	    		doDeltaImport(req, writer);
	    	}
	    } finally {
	    	mImportLock.unlock();
	    }
	}
	
	private void doFullImport(ImportRequest req, ImportWriter writer) { 
		if (LOG.isInfoEnabled())
			LOG.info("Starting Full Import");
		
	    setStatus(Status.RUNNING_FULL_DUMP);
	    
	    try {
		    setIndexStartTime(new Date());
		    
		    mDocBuilder = new ImportBuilder(this, writer, req);
		    //checkWritablePersistFile(writer, props);
		    mDocBuilder.execute();
		    
		    if (!req.isDebug()) 
		          mCumulativeStatistics.add(mDocBuilder.getStatistics());
		    
	    } catch (Throwable ex) { 
	    	if (LOG.isErrorEnabled())
	    		LOG.error("Full Import failed: " + ex.toString(), ex);
	    	
	    	mDocBuilder.addStatusMessage("Full Import failed", wrapException(ex));
	    	mDocBuilder.rollback(ex.toString());
	    	
	    } finally { 
	    	setStatus(Status.IDLE);
	    }
	}
	
	private void doDeltaImport(ImportRequest req, ImportWriter writer) { 
		if (LOG.isInfoEnabled())
			LOG.info("Starting Delta Import");
		
	    setStatus(Status.RUNNING_DELTA_DUMP);
	    
	    try { 
		    setIndexStartTime(new Date());
		    
		    mDocBuilder = new ImportBuilder(this, writer, req);
		    //checkWritablePersistFile(writer, props);
		    mDocBuilder.execute();
		    
		    if (!req.isDebug()) 
		          mCumulativeStatistics.add(mDocBuilder.getStatistics());
		    
	    } catch (Throwable ex) { 
	    	if (LOG.isErrorEnabled())
	    		LOG.error("Delta Import failed: " + ex.toString(), ex);
	    	
	    	mDocBuilder.addStatusMessage("Delta Import failed", wrapException(ex));
	    	mDocBuilder.rollback(ex.toString());
	    	
	    } finally { 
	    	setStatus(Status.IDLE);
	    }
	}
	
	//protected void checkWritablePersistFile(ImportWriter writer, ImportProperties props) {
	//	if (mDeltaImportSupported && !props.isWritable()) {
	//		throw new RuntimeException("Properties is not writable. " 
	//				+ "Delta imports are supported by data config but will not work.");
	//	}
	//}
	
	public static String getTimeElapsedSince(long l) {
		l = System.currentTimeMillis() - l;
		return (l / (60000 * 60)) + ":" + (l / 60000) % 60 + ":" + (l / 1000) % 60 + "." + l % 1000;
	}
	
	public static String wrapException(Throwable ex) { 
		return wrapException(ex != null ? ex.toString() : null, ex);
	}
	
	public static String wrapException(String msg, Throwable ex) { 
		StringBuilder sbuf = new StringBuilder();
		
		if (msg != null) 
			sbuf.append(msg);
		
		if (ex != null) { 
			StringWriter sw = new StringWriter();
	    	ex.printStackTrace(new PrintWriter(sw));
	    	
	    	if (sbuf.length() > 0) 
	    		sbuf.append("\r\n");
	    	
	    	sbuf.append(sw.toString());
		}
		
		return sbuf.toString();
	}
	
}
