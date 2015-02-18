package org.javenstudio.falcon.datum.index;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.hornet.wrapper.SimpleIndexer;
import org.javenstudio.hornet.wrapper.SimpleSearcher;
import org.javenstudio.panda.analysis.standard.StandardAnalyzer;

public class IndexBuilder extends IndexSearcher {
	private static final Logger LOG = Logger.getLogger(IndexBuilder.class);

	private final DataManager mManager;
	
	private SimpleSearcher mSearcher = null;
	private SimpleIndexer mIndexer = null;
	
	public IndexBuilder(DataManager manager) { 
		if (manager == null) throw new NullPointerException();
		mManager = manager;
	}
	
	@Override
	public DataManager getManager() { 
		return mManager;
	}
	
	@Override
	public String getOwner() { 
		try {
			return getManager().getUser().getUserName();
		} catch (Throwable e) { 
			if (LOG.isDebugEnabled())
				LOG.debug("getOwner: error: " + e, e);
			
			return "";
		}
	}
	
	@Override
	protected synchronized SimpleSearcher initSearcher()
			throws IOException, ErrorException { 
		if (mIndexer != null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Cannot search when indexing");
		}
		
		if (mSearcher == null) { 
			File userDir = new File(getManager().getBlobCacheDir());
			File indexDir = new File(userDir, "index");
			
			if (!indexDir.exists()) 
				return null;
			
			SimpleSearcher searcher = new SimpleSearcher(indexDir, 
					new StandardAnalyzer());
			
			mSearcher = searcher;
		}
		
		return mSearcher;
	}
	
	private synchronized SimpleIndexer initIndexer() 
			throws IOException, ErrorException { 
		if (mSearcher != null) { 
			if (LOG.isDebugEnabled())
				LOG.debug("initIndexer: close searcher: " + mSearcher);
			
			mSearcher.close();
			mSearcher = null;
		}
		
		if (mIndexer == null) { 
			File userDir = new File(getManager().getBlobCacheDir());
			if (!userDir.exists())
				userDir.mkdirs();
			
			File indexDir = new File(userDir, "index");
			if (!indexDir.exists())
				indexDir.mkdirs();
			
			SimpleIndexer indexer = new SimpleIndexer(indexDir, 
		    		new StandardAnalyzer());
			
			mIndexer = indexer;
		}
		
		return mIndexer;
	}
	
	public synchronized void addDoc(IndexDoc doc) 
			throws IOException, ErrorException { 
		if (doc == null) return;
		
		SimpleIndexer indexer = initIndexer();
		indexer.addDocument(doc.toDoc());
	}
	
	public synchronized void commit() 
			throws IOException, ErrorException { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("commit");
		
		SimpleIndexer indexer = mIndexer;
		mIndexer = null;
		
		if (indexer != null)
			indexer.close();
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("close");
		
		try { 
			commit();
			
			if (mSearcher != null) mSearcher.close();
			mSearcher = null;
			
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("close: " + e, e);
		}
	}
	
}
