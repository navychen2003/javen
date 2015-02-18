package org.javenstudio.hornet.wrapper;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ITopDocs;

public final class SimpleTopDocs {

	public static class ScoreDoc { 
		private final IScoreDoc mDoc;
		
		ScoreDoc(IScoreDoc doc) { 
			mDoc = doc;
		}
		
		public float getScore() { 
			return mDoc.getScore();
		}
		
		public int getDoc() { 
			return mDoc.getDoc();
		}
	}
	
	private final ITopDocs mTopDocs;
	
	SimpleTopDocs(ITopDocs topDocs) { 
		mTopDocs = topDocs;
	}
	
	public float getMaxScore() { 
		return mTopDocs.getMaxScore();
	}
	
	public int getTotalHits() { 
		return mTopDocs.getTotalHits();
	}
	
	public int getScoreDocsSize() { 
		return mTopDocs.getScoreDocsSize();
	}
	
	public ScoreDoc getScoreDocAt(int index) { 
		IScoreDoc doc = mTopDocs.getScoreDocAt(index);
		return doc != null ? new ScoreDoc(doc) : null;
	}
	
	public ScoreDoc[] getScoreDocs() { 
		IScoreDoc[] docs = mTopDocs.getScoreDocs();
		if (docs == null || docs.length == 0)
			return null;
		
		ScoreDoc[] scoreDocs = new ScoreDoc[docs.length];
		for (int i=0; i < docs.length; i++) { 
			scoreDocs[i] = new ScoreDoc(docs[i]);
		}
		
		return scoreDocs;
	}
	
}
