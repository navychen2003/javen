package org.javenstudio.common.indexdb.index;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.IPayloadProcessor;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.MergeAbortedException;

/** 
 * Holds common state used during segment merging
 */
public class MergeState implements IMergeState {

	@SuppressWarnings("unused")
	private final SegmentMerger mMerger;
	private final ISegmentInfo mSegmentInfo;
	private final CheckAbort mCheckAbort;
	private final List<IAtomicReader> mReaders; // Readers being merged
	
	private IFieldInfos mFieldInfos;
	private DocMap[] mDocMaps;	// Maps docIDs around deletions
	private int[] mDocBase; 	// New docID base per reader
	
	// Updated per field;
	private IFieldInfo mFieldInfo;
  
	// Used to process payloads
	// TODO: this is a FactoryFactory here basically
	// and we could make a codec(wrapper) to do all of this privately so IW is uninvolved
	private IPayloadProcessor.Provider mPayloadProcessorProvider;
	private IPayloadProcessor.Reader[] mPayloadProcessorReaders;
	private IPayloadProcessor[] mCurrentPayloadProcessors;

	// TODO: get rid of this? it tells you which segments are 'aligned' (e.g. for bulk merging)
	// but is this really so expensive to compute again in different components, versus once in SM?
	private ISegmentReader[] mMatchingSegmentReaders;
	private int mMatchedCount = 0;
	
	public MergeState(SegmentMerger merger, ISegmentInfo segmentInfo, 
			CheckAbort checkAbort) {
		mMerger = merger;
		mSegmentInfo = segmentInfo;
		mCheckAbort = checkAbort;
		mReaders = new ArrayList<IAtomicReader>();
		mPayloadProcessorProvider = merger.getIndexWriter().getPayloadProcessorProvider();
	}
	
	public ISegmentInfo getSegmentInfo() { return mSegmentInfo; }
	//public void setSegmentInfo(ISegmentInfo info) { mSegmentInfo = info; }
	
	public IFieldInfos getFieldInfos() { return mFieldInfos; }
	public void setFieldInfos(IFieldInfos infos) { mFieldInfos = infos; }
	
	//public List<IAtomicReader> getReaders() { return mReaders; }
	//public void initReaders() { mReaders = new ArrayList<IAtomicReader>(); }
	public void addReader(IAtomicReader reader) { mReaders.add(reader); }
	
	public int getReaderCount() { return mReaders.size(); }
	public IAtomicReader getReaderAt(int index) { return mReaders.get(index); }
	
	public void initDocBase(int size) { mDocBase = new int[size]; }
	public void setDocBaseAt(int index, int val) { mDocBase[index] = val; }
	public int getDocBaseAt(int index) { return mDocBase[index]; }
	
	public void initDocMaps(int size) { mDocMaps = new DocMap[size]; }
	public void setDocMapAt(int index, DocMap docMap) { mDocMaps[index] = docMap; }
	public DocMap getDocMapAt(int index) { return mDocMaps[index]; }
	
	public IFieldInfo getFieldInfo() { return mFieldInfo; }
	public void setFieldInfo(IFieldInfo info) { mFieldInfo = info; }
	
	final void initMatchingSegmentReaders(int numReaders) { 
		mMatchingSegmentReaders = new ISegmentReader[numReaders];
		mMatchedCount = 0;
	}
	
	final void setMatchingSegmentReaderAt(int index, ISegmentReader reader) { 
		mMatchingSegmentReaders[index] = reader;
	}
	
	final void increaseMatchedCount(int count) { 
		mMatchedCount += count;
	}
	
	public int getMatchedCount() { return mMatchedCount; }
	
	public ISegmentReader getMatchingSegmentReaderAt(int index) { 
		return mMatchingSegmentReaders[index]; 
	}
	
	//public void setPayloadProcessorProvider(IPayloadProcessorProvider provider) { 
	//	mPayloadProcessorProvider = provider;
	//}
	
	public IPayloadProcessor.Provider getPayloadProcessorProvider() { 
		return mPayloadProcessorProvider;
	}
	
	public void initPayloadProcessorReaders(int size) { 
		mPayloadProcessorReaders = new IPayloadProcessor.Reader[size];
	}
	
	public void setPayloadProcessorReaderAt(int index, IPayloadProcessor.Reader reader) { 
		mPayloadProcessorReaders[index] = reader;
	}
	
	public IPayloadProcessor.Reader getPayloadProcessorReaderAt(int index) { 
		return mPayloadProcessorReaders[index];
	}
	
	public void initCurrentPayloadProcessors(int size) { 
		mCurrentPayloadProcessors = new IPayloadProcessor[size];
	}
	
	public IPayloadProcessor getCurrentPayloadProcessorAt(int index) { 
		return mCurrentPayloadProcessors[index];
	}
	
	public void setCurrentPayloadProcessorAt(int index, IPayloadProcessor processor) { 
		mCurrentPayloadProcessors[index] = processor;
	}
	
	//public void setCheckAbort(CheckAbort checkAbort) { 
	//	mCheckAbort = checkAbort;
	//}
	
	public void checkAbort(double units) throws MergeAbortedException { 
		mCheckAbort.work(units);
	}
	
}
