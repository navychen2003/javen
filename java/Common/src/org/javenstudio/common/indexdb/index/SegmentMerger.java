package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IPayloadProcessor;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.codec.IFieldInfosFormat;
import org.javenstudio.common.indexdb.codec.IFieldsConsumer;
import org.javenstudio.common.indexdb.codec.IFieldsFormat;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;
import org.javenstudio.common.indexdb.index.field.FieldInfosBuilder;
import org.javenstudio.common.indexdb.index.field.FieldNumbers;
import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.util.Logger;

/**
 * The SegmentMerger class combines two or more Segments, represented by 
 * an IndexReader ({@link #add}, into a single Segment.  
 * After adding the appropriate readers, call the merge method to combine 
 * the segments.
 *
 * @see #merge
 * @see #add
 */
public abstract class SegmentMerger {
	private static final Logger LOG = Logger.getLogger(SegmentMerger.class);

	private final IndexWriter mIndexWriter;
	private final MergeState mMergeState;
	private final IDirectory mDirectory;
	private final FieldInfosBuilder mFieldInfosBuilder;
	
	// note, just like in codec apis Directory 'dir' is NOT the same as segmentInfo.dir!!
	public SegmentMerger(IndexWriter indexWriter, 
			ISegmentInfo segmentInfo, IDirectory dir, CheckAbort checkAbort, 
			FieldNumbers globalFieldNumbers) {
		mIndexWriter = indexWriter;
		mDirectory = dir;
		mMergeState = new MergeState(this, segmentInfo, checkAbort);
		mFieldInfosBuilder = new FieldInfosBuilder(globalFieldNumbers);
	}
	
	public final IndexWriter getIndexWriter() { 
		return mIndexWriter;
	}
	
	protected abstract DocMap buildDocMap(IAtomicReader reader);
	protected abstract ISegmentWriteState createSegmentWriteState(MergeState state);
	protected abstract IFields createMergeFields(IFields[] fields, ReaderSlice[] slices);
	
	/**
	 * Add an IndexReader to the collection of readers that are to be merged
	 * @param reader
	 */
	public final void add(IIndexReader reader) {
		for (final IAtomicReaderRef ctx : reader.getReaderContext().getLeaves()) {
			final IAtomicReader r = ctx.getReader();
			mMergeState.addReader(r);
		}
	}

	/**
	 * Merges the readers specified by the {@link #add} method into the directory 
	 * passed to the constructor
	 * @return The number of documents that were merged
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public final MergeState merge() throws IOException {
		// NOTE: it's important to add calls to
		// checkAbort.work(...) if you make any changes to this
		// method that will spend alot of time.  The frequency
		// of this check impacts how long
		// IndexWriter.close(false) takes to actually stop the
		// threads.
		final int docCount = setDocMaps();
		mMergeState.getSegmentInfo().setDocCount(docCount);
		
		mergeFieldInfos();
		setMatchingSegmentReaders();
		
		int numMerged = mergeFields();
		assert numMerged == mMergeState.getSegmentInfo().getDocCount();

		if (LOG.isDebugEnabled())
			LOG.debug("mergeFields: numMerged=" + numMerged + " vs docCount=" + docCount);
		
		final ISegmentWriteState segmentWriteState = createSegmentWriteState(mMergeState);
		mergeTerms(segmentWriteState);
		
		//if (mergeState.fieldInfos.hasNorms()) {
		//    mergeNorms(segmentWriteState);
		//}

		if (mMergeState.getFieldInfos().hasVectors()) {
			numMerged = mergeVectors();
			assert numMerged == mMergeState.getSegmentInfo().getDocCount();
			
			if (LOG.isDebugEnabled())
				LOG.debug("mergeVectors: numMerged=" + numMerged + " vs docCount=" + docCount);
		}
    
		// write the merged infos
		IFieldInfosFormat.Writer fieldInfosWriter = (IFieldInfosFormat.Writer)
				mIndexWriter.getIndexFormat().getFieldInfosFormat().createWriter(mDirectory, 
						mMergeState.getSegmentInfo().getName()); 
		
		fieldInfosWriter.writeFieldInfos(mMergeState.getFieldInfos());

		return mMergeState;
	}
	
	// NOTE: this is actually merging all the fieldinfos
	private void mergeFieldInfos() throws IOException {
	    // mapping from all docvalues fields found to their promoted types
	    // this is because FieldInfos does not store the
	    // valueSize
	    //Map<FieldInfo,TypePromoter> normValuesTypes = new HashMap<FieldInfo,TypePromoter>();

	    for (int i=0; i < mMergeState.getReaderCount(); i++) {
	    	IAtomicReader reader = mMergeState.getReaderAt(i);
	    	IFieldInfos readerFieldInfos = reader.getFieldInfos();
	    	
	    	for (IFieldInfo fi : readerFieldInfos) {
	    		IFieldInfo merged = mFieldInfosBuilder.add(fi);
	    		if (merged != null) {
	    			//if (LOG.isDebugEnabled())
	    			//	LOG.debug("reader[" + i + "]: merged fieldInfo: " + merged.getName());
	    			
		    		// update the type promotion mapping for this reader
		    		if (fi.hasNorms()) {
		    			//TypePromoter previous = normValuesTypes.get(merged);
		    			//normValuesTypes.put(merged, mergeDocValuesType(previous, reader.normValues(fi.name))); 
		    		}
	    		}
	    	}
	    }
	    
	    //updatePromoted(normValuesTypes, true);
	    mMergeState.setFieldInfos(mFieldInfosBuilder.finish());
	}
	
	private void setMatchingSegmentReaders() {
		// If the i'th reader is a SegmentReader and has
		// identical fieldName -> number mapping, then this
		// array will be non-null at position i:
		final int numReaders = mMergeState.getReaderCount();
		mMergeState.initMatchingSegmentReaders(numReaders);

		// If this reader is a SegmentReader, and all of its
		// field name -> number mappings match the "merged"
		// FieldInfos, then we can do a bulk copy of the
		// stored fields:
		for (int i = 0; i < numReaders; i++) {
			IAtomicReader reader = mMergeState.getReaderAt(i);
			
			// TODO: we may be able to broaden this to
			// non-SegmentReaders, since FieldInfos is now
			// required?  But... this'd also require exposing
			// bulk-copy (TVs and stored fields) API in foreign
			// readers..
			setMatchingSegmentReader(mMergeState, i, reader);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("merge store matchedCount=" + mMergeState.getMatchedCount() 
					+ " vs readerCount=" + mMergeState.getReaderCount());
			
			if (mMergeState.getMatchedCount() != mMergeState.getReaderCount()) {
				LOG.debug("  " + (mMergeState.getReaderCount() - mMergeState.getMatchedCount()) 
						+ " non-bulk merges");
			}
		}
	}

	private void setMatchingSegmentReader(MergeState state, int idx, IAtomicReader reader) { 
		// TODO: we may be able to broaden this to
		// non-SegmentReaders, since FieldInfos is now
		// required?  But... this'd also require exposing
		// bulk-copy (TVs and stored fields) API in foreign
		// readers..
		if (reader instanceof ISegmentReader) {
			ISegmentReader segmentReader = (ISegmentReader) reader;
			IFieldInfos segmentFieldInfos = segmentReader.getFieldInfos();
			boolean same = true;
			
			for (IFieldInfo fi : segmentFieldInfos) {
				IFieldInfo other = state.getFieldInfos().getFieldInfo(fi.getNumber());
				if (other == null || !other.getName().equals(fi.getName())) {
					same = false;
					break;
				}
			}
			
			if (same) {
				state.setMatchingSegmentReaderAt(idx, segmentReader);
				state.increaseMatchedCount(1);
			}
		}
	}
	
	/**
	 * @return The number of documents in all of the readers
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	private int mergeFields() throws IOException {
		final IFieldsFormat.Writer fieldsWriter = (IFieldsFormat.Writer)
				mIndexWriter.getIndexFormat().getFieldsFormat().createWriter(mDirectory, 
						mMergeState.getSegmentInfo().getName());
    
		try {
			return fieldsWriter.merge(mMergeState);
		} finally {
			fieldsWriter.close();
		}
	}

	/**
	 * Merge the TermVectors from each of the segments into the new one.
	 * @throws IOException
	 */
	private final int mergeVectors() throws IOException {
		final ITermVectorsFormat.Writer termVectorsWriter = (ITermVectorsFormat.Writer)
				mIndexWriter.getIndexFormat().getTermVectorsFormat().createWriter(mDirectory, 
						mMergeState.getSegmentInfo().getName());
    
		try {
			return termVectorsWriter.merge(mMergeState);
		} finally {
			termVectorsWriter.close();
		}
	}

	// NOTE: removes any "all deleted" readers from mergeState.readers
	private int setDocMaps() throws IOException {
		final int numReaders = mMergeState.getReaderCount();

		// Remap docIDs
		mMergeState.initDocMaps(numReaders);
		mMergeState.initDocBase(numReaders);
		mMergeState.initPayloadProcessorReaders(numReaders);
		mMergeState.initCurrentPayloadProcessors(numReaders);

		int docBase = 0;
		int i = 0;
		
		while (i < mMergeState.getReaderCount()) {
			final IAtomicReader reader = mMergeState.getReaderAt(i);
			mMergeState.setDocBaseAt(i, docBase);
			
			final DocMap docMap = buildDocMap(reader);
			mMergeState.setDocMapAt(i, docMap);
			docBase += docMap.getNumDocs();

			IPayloadProcessor.Provider p = mMergeState.getPayloadProcessorProvider();
			if (p != null) 
				mMergeState.setPayloadProcessorReaderAt(i, p.getProcessorReader(reader));

			i++;
		}

		if (LOG.isDebugEnabled())
			LOG.debug("setDocMaps: numReaders=" + numReaders + " docBase=" + docBase);
		
		return docBase;
	}

	private final void mergeTerms(ISegmentWriteState segmentWriteState) throws IOException {
		final List<IFields> fields = new ArrayList<IFields>();
		final List<ReaderSlice> slices = new ArrayList<ReaderSlice>();

		int docBase = 0;

		for (int readerIndex=0; readerIndex < mMergeState.getReaderCount(); readerIndex++) {
			final IAtomicReader reader = mMergeState.getReaderAt(readerIndex);
			final IFields field = reader.getFields();
			final int maxDoc = reader.getMaxDoc();
			
			if (field != null) {
				ReaderSlice slice = new ReaderSlice(docBase, maxDoc, readerIndex);
				//if (LOG.isDebugEnabled())
				//	LOG.debug("add merge field: " + field + " slice: " + slice);
				
				slices.add(slice);
				fields.add(field);
			}
			
			docBase += maxDoc;
		}

		final IFieldsConsumer consumer = (IFieldsConsumer)
				mIndexWriter.getIndexFormat().getPostingsFormat().getFieldsConsumer(
						mDirectory, segmentWriteState);
		
		boolean success = false;
		try {
			ITermState state = consumer.merge(mMergeState, createMergeFields(
					fields.toArray(new IFields[0]), slices.toArray(new ReaderSlice[0])));
			
			if (LOG.isDebugEnabled())
				LOG.debug("mergeTerms: " + state);
			
			success = true;
		} finally {
			if (success) 
				IOUtils.close(consumer);
			else 
				IOUtils.closeWhileHandlingException(consumer);
		}
	}
	
}
