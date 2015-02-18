package org.javenstudio.falcon.search.update;

import java.util.List;
import java.util.Vector;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.search.EventListener;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.SearchCoreState;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * <code>UpdateHandler</code> handles requests to change the index
 * (adds, deletes, commits, optimizes, etc).
 *
 * @since 0.9
 */
public abstract class UpdateIndexer implements InfoMBean {
  	private static Logger LOG = Logger.getLogger(UpdateIndexer.class);

  	protected final ISearchCore mCore;
  	protected final IndexSchema mSchema;

  	protected final SchemaField mIdField;
  	protected final SchemaFieldType mIdFieldType;

  	private List<EventListener> mCommitCallbacks = new Vector<EventListener>();
  	private List<EventListener> mSoftCommitCallbacks = new Vector<EventListener>();
  	private List<EventListener> mOptimizeCallbacks = new Vector<EventListener>();

  	protected UpdateLog mUpdateLog = null;

  	public UpdateIndexer(ISearchCore core) throws ErrorException {
  		mCore = core;
  		mSchema = core.getSchema();
  		mIdField = mSchema.getUniqueKeyField();
  		mIdFieldType = (mIdField != null) ? mIdField.getType() : null;
  		
  		if (LOG.isDebugEnabled())
  			LOG.debug("Opening UpdateIndexer: " + getClass().getName() + " idField=" + mIdField);
  		
  		parseEventListeners();
  		initUpdateLog();
  	}
  	
  	public final ISearchCore getSearchCore() { return mCore; }
  	public final IndexSchema getSchema() { return mSchema; }
  	
  	public boolean isRollbackSupported() { return mUpdateLog != null; }
  	
  	/**
  	 * Called when a Core using this UpdateHandler is closed.
  	 */
  	public abstract void decreaseRef() throws ErrorException;
  
  	/**
  	 * Called when this UpdateHandler is shared with another Core.
  	 */
  	public abstract void increaseRef() throws ErrorException;

  	private void parseEventListeners() throws ErrorException {
  		final Class<EventListener> clazz = EventListener.class;
  		
  		for (PluginInfo info : mCore.getSearchConfig().getPluginInfos(EventListener.class.getName())) {
  			String event = info.getAttribute("event");
  			if ("postCommit".equals(event)) {
  				EventListener obj = mCore.createPlugin(info, clazz);
  				registerCommitCallback(obj);
  				
  			} else if ("postOptimize".equals(event)) {
  				EventListener obj = mCore.createPlugin(info, clazz);
  				registerOptimizeCallback(obj);
  				
  			}
  		}
  	}

  	protected void initUpdateLog() throws ErrorException {
  		PluginInfo info = mCore.getSearchConfig().getPluginInfo(UpdateLog.class.getName());
  		if (info != null && info.isEnabled()) {
  			// not implements
  		}
  	}

  	protected void callPostCommitCallbacks() {
  		synchronized (mCommitCallbacks) {
	  		for (EventListener listener : mCommitCallbacks) {
	  			if (LOG.isDebugEnabled())
	  				LOG.debug("callPostCommitCallbacks: " + listener);
	  			
	  			listener.postCommit();
	  		}
  		}
  	}

  	protected void callPostSoftCommitCallbacks() {
  		synchronized (mSoftCommitCallbacks) {
	  		for (EventListener listener : mSoftCommitCallbacks) {
	  			if (LOG.isDebugEnabled())
	  				LOG.debug("callPostSoftCommitCallbacks: " + listener);
	  			
	  			listener.postSoftCommit();
	  		}
  		}
  	}
  
  	protected void callPostOptimizeCallbacks() {
  		synchronized (mOptimizeCallbacks) {
	  		for (EventListener listener : mOptimizeCallbacks) {
	  			if (LOG.isDebugEnabled())
	  				LOG.debug("callPostOptimizeCallbacks: " + listener);
	  			
	  			listener.postCommit();
	  		}
  		}
  	}

  	/**
  	 * Called when the Writer should be opened again - eg when replication replaces
  	 * all of the index files.
  	 * 
  	 * @param rollback IndexWriter if true else close
  	 * 
  	 * @throws IOException If there is a low-level I/O error.
  	 */
  	public abstract void newIndexWriter(boolean rollback) throws ErrorException;

  	public abstract SearchCoreState getCoreState();

  	public abstract int addDoc(AddCommand cmd) throws ErrorException;
  	public abstract void delete(DeleteCommand cmd) throws ErrorException;
  	public abstract void deleteByQuery(DeleteCommand cmd) throws ErrorException;
  	public abstract int mergeIndexes(MergeCommand cmd) throws ErrorException;
  	public abstract void commit(CommitCommand cmd) throws ErrorException;
  	public abstract void rollback(RollbackCommand cmd) throws ErrorException;
  	public abstract void close() throws ErrorException;
  	
  	public abstract UpdateLog getUpdateLog();
  	public abstract void finish(boolean changesSinceCommit) throws ErrorException;

  	/**
  	 * NOTE: this function is not thread safe.  However, it is safe to call within the
  	 * <code>inform( Core core )</code> function for <code>CoreAware</code> classes.
  	 * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
  	 *
  	 * @see CoreAware
  	 */
  	public void registerCommitCallback(EventListener listener) {
  		if (listener != null) {
  			synchronized (mCommitCallbacks) {
  				mCommitCallbacks.add(listener);
  			}
  		
  			if (LOG.isDebugEnabled())
  				LOG.debug("registerCommitCallback: " + listener);
  		}
  	}
  
  	/**
  	 * NOTE: this function is not thread safe.  However, it is safe to call within the
  	 * <code>inform( Core core )</code> function for <code>CoreAware</code> classes.
  	 * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
  	 *
  	 * @see CoreAware
  	 */
  	public void registerSoftCommitCallback(EventListener listener) {
  		if (listener != null) {
  			synchronized (mSoftCommitCallbacks) {
  				mSoftCommitCallbacks.add(listener);
  			}
  			
  			if (LOG.isDebugEnabled())
  				LOG.debug("registerSoftCommitCallback: " + listener);
  		}
  	}

  	/**
  	 * NOTE: this function is not thread safe.  However, it is safe to call within the
  	 * <code>inform( Core core )</code> function for <code>CoreAware</code> classes.
  	 * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
  	 *
  	 * @see CoreAware
  	 */
  	public void registerOptimizeCallback(EventListener listener) {
  		if (listener != null) {
  			synchronized (mOptimizeCallbacks) {
  				mOptimizeCallbacks.add(listener);
  			}
  			
  			if (LOG.isDebugEnabled())
  				LOG.debug("registerOptimizeCallback: " + listener);
  		}
  	}

  	//public abstract void split(SplitIndexCommand cmd) throws IOException;
  
	@Override
	public String getMBeanKey() {
		return "UpdateIndexer";
	}

	@Override
	public String getMBeanName() {
		return getClass().getName();
	}

	@Override
	public String getMBeanVersion() {
		return "1.0";
	}
  	
	@Override
	public String getMBeanCategory() {
		return InfoMBean.CATEGORY_UPDATEHANDLER;
	}
	
}
