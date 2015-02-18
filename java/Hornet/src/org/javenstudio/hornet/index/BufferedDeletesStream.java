package org.javenstudio.hornet.index;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.BufferedDeletes;
import org.javenstudio.common.indexdb.index.DeletesStream;
import org.javenstudio.common.indexdb.index.FrozenDeletes;
import org.javenstudio.common.indexdb.index.ReaderPool;
import org.javenstudio.common.indexdb.index.ReadersAndLiveDocs;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.QueryWrapperFilter;
import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * Tracks the stream of {@link BufferedDeletes}.
 * When DocumentsWriterPerThread flushes, its buffered
 * deletes are appended to this stream.  We later
 * apply these deletes (resolve them to the actual
 * docIDs, per segment) when a merge is started
 * (only to the to-be-merged segments).  We
 * also apply to all segments when NRT reader is pulled,
 * commit/close is called, or when too many deletes are
 * buffered and must be flushed (by RAM usage or by count).
 *
 * Each packet is assigned a generation, and each flushed or
 * merged segment is also assigned a generation, so we can
 * track which BufferedDeletes packets to apply to any given
 * segment. 
 */
final class BufferedDeletesStream extends DeletesStream {

	// TODO: maybe linked list?
	private final List<FrozenDeletes> mDeletes = new ArrayList<FrozenDeletes>();

	// Starts at 1 so that SegmentInfos that have never had
	// deletes applied (whose bufferedDelGen defaults to 0)
	// will be correct:
	private long mNextGen = 1;

	// used only by assert
	private ITerm mLastDeleteTerm;

	private final AtomicLong mBytesUsed = new AtomicLong();
	private final AtomicInteger mNumTerms = new AtomicInteger();

	public BufferedDeletesStream() {}

	// Appends a new packet of buffered deletes to the stream,
	// setting its generation:
	@Override
	public synchronized long push(FrozenDeletes packet) {
		/**
		 * The insert operation must be atomic. If we let threads increment the gen
		 * and push the packet afterwards we risk that packets are out of order.
		 * With DWPT this is possible if two or more flushes are racing for pushing
		 * updates. If the pushed packets get our of order would loose documents
		 * since deletes are applied to the wrong segments.
		 */
		packet.setDelGen(mNextGen++);
		
		assert packet.any();
		assert checkDeleteStats();
		assert packet.getDelGen() < mNextGen;
		assert mDeletes.isEmpty() || mDeletes.get(mDeletes.size()-1).getDelGen() < packet.getDelGen() : 
			"Delete packets must be in order";
		
		mDeletes.add(packet);
		mNumTerms.addAndGet(packet.getNumTermDeletes());
		mBytesUsed.addAndGet(packet.getBytesUsed());

		assert checkDeleteStats();
		return packet.getDelGen();
	}

	@Override
	public synchronized void clear() {
		mDeletes.clear();
		mNextGen = 1;
		mNumTerms.set(0);
		mBytesUsed.set(0);
	}

	@Override
	public boolean any() {
		return mBytesUsed.get() != 0;
	}

	@Override
	public int getNumTerms() {
		return mNumTerms.get();
	}

	@Override
	public long getBytesUsed() {
		return mBytesUsed.get();
	}

	// Sorts SegmentInfos from smallest to biggest bufferedDelGen:
	private static final Comparator<ISegmentCommitInfo> sSortSegInfoByDelGen = 
		new Comparator<ISegmentCommitInfo>() {
			@Override
			public int compare(ISegmentCommitInfo si1, ISegmentCommitInfo si2) {
				final long cmp = si1.getBufferedDeletesGen() - si2.getBufferedDeletesGen();
				if (cmp > 0) 
					return 1;
				else if (cmp < 0) 
					return -1;
				else 
					return 0;
			}
		};
  
	/** 
	 * Resolves the buffered deleted Term/Query/docIDs, into
	 *  actual deleted docIDs in the liveDocs MutableBits for
	 *  each SegmentReader. 
	 */
	@Override
	public synchronized ApplyResult applyDeletes(
			ReaderPool readerPool, List<ISegmentCommitInfo> infos) 
			throws IOException {
		//final long t0 = System.currentTimeMillis();
		if (infos.size() == 0) 
			return new ApplyResult(false, mNextGen++, null);

		assert checkDeleteStats();
		if (!any()) 
			return new ApplyResult(false, mNextGen++, null);

		List<ISegmentCommitInfo> infos2 = new ArrayList<ISegmentCommitInfo>();
		infos2.addAll(infos);
		Collections.sort(infos2, sSortSegInfoByDelGen);

		CoalescedDeletes coalescedDeletes = null;
		boolean anyNewDeletes = false;

		int infosIDX = infos2.size()-1;
		int delIDX = mDeletes.size()-1;

		List<ISegmentCommitInfo> allDeleted = null;

		while (infosIDX >= 0) {
			final FrozenDeletes packet = (delIDX >= 0) ? mDeletes.get(delIDX) : null;
			final ISegmentCommitInfo info = infos2.get(infosIDX);
			final long segGen = info.getBufferedDeletesGen();

			if (packet != null && segGen < packet.getDelGen()) {
				if (coalescedDeletes == null) 
					coalescedDeletes = new CoalescedDeletes();
				
				if (!packet.isSegmentPrivate()) {
					/**
					 * Only coalesce if we are NOT on a segment private del packet: 
					 * the segment private del packet
					 * must only applied to segments with the same delGen.  
					 * Yet, if a segment is already deleted
					 * from the SI since it had no more documents remaining after 
					 * some del packets younger than
					 * its segPrivate packet (higher delGen) have been applied, 
					 * the segPrivate packet has not been removed.
					 */
					coalescedDeletes.update(packet);
				}

				delIDX --;
				
			} else if (packet != null && segGen == packet.getDelGen()) {
				assert packet.isSegmentPrivate() : "Packet and Segments deletegen can only " 
						+ "match on a segment private del packet gen=" + segGen;

				// Lock order: IW -> BD -> RP
				//assert readerPool.infoIsLive(info);
				
				final ReadersAndLiveDocs rld = readerPool.get(info, true);
				final ISegmentReader reader = rld.getReader();
				final boolean segAllDeletes;
				int delCount = 0;
				
				try {
					if (coalescedDeletes != null) {
						delCount += applyTermDeletes(coalescedDeletes.termsIterable(), rld, reader);
						delCount += applyQueryDeletes(coalescedDeletes.queriesIterable(), rld, reader);
					}
					
					// Don't delete by Term here; DocumentsWriterPerThread
					// already did that on flush:
					delCount += applyQueryDeletes(packet.queriesIterable(), rld, reader);
					
					final int fullDelCount = rld.getCommitInfo().getDelCount() + rld.getPendingDeleteCount();
					assert fullDelCount <= rld.getCommitInfo().getSegmentInfo().getDocCount();
					
					segAllDeletes = fullDelCount == rld.getCommitInfo().getSegmentInfo().getDocCount();
					
				} finally {
					rld.release(reader);
					readerPool.release(rld);
				}
				
				anyNewDeletes |= delCount > 0;

				if (segAllDeletes) {
					if (allDeleted == null) 
						allDeleted = new ArrayList<ISegmentCommitInfo>();
					
					allDeleted.add(info);
				}

				if (coalescedDeletes == null) 
					coalescedDeletes = new CoalescedDeletes();
        
				/**
				 * Since we are on a segment private del packet we must not
				 * update the coalescedDeletes here! We can simply advance to the 
				 * next packet and seginfo.
				 */
				delIDX --;
				infosIDX --;
				info.setBufferedDeletesGen(mNextGen);

			} else {
				if (coalescedDeletes != null) {
					// Lock order: IW -> BD -> RP
					//assert readerPool.infoIsLive(info);
					
					final ReadersAndLiveDocs rld = readerPool.get(info, true);
					final ISegmentReader reader = rld.getReader();
					final boolean segAllDeletes;
					int delCount = 0;
					
					try {
						delCount += applyTermDeletes(coalescedDeletes.termsIterable(), rld, reader);
						delCount += applyQueryDeletes(coalescedDeletes.queriesIterable(), rld, reader);
						
						final int fullDelCount = rld.getCommitInfo().getDelCount() + rld.getPendingDeleteCount();
						assert fullDelCount <= rld.getCommitInfo().getSegmentInfo().getDocCount();
						
						segAllDeletes = fullDelCount == rld.getCommitInfo().getSegmentInfo().getDocCount();
						
					} finally {   
						rld.release(reader);
						readerPool.release(rld);
					}
					
					anyNewDeletes |= delCount > 0;

					if (segAllDeletes) {
						if (allDeleted == null) 
							allDeleted = new ArrayList<ISegmentCommitInfo>();
						
						allDeleted.add(info);
					}
				}
				
				info.setBufferedDeletesGen(mNextGen);
				infosIDX --;
			}
		}

		assert checkDeleteStats();

		return new ApplyResult(anyNewDeletes, mNextGen++, allDeleted);
	}

	@Override
	public synchronized long nextGen() {
		return mNextGen ++;
	}

	// Lock order IW -> BD
	/** 
	 * Removes any BufferedDeletes that we no longer need to
	 * store because all segments in the index have had the
	 * deletes applied. 
	 */
	@Override
	public synchronized void prune(ISegmentInfos segmentInfos) {
		assert checkDeleteStats();
		
		long minGen = Long.MAX_VALUE;
		for (ISegmentCommitInfo info : segmentInfos) {
			minGen = Math.min(info.getBufferedDeletesGen(), minGen);
		}

		final int limit = mDeletes.size();
		for (int delIDX=0; delIDX < limit; delIDX++) {
			if (mDeletes.get(delIDX).getDelGen() >= minGen) {
				prune(delIDX);
				assert checkDeleteStats();
				return;
			}
		}

		// All deletes pruned
		prune(limit);
		assert !any();
		assert checkDeleteStats();
	}

	private synchronized void prune(int count) {
		if (count > 0) {
			for (int delIDX=0; delIDX < count; delIDX++) {
				final FrozenDeletes packet = mDeletes.get(delIDX);
				
				mNumTerms.addAndGet(-packet.getNumTermDeletes());
				assert mNumTerms.get() >= 0;
				
				mBytesUsed.addAndGet(-packet.getBytesUsed());
				assert mBytesUsed.get() >= 0;
			}
			
			mDeletes.subList(0, count).clear();
		}
	}

	// Delete by Term
	private synchronized long applyTermDeletes(Iterable<ITerm> termsIter, 
			ReadersAndLiveDocs rld, ISegmentReader reader) throws IOException {
		long delCount = 0;
		IFields fields = reader.getFields();
		if (fields == null) {
			// This reader has no postings
			return 0;
		}

		ITermsEnum termsEnum = null;
		String currentField = null;
		IDocsEnum docs = null;
		boolean any = false;
		
		assert checkDeleteTerm(null);

		for (ITerm term : termsIter) {
			// Since we visit terms sorted, we gain performance
			// by re-using the same TermsEnum and seeking only
			// forwards
			if (!term.getField().equals(currentField)) {
				assert currentField == null || currentField.compareTo(term.getField()) < 0;
				currentField = term.getField();
				ITerms terms = fields.getTerms(currentField);
				if (terms != null) 
					termsEnum = terms.iterator(null);
				else 
					termsEnum = null;
			}

			if (termsEnum == null) 
				continue;
			
			assert checkDeleteTerm(term);

			if (termsEnum.seekExact(term.getBytes(), false)) {
				IDocsEnum docsEnum = termsEnum.getDocs(rld.getLiveDocs(), docs, 0);

				if (docsEnum != null) {
					while (true) {
						final int docID = docsEnum.nextDoc();
						if (docID == IDocIdSetIterator.NO_MORE_DOCS) 
							break;
						
						// NOTE: there is no limit check on the docID
						// when deleting by Term (unlike by Query)
						// because on flush we apply all Term deletes to
						// each segment.  So all Term deleting here is
						// against prior segments:
						if (!any) {
							rld.initWritableLiveDocs();
							any = true;
						}
						
						if (rld.delete(docID)) 
							delCount ++;
					}
				}
			}
		}

		return delCount;
	}

	// Delete by query
	private static long applyQueryDeletes(Iterable<QueryAndLimit> queriesIter, 
			ReadersAndLiveDocs rld, final ISegmentReader reader) throws IOException {
		final IAtomicReaderRef readerContext = (IAtomicReaderRef)reader.getReaderContext();
		long delCount = 0;
		boolean any = false;
		
		for (QueryAndLimit ent : queriesIter) {
			IQuery query = ent.getQuery();
			int limit = ent.getLimit();
			
			final IDocIdSet docs = new QueryWrapperFilter(query)
				.getDocIdSet(readerContext, reader.getLiveDocs());
			
			if (docs != null) {
				final IDocIdSetIterator it = docs.iterator();
				if (it != null) {
					while (true)  {
						int doc = it.nextDoc();
						if (doc >= limit) 
							break;

						if (!any) {
							rld.initWritableLiveDocs();
							any = true;
						}

						if (rld.delete(doc)) 
							delCount ++;
					}
				}
			}
		}

		return delCount;
	}

	// used only by assert
	private boolean checkDeleteTerm(ITerm term) {
		if (term != null) {
			assert mLastDeleteTerm == null || term.compareTo(mLastDeleteTerm) > 0: 
				"lastTerm=" + mLastDeleteTerm + " vs term=" + term;
		}
		
		// TODO: we re-use term now in our merged iterable, but we shouldn't clone, 
		// instead copy for this assert
		mLastDeleteTerm = (term == null) ? null : 
			new Term(term.getField(), BytesRef.deepCopyOf(term.getBytes()));
		
		return true;
	}

	// only for assert
	private boolean checkDeleteStats() {
		int numTerms2 = 0;
		long bytesUsed2 = 0;
		
		for (FrozenDeletes packet : mDeletes) {
			numTerms2 += packet.getNumTermDeletes();
			bytesUsed2 += packet.getBytesUsed();
		}
		
		assert numTerms2 == mNumTerms.get(): "numTerms2=" + numTerms2 + " vs " + mNumTerms.get();
		assert bytesUsed2 == mBytesUsed.get(): "bytesUsed2=" + bytesUsed2 + " vs " + mBytesUsed;
		
		return true;
	}
	
}
