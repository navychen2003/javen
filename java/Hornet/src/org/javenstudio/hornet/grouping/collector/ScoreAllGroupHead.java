package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.GroupHead;

public class ScoreAllGroupHead extends GroupHead<BytesRef> {
	
	private final ScoreAllGroupHeadsCollector mCollector;
    private final float[] mScores;

    public ScoreAllGroupHead(ScoreAllGroupHeadsCollector collector, 
    		int doc, BytesRef groupValue, int fieldsLength) throws IOException {
    	super(groupValue, doc + collector.getReaderContext().getDocBase());
    	
    	mCollector = collector;
    	mScores = new float[fieldsLength];
    	
    	float score = collector.getScorer().getScore();
    	for (int i = 0; i < mScores.length; i++) {
    		mScores[i] = score;
    	}
    }

    @Override
    public int compare(int compIDX, int doc) throws IOException {
    	float score = mCollector.getScorer().getScore();
    	
    	if (mScores[compIDX] < score) 
    		return 1;
    	else if (mScores[compIDX] > score) 
    		return -1;
    	
    	return 0;
    }

    @Override
    public void updateDocHead(int doc) throws IOException {
      float score = mCollector.getScorer().getScore();
      for (int i = 0; i < mScores.length; i++) {
    	  mScores[i] = score;
      }
      
      mDoc = doc + mCollector.getReaderContext().getDocBase();
    }
    
}
