package org.javenstudio.falcon.search;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.index.IndexCommit;
import org.javenstudio.common.indexdb.index.IndexParams;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.index.AdvancedIndexWriter;
import org.javenstudio.falcon.search.store.DirectoryFactory;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class SearchWriter extends AdvancedIndexWriter implements InfoMBean {
	private static Logger LOG = Logger.getLogger(SearchWriter.class);
	
	/** 
	 * Stored into each commit to record the
	 *  System.currentTimeMillis() when commit was called. 
	 */
	public static final String COMMIT_TIME_MSEC_KEY = "commitTimeMSec";
	
	// These should *only* be used for debugging or monitoring purposes
	private static final AtomicLong sNumOpens = new AtomicLong();
	private static final AtomicLong sNumCloses = new AtomicLong();
	private static final AtomicLong sNumCounter = new AtomicLong();

	private long mOpenTime = System.currentTimeMillis();
	private long mFlushTime = 0;
	
	private DirectoryFactory mDirectoryFactory;
	private volatile boolean mIsClosed = false;
	private String mName;

	public static SearchWriter create(String name, String path, 
			DirectoryFactory directoryFactory, IndexParams params) throws IOException {
		SearchWriter w = null;
		boolean forceNewDirectory = false;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Open directory for Writer with path=" + path 
					+ ", forceNewDirectory=" + forceNewDirectory);
		}
		
		final IDirectory d = directoryFactory.get(path, null, forceNewDirectory);
		try {
			w = new SearchWriter(name, d, params);
			w.setDirectoryFactory(directoryFactory);
			return w;
			
		} finally {
			if (w == null && d != null) { 
				if (LOG.isDebugEnabled())
					LOG.debug("Writer create failed and release directory: " + d);
				
				directoryFactory.doneWithDirectory(d);
				directoryFactory.release(d);
			}
		}
	}

	private SearchWriter(String name, IDirectory directory, IndexParams params) 
			throws IOException {
		super(directory, params);
		
		mName = "Indexer-" + sNumCounter.incrementAndGet() + "@" 
				+ name + "/" + Thread.currentThread().getName();
		
		if (LOG.isDebugEnabled())
			LOG.debug("Opened Writer " + mName);
		
		sNumOpens.incrementAndGet();
	}
  
	@Override
	public String toString() { return getName(); }
	
	public String getName() { return mName; }
	
	private void setDirectoryFactory(DirectoryFactory factory) {
		mDirectoryFactory = factory;
	}

	public void forceMergeDeletes() throws IOException {
		getMergeControl().forceMergeDeletes();
	}
	
	public void forceMerge(int maxNumSegments) throws IOException {
		getMergeControl().forceMerge(maxNumSegments);
	}
	
	/**
	 * use DocumentBuilder now...
	 * private final void addField(Document doc, String name, String val) {
	 * SchemaField ftype = schema.getField(name);
	 * <p/>
	 * // we don't check for a null val ourselves because a lightning.FieldType
	 * // might actually want to map it to something.  If createField()
	 * // returns null, then we don't store the field.
	 * <p/>
	 * Field field = ftype.createField(val, boost);
	 * if (field != null) doc.add(field);
	 * }
	 * <p/>
	 * <p/>
	 * public void addRecord(String[] fieldNames, String[] fieldValues) throws IOException {
	 * Document doc = new Document();
	 * for (int i=0; i<fieldNames.length; i++) {
	 * String name = fieldNames[i];
	 * String val = fieldNames[i];
	 * <p/>
	 * // first null is end of list.  client can reuse arrays if they want
	 * // and just write a single null if there is unused space.
	 * if (name==null) break;
	 * <p/>
	 * addField(doc,name,val);
	 * }
	 * addDocument(doc);
	 * }
	 * ****
	 */
	@Override
	public void close() throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("Closing Writer " + mName);
		
		IDirectory directory = getDirectory();  
		try {
			super.close();
		} finally {
			mIsClosed = true;
			mDirectoryFactory.release(directory);
			sNumCloses.incrementAndGet();
		}
	}

	@Override
	public void rollback() throws IOException {
		try {
			super.rollback();
		} finally {
			mIsClosed = true;
			mDirectoryFactory.release(getDirectory());
			sNumCloses.incrementAndGet();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			if (!mIsClosed) {
				assert false : "SearchWriter was not closed prior to finalize()";
				if (LOG.isErrorEnabled()) {
					LOG.error("SearchWriter was not closed prior to finalize(), " 
							+ "indicates a bug -- POSSIBLE RESOURCE LEAK!!!");
				}
				close();
			}
		} finally { 
			super.finalize();
		}
	}
  
	@Override
	public void doAfterFlush() throws IOException { 
		super.doAfterFlush();
		mFlushTime = System.currentTimeMillis();
	}
	
	public static long getCommitTimestamp(IndexCommit commit) throws IOException {
		final Map<String,String> commitData = commit.getUserData();
		String commitTime = commitData.get(SearchWriter.COMMIT_TIME_MSEC_KEY);
		if (commitTime != null) 
			return Long.parseLong(commitTime);
		else 
			return 0;
	}

	@Override
	public String getMBeanKey() {
		return getName();
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
	public String getMBeanDescription() {
		return "Index Writer";
	}

	@Override
	public String getMBeanCategory() {
		return InfoMBean.CATEGORY_CORE;
	}

	@Override
	public NamedList<?> getMBeanStatistics() {
		NamedList<Object> lst = new NamedMap<Object>();
	    lst.add("writerName", mName);
	    
	    boolean closed = true;
	    if (!isClosed() && !isClosing()) {
	    	closed = false;
	    	
		    lst.add("numDocs", getNumDocs());
		    lst.add("maxDoc", getMaxDoc());
		    lst.add("ramSizeInBytes", getRamSizeInBytes());
		    lst.add("numRamDocs", getNumRamDocs());
		    lst.add("hasDeletions", hasDeletions());
		    lst.add("writerDir", getDirectory());
		    lst.add("segmentInfos", getSegmentInfos());
	    }
	    
	    lst.add("openedAt", new Date(mOpenTime));
	    if (mFlushTime != 0)
	    	lst.add("lastFlushAt", new Date(mFlushTime));
	    
	    if (closed)
	    	lst.add("closed", "true");
	    
	    return lst;
	}
  
}
