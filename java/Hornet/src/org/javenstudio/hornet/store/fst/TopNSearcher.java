package org.javenstudio.hornet.store.fst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

final class TopNSearcher<T> {

    private final FST<T> mFst;
    private final FSTArc<T> mFromNode;
    private final int mTopN;
    
    // Set once the queue has filled:
    private FSTPath<T> mBottom = null;

    private final Comparator<T> mComparator;
    private TreeSet<FSTPath<T>> mQueue = null;

    public TopNSearcher(FST<T> fst, FSTArc<T> fromNode, int topN, 
    		Comparator<T> comparator) {
    	mFst = fst;
    	mTopN = topN;
    	mFromNode = fromNode;
    	mComparator = comparator;
    }

    // If back plus this arc is competitive then add to queue:
    private void addIfCompetitive(FSTPath<T> path) {
    	assert mQueue != null;
    	T cost = mFst.getOutputs().add(path.getCost(), path.getArc().getOutput());

    	if (mBottom != null) {
    		int comp = mComparator.compare(cost, mBottom.getCost());
    		if (comp > 0) {
    			// Doesn't compete
    			return;
    			
    		} else if (comp == 0) {
    			// Tie break by alpha sort on the input:
    			path.getInput().grow(path.getInput().getLength()+1);
    			path.getInput().setIntAt(path.getInput().mLength++, path.getArc().getLabel());
    			
    			final int cmp = mBottom.getInput().compareTo(path.getInput());
    			path.getInput().mLength--;
    			assert cmp != 0;
    			if (cmp < 0) {
    				// Doesn't compete
    				return;
    			}
    		}
    		// Competes
    	} else {
    		// Queue isn't full yet, so any path we hit competes:
    	}

    	final FSTPath<T> newPath = new FSTPath<T>(cost, path.getArc(), mComparator);

    	newPath.getInput().grow(path.getInput().getLength()+1);
    	System.arraycopy(path.getInput().mInts, 0, newPath.getInput().mInts, 0, path.getInput().mLength);
    	newPath.getInput().mInts[path.getInput().mLength] = path.getArc().getLabel();
    	newPath.getInput().mLength = path.getInput().mLength+1;

    	mQueue.add(newPath);
    	if (mBottom != null) {
    		final FSTPath<T> removed = mQueue.pollLast();
    		assert removed == mBottom;
    		mBottom = mQueue.last();
    		
    	} else if (mQueue.size() == mTopN) {
    		// Queue just filled up:
    		mBottom = mQueue.last();
    	}
    }

    public MinResult<T>[] search() throws IOException {
    	final FSTArc<T> scratchArc = new FSTArc<T>();
    	final List<MinResult<T>> results = new ArrayList<MinResult<T>>();

    	final BytesReader fstReader = mFst.getBytesReader(0);
    	final T NO_OUTPUT = mFst.getOutputs().getNoOutput();

    	// TODO: we could enable FST to sorting arcs by weight
    	// as it freezes... can easily do this on first pass
    	// (w/o requiring rewrite)

    	// TODO: maybe we should make an FST.INPUT_TYPE.BYTE0.5!?
    	// (nibbles)

    	// For each top N path:
    	while (results.size() < mTopN) {
    		FSTPath<T> path;
    		if (mQueue == null) {
    			if (results.size() != 0) {
    				// Ran out of paths
    				break;
    			}

    			// First pass (top path): start from original fromNode
    			if (mTopN > 1) 
    				mQueue = new TreeSet<FSTPath<T>>();

    			T minArcCost = null;
    			FSTArc<T> minArc = null;

    			path = new FSTPath<T>(NO_OUTPUT, mFromNode, mComparator);
    			mFst.readFirstTargetArc(mFromNode, path.getArc(), fstReader);

    			// Bootstrap: find the min starting arc
    			while (true) {
    				T arcScore = path.getArc().getOutput();
    				if (minArcCost == null || mComparator.compare(arcScore, minArcCost) < 0) {
    					minArcCost = arcScore;
    					minArc = scratchArc.copyFrom(path.getArc());
    				}
    				
    				if (mQueue != null) 
    					addIfCompetitive(path);
    				
    				if (path.getArc().isLast()) 
    					break;
    				
    				mFst.readNextArc(path.getArc(), fstReader);
    			}

    			assert minArc != null;

    			if (mQueue != null) {
    				// Remove top path since we are now going to
    				// pursue it:
    				path = mQueue.pollFirst();
    				assert path.getArc().getLabel() == minArc.getLabel();
    				if (mBottom != null && mQueue.size() == mTopN-1) 
    					mBottom = mQueue.last();
    				
    			} else {
    				path.getArc().copyFrom(minArc);
    				path.getInput().grow(1);
    				path.getInput().mInts[0] = minArc.getLabel();
    				path.getInput().mLength = 1;
    				path.setCost(minArc.getOutput());
    			}

    		} else {
    			path = mQueue.pollFirst();
    			if (path == null) {
    				// There were less than topN paths available:
    				break;
    			}
    		}

    		if (path.getArc().getLabel() == FST.END_LABEL) {
    			// Empty string!
    			path.getInput().mLength--;
    			results.add(new MinResult<T>(path.getInput(), path.getCost(), mComparator));
    			continue;
    		}

    		if (results.size() == mTopN-1) {
    			// Last path -- don't bother w/ queue anymore:
    			mQueue = null;
    		}

    		// We take path and find its "0 output completion",
    		// ie, just keep traversing the first arc with
    		// NO_OUTPUT that we can find, since this must lead
    		// to the minimum path that completes from
    		// path.arc.

    		// For each input letter:
    		while (true) {
    			mFst.readFirstTargetArc(path.getArc(), path.getArc(), fstReader);

    			// For each arc leaving this node:
    			boolean foundZero = false;
    			while (true) {
    				// tricky: instead of comparing output == 0, we must
    				// express it via the comparator compare(output, 0) == 0
    				if (mComparator.compare(NO_OUTPUT, path.getArc().getOutput()) == 0) {
    					if (mQueue == null) {
    						foundZero = true;
    						break;
    					} else if (!foundZero) {
    						scratchArc.copyFrom(path.getArc());
    						foundZero = true;
    					} else 
    						addIfCompetitive(path);
    					
    				} else if (mQueue != null) {
    					addIfCompetitive(path);
    				}
    				if (path.getArc().isLast()) 
    					break;
    				
    				mFst.readNextArc(path.getArc(), fstReader);
    			}

    			assert foundZero;

    			if (mQueue != null) {
    				// TODO: maybe we can save this copyFrom if we
    				// are more clever above... eg on finding the
    				// first NO_OUTPUT arc we'd switch to using
    				// scratchArc
    				path.getArc().copyFrom(scratchArc);
    			}

    			if (path.getArc().getLabel() == FST.END_LABEL) {
    				// Add final output:
    				results.add(new MinResult<T>(path.getInput(), 
    						mFst.getOutputs().add(path.getCost(), path.getArc().getOutput()), mComparator));
    				break;
    			} else {
    				path.getInput().grow(1+path.getInput().mLength);
    				path.getInput().mInts[path.getInput().mLength] = path.getArc().getLabel();
    				path.getInput().mLength ++;
    				path.setCost(mFst.getOutputs().add(path.getCost(), path.getArc().getOutput()));
    			}
    		}
    	}
    
    	@SuppressWarnings({"unchecked"}) 
    	final MinResult<T>[] arr = (MinResult<T>[]) new MinResult[results.size()];
    	return results.toArray(arr);
	}
    
}
