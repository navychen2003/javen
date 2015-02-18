package org.javenstudio.falcon.search.hits;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.LongPriorityQueue;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.PrimUtils;
import org.javenstudio.hornet.index.term.DocTermOrds;
import org.javenstudio.hornet.search.OpenBitSet;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.cache.SearchCache;
import org.javenstudio.falcon.search.facet.FacetHelper;
import org.javenstudio.falcon.search.facet.FieldFacetStats;
import org.javenstudio.falcon.search.params.FacetParams;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.falcon.search.schema.TrieFieldType;
import org.javenstudio.falcon.search.stats.StatsValues;
import org.javenstudio.falcon.search.stats.StatsValuesFactory;

/**
 * Final form of the un-inverted field:
 *   Each document points to a list of term numbers that are contained in that document.
 *
 *   Term numbers are in sorted order, and are encoded as variable-length deltas from the
 *   previous term number.  Real term numbers start at 2 since 0 and 1 are reserved.  A
 *   term number of 0 signals the end of the termNumber list.
 *
 *   There is a single int[maxDoc()] which either contains a pointer into a byte[] for
 *   the termNumber lists, or directly contains the termNumber list if it fits in the 4
 *   bytes of an integer.  If the first byte in the integer is 1, the next 3 bytes
 *   are a pointer into a byte[] where the termNumber list starts.
 *
 *   There are actually 256 byte arrays, to compensate for the fact that the pointers
 *   into the byte arrays are only 3 bytes long.  The correct byte array for a document
 *   is a function of it's id.
 *
 *   To save space and speed up faceting, any term that matches enough documents will
 *   not be un-inverted... it will be skipped while building the un-inverted field structure,
 *   and will use a set intersection method during faceting.
 *
 *   To further save memory, the terms (the actual string values) are not all stored in
 *   memory, but a TermIndex is used to convert term numbers to term values only
 *   for the terms needed after faceting has completed.  Only every 128th term value
 *   is stored, along with it's corresponding term number, and this is used as an
 *   index to find the closest term and iterate until the desired number is hit (very
 *   much like indexdb's own internal term index).
 *
 */
public class UnInvertedField extends DocTermOrds {
	private static Logger LOG = Logger.getLogger(UnInvertedField.class);
	
	private static int TNUM_OFFSET = 2;

	static class TopTerm {
		private BytesRef mTerm;
		private int mTermNum;

		private long getMemorySize() {
			return 8 +   // obj header
					8 + 8 + mTerm.getLength() +  //term
					4;    // int
		}
	}

	private final Map<Integer,TopTerm> mBigTerms = new LinkedHashMap<Integer,TopTerm>();
	private final AtomicLong mUse = new AtomicLong(); // number of uses
	private final Searcher mSearcher;
	
	private DocsEnumState mDocsState;
	private int[] mMaxTermCounts = new int[1024];
	private long mMemsz;

	@Override
	protected void visitTerm(ITermsEnum te, int termNum) throws IOException {
		if (termNum >= mMaxTermCounts.length) {
			// resize by doubling - for very large number of unique terms, expanding
			// by 4K and resultant GC will dominate uninvert times.  Resize at end if material
			int[] newMaxTermCounts = new int[mMaxTermCounts.length*2];
			System.arraycopy(mMaxTermCounts, 0, newMaxTermCounts, 0, termNum);
			mMaxTermCounts = newMaxTermCounts;
		}

		final BytesRef term = te.getTerm();

		if (te.getDocFreq() > mMaxTermDocFreq) {
			TopTerm topTerm = new TopTerm();
			topTerm.mTerm = BytesRef.deepCopyOf(term);
			topTerm.mTermNum = termNum;
			
			mBigTerms.put(topTerm.mTermNum, topTerm);

			if (mDocsState == null) {
				mDocsState = new DocsEnumState(mField);
				// mDocsState.fieldName = mField;
				mDocsState.setLiveDocs(mSearcher.getAtomicReader().getLiveDocs());
				// TODO: check for MultiTermsEnum in Searcher could now fail?
				mDocsState.setTermsEnum(te); 
				mDocsState.setDocsEnum(mDocsEnum);
				mDocsState.setMinSetSizeCached(mMaxTermDocFreq);
			}

			mDocsEnum = (DocsEnum)mDocsState.getDocsEnum();
			
			try {
				DocSet set = mSearcher.getDocSet(mDocsState);
				mMaxTermCounts[termNum] = set.size();
				
			} catch (ErrorException ex) { 
				Throwable cause = ex.getCause();
				if (cause != null && cause instanceof IOException) 
					throw (IOException)cause;
				
				throw new IOException(ex);
			}
		}
	}

	@Override
	protected void setActualDocFreq(int termNum, int docFreq) {
		mMaxTermCounts[termNum] = docFreq;
	}

	public long getMemorySize() {
		// can cache the mem size since it shouldn't change
		if (mMemsz != 0) 
			return mMemsz;
		
		long sz = super.ramUsedInBytes();
		sz += 8*8 + 32; // local fields
		sz += mBigTerms.size() * 64;
		
		for (TopTerm tt : mBigTerms.values()) {
			sz += tt.getMemorySize();
		}
		
		if (mMaxTermCounts != null)
			sz += mMaxTermCounts.length * 4;
		
		if (mIndexedTermsArray != null) {
			// assume 8 byte references?
			sz += 8+8+8+8+(mIndexedTermsArray.length<<3)+mSizeOfIndexedStrings;
		}
		
		mMemsz = sz;
		
		return sz;
	}

	public UnInvertedField(String field, Searcher searcher) 
			throws IOException, ErrorException {
		super(field,
				// threshold, over which we use set intersections instead of counting
				// to (1) save memory, and (2) speed up faceting.
				// Add 2 for testing purposes so that there will always be some terms under
				// the threshold even when the index is very
				// small.
				searcher.getMaxDoc()/20 + 2,
				DEFAULT_INDEX_INTERVAL_BITS);

		final String prefix = TrieFieldType.getMainValuePrefix(
				searcher.getSchema().getFieldType(field));
		
		mSearcher = searcher;
		
		uninvert(searcher.getAtomicReader(), 
				(prefix == null) ? null : new BytesRef(prefix));

		if (mTnums != null) {
			for (byte[] target : mTnums) {
				if (target != null && target.length > (1<<24)*.9) {
					LOG.warn("Approaching too many values for UnInvertedField faceting on field '" 
							+ field + "' : bucket size=" + target.length);
				}
			}
		}

		// free space if outrageously wasteful (tradeoff memory/cpu) 
		if ((mMaxTermCounts.length - mNumTermsInField) > 1024) { // too much waste!
			int[] newMaxTermCounts = new int[mNumTermsInField];
			System.arraycopy(mMaxTermCounts, 0, newMaxTermCounts, 0, mNumTermsInField);
			mMaxTermCounts = newMaxTermCounts;
		}

		if (LOG.isDebugEnabled())
			LOG.debug("UnInverted multi-valued field " + toString());
	}

	public int getNumTerms() {
		return mNumTermsInField;
	}

	public NamedList<Integer> getCounts(Searcher searcher, 
			DocSet baseDocs, int offset, int limit, Integer mincount, boolean missing, 
			String sort, String prefix) throws ErrorException {
		try { 
			return doGetCounts(searcher, baseDocs, offset, limit, mincount, missing, sort, prefix);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ex);
		}
	}
	
	private NamedList<Integer> doGetCounts(Searcher searcher, 
			DocSet baseDocs, int offset, int limit, Integer mincount, boolean missing, 
			String sort, String prefix) throws IOException, ErrorException {
		mUse.incrementAndGet();

		SchemaFieldType ft = searcher.getSchema().getFieldType(mField);
		NamedList<Integer> res = new NamedList<Integer>(); // order is important

		DocSet docs = baseDocs;
		int baseSize = docs.size();
		int maxDoc = searcher.getMaxDoc();

		if (baseSize >= mincount) {
			final int[] index = this.mIndex;
			// tricky: we add more more element than we need because we will reuse this array later
			// for ordering term ords before converting to term labels.
			final int[] counts = new int[mNumTermsInField + 1];

			// If there is prefix, find it's start and end term numbers
			int startTerm = 0;
			int endTerm = mNumTermsInField;  // one past the end

			ITermsEnum te = getOrdTermsEnum(searcher.getAtomicReader());
			if (te != null && prefix != null && prefix.length() > 0) {
				final BytesRef prefixBr = new BytesRef(prefix);
				if (te.seekCeil(prefixBr, true) == ITermsEnum.SeekStatus.END) 
					startTerm = mNumTermsInField;
				else 
					startTerm = (int) te.getOrd();
				
				prefixBr.append(UnicodeUtil.BIG_TERM);
				if (te.seekCeil(prefixBr, true) == ITermsEnum.SeekStatus.END) 
					endTerm = mNumTermsInField;
				else 
					endTerm = (int) te.getOrd();
			}

      /***********
      // Alternative 2: get the docSet of the prefix (could take a while) and
      // then do the intersection with the baseDocSet first.
      if (prefix != null && prefix.length() > 0) {
        docs = searcher.getDocSet(new ConstantScorePrefixQuery(new Term(field, ft.toInternal(prefix))), docs);
        // The issue with this method are problems of returning 0 counts for terms w/o
        // the prefix.  We can't just filter out those terms later because it may
        // mean that we didn't collect enough terms in the queue (in the sorted case).
      }
      ***********/

			boolean doNegative = baseSize > maxDoc >> 1 && mTermInstances > 0
					&& startTerm == 0 && endTerm == mNumTermsInField
					&& docs instanceof BitDocSet;

			if (doNegative) {
				OpenBitSet bs = (OpenBitSet)((BitDocSet)docs).getBits().clone();
				bs.flip(0, maxDoc);
				
				// TODO: when iterator across negative elements is available, use that
				// instead of creating a new bitset and inverting.
				docs = new BitDocSet(bs, maxDoc - baseSize);
				// simply negating will mean that we have deleted docs in the set.
				// that should be OK, as their entries in our table should be empty.
			}

			// For the biggest terms, do straight set intersections
			for (TopTerm tt : mBigTerms.values()) {
				// TODO: counts could be deferred if sorted==false
				if (tt.mTermNum >= startTerm && tt.mTermNum < endTerm) {
					counts[tt.mTermNum] = mSearcher.getNumDocs(
							new TermQuery(new Term(mField, tt.mTerm)), docs);
				}
			}

			// TODO: we could short-circuit counting altogether for sorted faceting
			// where we already have enough terms from the bigTerms

			// TODO: we could shrink the size of the collection array, and
			// additionally break when the termNumber got above endTerm, but
			// it would require two extra conditionals in the inner loop (although
			// they would be predictable for the non-prefix case).
			// Perhaps a different copy of the code would be warranted.

			if (mTermInstances > 0) {
				DocIterator iter = docs.iterator();
				while (iter.hasNext()) {
					int doc = iter.nextDoc();
					int code = index[doc];

					if ((code & 0xff) == 1) {
						int pos = code>>>8;
						int whichArray = (doc >>> 16) & 0xff;
						
						byte[] arr = mTnums[whichArray];
						int tnum = 0;
						
						for (;;) {
							int delta = 0;
							for (;;) {
								byte b = arr[pos++];
								delta = (delta << 7) | (b & 0x7f);
								if ((b & 0x80) == 0) break;
							}
							
							if (delta == 0) break;
							tnum += delta - TNUM_OFFSET;
							counts[tnum] ++;
						}
						
					} else {
						int tnum = 0;
						int delta = 0;
						
						for (;;) {
							delta = (delta << 7) | (code & 0x7f);
							
							if ((code & 0x80) == 0) {
								if (delta == 0) break;
								tnum += delta - TNUM_OFFSET;
								counts[tnum] ++;
								delta = 0;
							}
							
							code >>>= 8;
						}
					}
				}
			}
			
			final CharsRef charsRef = new CharsRef();

			int off = offset;
			int lim = limit >= 0 ? limit : Integer.MAX_VALUE;

			if (sort.equals(FacetParams.FACET_SORT_COUNT) || 
				sort.equals(FacetParams.FACET_SORT_COUNT_LEGACY)) {
				
				int maxsize = limit>0 ? offset+limit : Integer.MAX_VALUE-1;
				maxsize = Math.min(maxsize, mNumTermsInField);
				
				LongPriorityQueue queue = new LongPriorityQueue(
						Math.min(maxsize,1000), maxsize, Long.MIN_VALUE);

				int min = mincount-1; // the smallest value in the top 'N' values
				for (int i = startTerm; i < endTerm; i++) {
					int c = doNegative ? mMaxTermCounts[i] - counts[i] : counts[i];
					
					if (c > min) {
						// NOTE: we use c>min rather than c>=min as an optimization because we are going in
						// index order, so we already know that the keys are ordered.  This can be very
						// important if a lot of the counts are repeated (like zero counts would be).

						// smaller term numbers sort higher, so subtract the term number instead
						long pair = (((long)c)<<32) + (Integer.MAX_VALUE - i);
						boolean displaced = queue.insert(pair);
						
						if (displaced) 
							min = (int)(queue.top() >>> 32);
					}
				}

				// now select the right page from the results
				// if we are deep paging, we don't have to order the highest "offset" counts.
				int collectCount = Math.max(0, queue.size() - off);
				assert collectCount <= lim;

				// the start and end indexes of our list "sorted" (starting with the highest value)
				int sortedIdxStart = queue.size() - (collectCount - 1);
				int sortedIdxEnd = queue.size() + 1;
				
				final long[] sorted = queue.sort(collectCount);

				// reuse the counts array for the index into the tnums array
				final int[] indirect = counts; 
				assert indirect.length >= sortedIdxEnd;

				for (int i = sortedIdxStart; i < sortedIdxEnd; i++) {
					long pair = sorted[i];
					int c = (int)(pair >>> 32);
					int tnum = Integer.MAX_VALUE - (int)pair;

					indirect[i] = i;   // store the index for indirect sorting
					sorted[i] = tnum;  // reuse the "sorted" array to store the term numbers for indirect sorting

					// add a null label for now... we'll fill it in later.
					res.add(null, c);
				}

				// now sort the indexes by the term numbers
				PrimUtils.sort(sortedIdxStart, sortedIdxEnd, indirect, 
					new PrimUtils.IntComparator() {
						@Override
						public int compare(int a, int b) {
							return (int)sorted[a] - (int)sorted[b];
						}
	
						@Override
						public boolean lessThan(int a, int b) {
							return sorted[a] < sorted[b];
						}
	
						@Override
						public boolean equals(int a, int b) {
							return sorted[a] == sorted[b];
						}
					});

				// convert the term numbers to term values and set
				// as the label
				for (int i = sortedIdxStart; i < sortedIdxEnd; i++) {
					int idx = indirect[i];
					int tnum = (int)sorted[idx];
					
					final String label = getReadableValue(getTermValue(te, tnum), ft, charsRef);
					res.setName(idx - sortedIdxStart, label);
				}

			} else {
				// add results in index order
				int i = startTerm;
				
				if (mincount <= 0) {
					// if mincount<=0, then we won't discard any terms and we know exactly
					// where to start.
					i = startTerm + off;
					off = 0;
				}

				for (; i < endTerm; i++) {
					int c = doNegative ? mMaxTermCounts[i] - counts[i] : counts[i];
					if (c<mincount || --off >= 0) continue;
					if (--lim < 0) break;

					final String label = getReadableValue(getTermValue(te, i), ft, charsRef);
					res.add(label, c);
				}
			}
		}

		if (missing) {
			// TODO: a faster solution for this?
			res.add(null, FacetHelper.getFieldMissingCount(searcher, baseDocs, mField));
		}

		return res;
	}

	/**
	 * Collect statistics about the UninvertedField.  Code is very similar to 
	 * {@link #getCounts(Searcher, DocSet, int, int, Integer, boolean, String, String)}
	 * It can be used to calculate stats on multivalued fields.
	 * <p/>
	 * This method is mainly used by the {@link StatsComponent}.
	 *
	 * @param searcher The Searcher to use to gather the statistics
	 * @param baseDocs The {@link DocSet} to gather the stats on
	 * @param facet One or more fields to facet on.
	 * @return The {@link StatsValues} collected
	 * @throws IOException If there is a low-level I/O error.
	 */
	public StatsValues getStats(Searcher searcher, DocSet baseDocs, 
			String[] facet) throws ErrorException {
		try { 
			return doGetStats(searcher, baseDocs, facet);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
  
	private StatsValues doGetStats(Searcher searcher, DocSet baseDocs, 
			String[] facet) throws IOException, ErrorException {
		//this function is ripped off nearly wholesale from the getCounts function to use
		//for multiValued fields within the StatsComponent.  may be useful to find common
		//functionality between the two and refactor code somewhat
		mUse.incrementAndGet();

		SchemaField sf = searcher.getSchema().getField(mField);
		// FieldType ft = sf.getType();

		StatsValues allstats = StatsValuesFactory.createStatsValues(sf);
		DocSet docs = baseDocs;
		
		int baseSize = docs.size();
		int maxDoc = searcher.getMaxDoc();

		if (baseSize <= 0) 
			return allstats;

		DocSet missing = docs.andNot(searcher.getDocSet(
				new TermRangeQuery(mField, null, null, false, false)));

		int i = 0;
		final FieldFacetStats[] finfo = new FieldFacetStats[facet.length];
		
		//Initialize facetstats, if facets have been passed in
		IDocTermsIndex si;
		
		for (String f : facet) {
			SchemaField facet_sf = searcher.getSchema().getField(f);
			try {
				si = FieldCache.DEFAULT.getTermsIndex(searcher.getAtomicReader(), f);
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"failed to open field cache for: " + f, e);
			}
			
			finfo[i] = new FieldFacetStats(f, si, sf, facet_sf, mNumTermsInField);
			i++;
		}

		final int[] index = this.mIndex;
		// keep track of the number of times we see each word in the field for all the documents in the docset
		final int[] counts = new int[mNumTermsInField];

		ITermsEnum te = getOrdTermsEnum(searcher.getAtomicReader());

		boolean doNegative = false;
		if (finfo.length == 0) {
			//if we're collecting statistics with a facet field, can't do inverted counting
			doNegative = baseSize > maxDoc >> 1 && mTermInstances > 0
              	&& docs instanceof BitDocSet;
		}

		if (doNegative) {
			OpenBitSet bs = (OpenBitSet) ((BitDocSet) docs).getBits().clone();
			bs.flip(0, maxDoc);
			
			// TODO: when iterator across negative elements is available, use that
			// instead of creating a new bitset and inverting.
			docs = new BitDocSet(bs, maxDoc - baseSize);
			// simply negating will mean that we have deleted docs in the set.
			// that should be OK, as their entries in our table should be empty.
		}

		// For the biggest terms, do straight set intersections
		for (TopTerm tt : mBigTerms.values()) {
			// TODO: counts could be deferred if sorted==false
			if (tt.mTermNum >= 0 && tt.mTermNum < mNumTermsInField) {
				final Term t = new Term(mField, tt.mTerm);
				
				if (finfo.length == 0) {
					counts[tt.mTermNum] = searcher.getNumDocs(new TermQuery(t), docs);
					
				} else {
					//COULD BE VERY SLOW
					//if we're collecting stats for facet fields, we need to iterate on all matching documents
					DocSet bigTermDocSet = searcher.getDocSet(new TermQuery(t)).intersection(docs);
					DocIterator iter = bigTermDocSet.iterator();
					
					while (iter.hasNext()) {
						int doc = iter.nextDoc();
						counts[tt.mTermNum]++;
						
						for (FieldFacetStats f : finfo) {
							f.facetTermNum(doc, tt.mTermNum);
						}
					}
				}
			}
		}

		if (mTermInstances > 0) {
			DocIterator iter = docs.iterator();
			
			while (iter.hasNext()) {
				int doc = iter.nextDoc();
				int code = index[doc];

				if ((code & 0xff) == 1) {
					int pos = code >>> 8;
					int whichArray = (doc >>> 16) & 0xff;
					byte[] arr = mTnums[whichArray];
					int tnum = 0;
					
					for (; ;) {
						int delta = 0;
						for (; ;) {
							byte b = arr[pos++];
							delta = (delta << 7) | (b & 0x7f);
							if ((b & 0x80) == 0) break;
						}
						
						if (delta == 0) break;
						tnum += delta - TNUM_OFFSET;
						counts[tnum]++;
						
						for (FieldFacetStats f : finfo) {
							f.facetTermNum(doc, tnum);
						}
					}
				} else {
					int tnum = 0;
					int delta = 0;
					
					for (; ;) {
						delta = (delta << 7) | (code & 0x7f);
						if ((code & 0x80) == 0) {
							if (delta == 0) break;
							
							tnum += delta - TNUM_OFFSET;
							counts[tnum]++;
							
							for (FieldFacetStats f : finfo) {
								f.facetTermNum(doc, tnum);
							}
							
							delta = 0;
						}
						
						code >>>= 8;
					}
				}
			}
		}
    
		// add results in index order
		for (i = 0; i < mNumTermsInField; i++) {
			int c = doNegative ? mMaxTermCounts[i] - counts[i] : counts[i];
			if (c == 0) continue;
			
			BytesRef value = getTermValue(te, i);
			allstats.accumulate(value, c);
			
			//as we've parsed the termnum into a value, lets also accumulate fieldfacet statistics
			for (FieldFacetStats f : finfo) {
				f.accumulateTermNum(i, value);
			}
		}

		int c = missing.size();
		allstats.addMissing(c);

		if (finfo.length > 0) {
			for (FieldFacetStats f : finfo) {
				Map<String, StatsValues> facetStatsValues = f.getFacetStatsValues();
				SchemaFieldType facetType = searcher.getSchema().getFieldType(f.getName());
				
				for (Map.Entry<String,StatsValues> entry : facetStatsValues.entrySet()) {
					String termLabel = entry.getKey();
					int missingCount = searcher.getNumDocs(
							new TermQuery(new Term(f.getName(), facetType.toInternal(termLabel))), missing);
					
					entry.getValue().addMissing(missingCount);
				}
				
				allstats.addFacet(f.getName(), facetStatsValues);
			}
		}

		return allstats;
	}

	protected String getReadableValue(BytesRef termval, SchemaFieldType ft, 
			CharsRef charsRef) throws ErrorException {
		return ft.indexedToReadable(termval, charsRef).toString();
	}

	/** may return a reused BytesRef */
	protected BytesRef getTermValue(ITermsEnum te, int termNum) throws IOException {
		if (mBigTerms.size() > 0) {
			// see if the term is one of our big terms.
			TopTerm tt = mBigTerms.get(termNum);
			if (tt != null) 
				return tt.mTerm;
		}

		return lookupTerm(te, termNum);
	}

	@Override
	public String toString() {
		// assume 8 byte references?
		final long indexSize = mIndexedTermsArray == null ? 0 : 
			(8+8+8+8+(mIndexedTermsArray.length<<3)+mSizeOfIndexedStrings); 
		
		return "{field=" + mField
            + ",memSize=" + getMemorySize()
            + ",tindexSize=" + indexSize
            + ",time=" + mTotalTime
            + ",phase1=" + mPhase1Time
            + ",nTerms=" + mNumTermsInField
            + ",bigTerms=" + mBigTerms.size()
            + ",termInstances=" + mTermInstances
            + ",uses=" + mUse.get()
            + "}";
	}

	//////////////////////////////////////////////////////////////////
	//////////////////////////// caching /////////////////////////////
	//////////////////////////////////////////////////////////////////
	public static UnInvertedField getUnInvertedField(String field, 
			Searcher searcher) throws ErrorException {
		try {
			SearchCache<String,UnInvertedField> cache = searcher.getFieldValueCache();
			if (cache == null) 
				return new UnInvertedField(field, searcher);
	
			UnInvertedField uif = cache.get(field);
			if (uif == null) {
				synchronized (cache) {
					uif = cache.get(field);
					if (uif == null) {
						uif = new UnInvertedField(field, searcher);
						cache.put(field, uif);
					}
				}
			}
	
			return uif;
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
}
