package org.javenstudio.falcon.datum.index;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionQuery;
import org.javenstudio.falcon.datum.ISectionSet;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.hornet.wrapper.SimpleDocument;
import org.javenstudio.hornet.wrapper.SimpleField;
import org.javenstudio.hornet.wrapper.SimpleSearcher;
import org.javenstudio.hornet.wrapper.SimpleTopDocs;

public abstract class IndexSearcher implements IData {
	private static final Logger LOG = Logger.getLogger(IndexSearcher.class);

	public abstract DataManager getManager();
	
	protected abstract SimpleSearcher initSearcher()
			throws IOException, ErrorException;
	
	public synchronized ISectionSet search(ISectionQuery query, 
			IMember user) throws ErrorException { 
		if (query == null) return null;
		
		String text = query.getQuery();
		int hitsPerPage = query.getResultCount();
		int start = (int)query.getResultStart();
		
		if (hitsPerPage <= 0 || start < 0) 
			return null;
		
		try {
			SimpleSearcher searcher = initSearcher();
			if (searcher == null)
				return null;
			
			return collectDocs(searcher, text, start, hitsPerPage, user);
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	private SearchSet collectDocs(SimpleSearcher searcher, String text, 
			int start, int hitsPerPage, IMember user) throws IOException, ErrorException { 
		if (text == null) text = "";
		
		if (hitsPerPage <= 0 || start < 0) 
			return null;
		
		int topN = start + hitsPerPage;
		
		SimpleTopDocs results = searcher.search("text", text, topN);
		SimpleTopDocs.ScoreDoc[] hits = results.getScoreDocs();
		
		int numTotalHits = results.getTotalHits();
		ArrayList<ISection> list = new ArrayList<ISection>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("collectDocs: query=" + text + " hits=" 
					+ (hits != null ? hits.length : 0) + " total=" + numTotalHits 
					+ " start=" + start + " topN=" + topN);
		}
		
		if (hits != null && hits.length > 0) {
			int end = Math.min(hits.length, topN);
			
			for (int i = start; i < end; i++) {
				SimpleDocument doc = searcher.getDocument(hits[i].getDoc());
				SimpleField id = doc != null ? doc.getField("id") : null;
				
				if (id != null) { 
					String key = id.getStringValue();
					IData data = SectionHelper.getData(user, key, IData.Access.INDEX, null);
					
					if (data != null && data instanceof ISection)
						list.add((ISection)data);
				}
			}
		}
		
		return new SearchSet(list.toArray(new ISection[list.size()]), 
				start, numTotalHits);
	}
	
	public String getName() { return "Search"; }
	public String getExtension() { return null; }
	public String getContentId() { return "all"; }
	public String getContentType() { return "application/x-search"; }
	public String getHostName() { return getManager().getCore().getFriendlyName(); }
	
	public int getTotalFileCount() { return 0; }
	public long getTotalFileLength() { return 0; }
	public long getModifiedTime() { return 0; }
	
	@Override
	public boolean canRead() { 
		return true;
	}
	
	@Override
	public boolean canMove() { 
		return true;
	}
	
	@Override
	public boolean canDelete() { 
		return true;
	}
	
	@Override
	public boolean canWrite() { 
		return false;
	}
	
	@Override
	public boolean canCopy() { 
		return true;
	}
	
	@Override
	public boolean supportOperation(IData.Operation op) { 
		if (op != null) { 
			switch (op) { 
			case DELETE: return true;
			case UPLOAD: return false;
			case NEWFOLDER: return false;
			case MOVE: return true;
			case COPY: return true;
			default: return false;
			}
		}
		return false;
	}
	
}
