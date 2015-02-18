package org.javenstudio.hornet.index;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIntsMutable;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.CheckAbort;
import org.javenstudio.common.indexdb.index.DocMap;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.indexdb.index.MergeState;
import org.javenstudio.common.indexdb.index.SegmentMerger;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;
import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.hornet.codec.SegmentWriteState;
import org.javenstudio.hornet.index.field.MultiFields;
import org.javenstudio.hornet.store.packed.PackedInts;

/**
 * The SegmentMerger class combines two or more Segments, represented by 
 * an IndexReader ({@link #add}, into a single Segment.  
 * After adding the appropriate readers, call the merge method to combine 
 * the segments.
 *
 * @see #merge
 * @see #add
 */
final class AdvancedMerger extends SegmentMerger {

	public AdvancedMerger(IndexWriter indexWriter, 
			ISegmentInfo segmentInfo, IDirectory dir, CheckAbort checkAbort, 
			FieldNumbers globalFieldNumbers) {
		super(indexWriter, segmentInfo, dir, checkAbort, globalFieldNumbers);
	}
	
	@Override
	protected DocMap buildDocMap(IAtomicReader reader) { 
		final int maxDoc = reader.getMaxDoc();
		final int numDeletes = reader.getNumDeletedDocs();
		final int numDocs = maxDoc - numDeletes;
		
		assert reader.getLiveDocs() != null || numDeletes == 0;
		
		if (numDeletes == 0) {
			return new DocMap.NoDelDocMap(maxDoc);
			
		} else if (numDeletes < numDocs) {
			IIntsMutable numDeletesSoFar = PackedInts.getMutable(maxDoc,
	    			PackedInts.bitsRequired(numDeletes), IIntsReader.COMPACT);
			
			return DocMap.buildDelCountDocmap(numDeletesSoFar, maxDoc, numDeletes, 
					reader.getLiveDocs(), IIntsReader.COMPACT);
			
		} else {
			IIntsMutable docIds = PackedInts.getMutable(maxDoc,
	    			PackedInts.bitsRequired(Math.max(0, numDocs - 1)), 
	    			IIntsReader.COMPACT);
			
			return DocMap.buildDirectDocMap(docIds, maxDoc, numDocs, 
					reader.getLiveDocs(), IIntsReader.COMPACT);
		}
	}
	
	@Override
	protected ISegmentWriteState createSegmentWriteState(MergeState state) { 
		return new SegmentWriteState(state.getSegmentInfo(), state.getFieldInfos(), null);
	}
	
	@Override
	protected IFields createMergeFields(IFields[] fields, ReaderSlice[] slices) { 
		return new MultiFields(fields, slices);
	}
	
}
