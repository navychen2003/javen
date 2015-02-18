package org.javenstudio.hornet.index.segment;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentClosedListener;
import org.javenstudio.common.indexdb.index.field.FieldInfos;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.store.CompoundFileDirectory;
import org.javenstudio.common.indexdb.util.CloseableThreadLocal;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.FieldsProducer;
import org.javenstudio.hornet.codec.FieldsReader;
import org.javenstudio.hornet.codec.IndexFormat;
import org.javenstudio.hornet.codec.PostingsFormat;
import org.javenstudio.hornet.codec.SegmentReadState;
import org.javenstudio.hornet.codec.TermVectorsReader;

/** 
 * Holds core readers that are shared (unchanged) when
 * SegmentReader is cloned or reopened 
 */
final class SegReaders {
	//private static final Logger LOG = Logger.getLogger(SegReaders.class);
  
	// Counts how many other reader share the core objects
	// (freqStream, proxStream, tis, etc.) of this reader;
	// when coreRef drops to 0, these core objects may be
	// closed.  A given instance of SegmentReader may be
	// closed, even those it shares core objects with other
	// SegmentReaders:
	private final AtomicInteger mRef = new AtomicInteger(1);
  
	private final FieldInfos mFieldInfos;
	private final FieldsProducer mFields;

	private final FieldsReader mFieldsReaderOrig;
	private final TermVectorsReader mTermVectorsReaderOrig;
	private final CompoundFileDirectory mCfsReader;

	private final CloseableThreadLocal<FieldsReader> mFieldsReaderLocal = 
		new CloseableThreadLocal<FieldsReader>() {
			@Override
			protected FieldsReader initialValue() {
				return mFieldsReaderOrig.clone();
			}
		};
  
	private final CloseableThreadLocal<TermVectorsReader> mTermVectorsLocal = 
		new CloseableThreadLocal<TermVectorsReader>() {
			@Override
			protected TermVectorsReader initialValue() {
				return (mTermVectorsReaderOrig == null) ? null : mTermVectorsReaderOrig.clone();
			}
		};
  
	private final Set<ISegmentClosedListener> mClosedListeners = 
			Collections.synchronizedSet(new LinkedHashSet<ISegmentClosedListener>());
  
	private final SegmentReader mOwner;
	private final IIndexContext mContext;
	private final IDirectory mDirectory;
	
	final IIndexContext getContext() { return mContext; }
	final IDirectory getDirectory() { return mDirectory; }
	
	final FieldInfos getFieldInfos() { return mFieldInfos; }
	final FieldsProducer getFieldsProducer() { return mFields; }
	
	final FieldsReader getFieldsReader() { return mFieldsReaderLocal.get(); }
	final TermVectorsReader getTermVectorsReader() { return mTermVectorsLocal.get(); }
	
	SegReaders(SegmentReader owner, IDirectory dir, IndexFormat format, 
			SegmentCommitInfo si) throws IOException {
		mContext = format.getContext();
		mDirectory = dir;
		
		boolean success = false;
		try {
			// confusing name: if (cfs) its the cfsdir, otherwise its the segment's directory.
			final IDirectory cfsDir; 
			
			if (si.getSegmentInfo().getUseCompoundFile()) {
				cfsDir = mCfsReader = new CompoundFileDirectory(format.getContext(), dir, 
						format.getCompoundFileName(si.getSegmentInfo().getName()), 
						false);
			} else {
				mCfsReader = null;
				cfsDir = dir;
			}
			
			mFieldInfos = (FieldInfos)format.getFieldInfosFormat().createReader(cfsDir, 
					si.getSegmentInfo().getName()).readFieldInfos(); 
			
			final PostingsFormat postings = (PostingsFormat)format.getPostingsFormat();
			final SegmentReadState segmentReadState = new SegmentReadState(
					si.getSegmentInfo(), mFieldInfos);
			
			// Ask codec for its Fields
			mFields = (FieldsProducer)postings.getFieldsProducer(
					cfsDir, segmentReadState);
			assert mFields != null;
			
			mFieldsReaderOrig = (FieldsReader)format.getFieldsFormat().createReader(cfsDir, 
					si.getSegmentInfo().getName(), si.getSegmentInfo(), mFieldInfos); 

			if (mFieldInfos.hasVectors()) { // open term vector files only as needed
				mTermVectorsReaderOrig = (TermVectorsReader)format.getTermVectorsFormat().createReader(cfsDir, 
						si.getSegmentInfo(), mFieldInfos); 
			} else {
				mTermVectorsReaderOrig = null;
			}

			success = true;
		} finally {
			if (!success) 
				decreaseRef();
		}
    
		// Must assign this at the end -- if we hit an
		// exception above core, we don't want to attempt to
		// purge the FieldCache (will hit NPE because core is
		// not assigned yet).
		mOwner = owner;
	}
  
	final void increaseRef() {
		mRef.incrementAndGet();
	}
  
	final void decreaseRef() throws IOException {
		if (mRef.decrementAndGet() == 0) {
			IOUtils.close(mTermVectorsLocal, mFieldsReaderLocal, mFields, 
					mTermVectorsReaderOrig, mFieldsReaderOrig, mCfsReader);
			notifyClosedListeners();
		}
	}
  
	private final void notifyClosedListeners() {
		synchronized (mClosedListeners) {
			for (ISegmentClosedListener listener : mClosedListeners) {
				listener.onClose(mOwner);
			}
		}
	}

	final void addClosedListener(ISegmentClosedListener listener) {
		mClosedListeners.add(listener);
	}
  
	final void removeClosedListener(ISegmentClosedListener listener) {
		mClosedListeners.remove(listener);
	}

	@Override
	public String toString() {
		return "SegReaders(owner=" + mOwner + ")";
	}
	
}
