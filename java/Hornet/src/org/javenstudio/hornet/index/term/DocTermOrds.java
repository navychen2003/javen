package org.javenstudio.hornet.index.term;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocTermOrds;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.store.PagedBytes;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * This class enables fast access to multiple term ords for
 * a specified field across all docIDs.
 *
 * Like FieldCache, it uninverts the index and holds a
 * packed data structure in RAM to enable fast access.
 * Unlike FieldCache, it can handle multi-valued fields,
 * and, it does not hold the term bytes in RAM.  Rather, you
 * must obtain a TermsEnum from the {@link #getOrdTermsEnum}
 * method, and then seek-by-ord to get the term's bytes.
 *
 * While normally term ords are type long, in this API they are
 * int as the internal representation here cannot address
 * more than MAX_INT unique terms.  Also, typically this
 * class is used on fields with relatively few unique terms
 * vs the number of documents.  In addition, there is an
 * internal limit (16 MB) on how many bytes each chunk of
 * documents may consume.  If you trip this limit you'll hit
 * an IllegalStateException.
 *
 * Deleted documents are skipped during uninversion, and if
 * you look them up you'll get 0 ords.
 *
 * The returned per-document ords do not retain their
 * original order in the document.  Instead they are returned
 * in sorted (by ord, ie term's BytesRef comparator) order.  They
 * are also de-dup'd (ie if doc has same term more than once
 * in this field, you'll only get that ord back once).
 *
 * This class tests whether the provided reader is able to
 * retrieve terms by ord (ie, it's single segment, and it
 * uses an ord-capable terms index).  If not, this class
 * will create its own term index internally, allowing to
 * create a wrapped TermsEnum that can handle ord.  The
 * {@link #getOrdTermsEnum} method then provides this
 * wrapped enum, if necessary.
 *
 * The RAM consumption of this class can be high!
 *
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
 *   much like Lucene's own internal term index).
 *
 */
public class DocTermOrds implements IDocTermOrds {

	// Term ords are shifted by this, internally, to reserve
	// values 0 (end term) and 1 (index is a pointer into byte array)
	private final static int TNUM_OFFSET = 2;

	// Default: every 128th term is indexed
	// decrease to a low number like 2 for testing
	public final static int DEFAULT_INDEX_INTERVAL_BITS = 7; 

	private int mIndexIntervalBits;
	private int mIndexIntervalMask;
	private int mIndexInterval;
	private long mMemsz;

	protected final int mMaxTermDocFreq;
	protected final String mField;

	protected int mNumTermsInField;
	protected long mTermInstances; 	// total number of references to term numbers
	protected int mTotalTime;  		// total time to uninvert the field
	protected int mPhase1Time;  	// time for phase1 of the uninvert process

	protected int[] mIndex;
	protected byte[][] mTnums = new byte[256][];
	protected long mSizeOfIndexedStrings;
	protected BytesRef[] mIndexedTermsArray;
	protected BytesRef mPrefix;
	protected int mOrdBase;

	protected DocsEnum mDocsEnum; //used while uninverting

	/** Inverts all terms */
	public DocTermOrds(IAtomicReader reader, String field) throws IOException {
		this(reader, field, null, Integer.MAX_VALUE);
	}

	/** Inverts only terms starting w/ prefix */
	public DocTermOrds(IAtomicReader reader, String field, BytesRef termPrefix) 
			throws IOException {
		this(reader, field, termPrefix, Integer.MAX_VALUE);
	}

	/** 
	 * Inverts only terms starting w/ prefix, and only terms
	 *  whose docFreq (not taking deletions into account) is
	 *  <=  maxTermDocFreq 
	 */
	public DocTermOrds(IAtomicReader reader, String field, BytesRef termPrefix, 
			int maxTermDocFreq) throws IOException {
		this(reader, field, termPrefix, maxTermDocFreq, DEFAULT_INDEX_INTERVAL_BITS);
		uninvert(reader, termPrefix);
	}

	/** 
	 * Inverts only terms starting w/ prefix, and only terms
	 *  whose docFreq (not taking deletions into account) is
	 *  <=  maxTermDocFreq, with a custom indexing interval
	 *  (default is every 128nd term). 
	 */
	public DocTermOrds(IAtomicReader reader, String field, BytesRef termPrefix, 
			int maxTermDocFreq, int indexIntervalBits) throws IOException {
		this(field, maxTermDocFreq, indexIntervalBits);
		uninvert(reader, termPrefix);
	}

	/** 
	 * Subclass inits w/ this, but be sure you then call
	 *  uninvert, only once 
	 */
	protected DocTermOrds(String field, int maxTermDocFreq, int indexIntervalBits) 
			throws IOException {
		mField = field;
		mMaxTermDocFreq = maxTermDocFreq;
		mIndexIntervalBits = indexIntervalBits;
		mIndexIntervalMask = 0xffffffff >>> (32-indexIntervalBits);
		mIndexInterval = 1 << indexIntervalBits;
	}

	public long ramUsedInBytes() {
		// can cache the mem size since it shouldn't change
		if (mMemsz != 0) return mMemsz;
		long sz = 8*8 + 32; // local fields
		if (mIndex != null) sz += mIndex.length * 4;
		if (mTnums != null) {
			for (byte[] arr : mTnums) {
				if (arr != null) sz += arr.length;
			}
		}
		mMemsz = sz;
		return sz;
	}
	
	/** 
	 * Returns a TermsEnum that implements ord.  If the
	 *  provided reader supports ord, we just return its
	 *  TermsEnum; if it does not, we build a "private" terms
	 *  index internally (WARNING: consumes RAM) and use that
	 *  index to implement ord.  This also enables ord on top
	 *  of a composite reader.  The returned TermsEnum is
	 *  unpositioned.  This returns null if there are no terms.
	 *
	 *  <p><b>NOTE</b>: you must pass the same reader that was
	 *  used when creating this class 
	 */
	@Override
	public ITermsEnum getOrdTermsEnum(IAtomicReader reader) throws IOException {
		if (mIndexedTermsArray == null) {
			final IFields fields = reader.getFields();
			if (fields == null) 
				return null;
			
			final ITerms terms = fields.getTerms(mField);
			if (terms == null) 
				return null;
			
			return (TermsEnum)terms.iterator(null);
		}
		
		return new OrdWrappedTermsEnum(reader);
	}

	/**
	 * @return The number of terms in this field
	 */
	@Override
	public int getNumTerms() {
		return mNumTermsInField;
	}

	/**
	 * @return Whether this <code>DocTermOrds</code> instance is empty.
	 */
	@Override
	public boolean isEmpty() {
		return mIndex == null;
	}

	/** Subclass can override this */
	protected void visitTerm(ITermsEnum te, int termNum) throws IOException {
		// do nothing
	}

	protected void setActualDocFreq(int termNum, int df) throws IOException {
		// do nothing
	}

	// Call this only once (if you subclass!)
	protected void uninvert(final IAtomicReader reader, final BytesRef termPrefix) throws IOException {
		final long startTime = System.currentTimeMillis();
		mPrefix = (termPrefix == null) ? null : BytesRef.deepCopyOf(termPrefix);

		final int maxDoc = reader.getMaxDoc();
		final int[] index = new int[maxDoc];       // immediate term numbers, or the index into the byte[] representing the last number
		final int[] lastTerm = new int[maxDoc];    // last term we saw for this document
		final byte[][] bytes = new byte[maxDoc][]; // list of term numbers for the doc (delta encoded vInts)

		final IFields fields = reader.getFields();
		if (fields == null) 
			return; // No terms
		
		final ITerms terms = fields.getTerms(mField);
		if (terms == null) 
			return; // No terms

		final ITermsEnum te = terms.iterator(null);
		final BytesRef seekStart = termPrefix != null ? termPrefix : new BytesRef();
		
		if (te.seekCeil(seekStart) == TermsEnum.SeekStatus.END) 
			return; // No terms match

		// If we need our "term index wrapper", these will be
		// init'd below:
		List<BytesRef> indexedTerms = null;
		PagedBytes indexedTermsBytes = null;

		boolean testedOrd = false;
		final Bits liveDocs = reader.getLiveDocs();

		// we need a minimum of 9 bytes, but round up to 12 since the space would
		// be wasted with most allocators anyway.
		byte[] tempArr = new byte[12];

		//
		// enumerate all terms, and build an intermediate form of the un-inverted field.
		//
		// During this intermediate form, every document has a (potential) byte[]
		// and the int[maxDoc()] array either contains the termNumber list directly
		// or the *end* offset of the termNumber list in it's byte array (for faster
		// appending and faster creation of the final form).
		//
		// idea... if things are too large while building, we could do a range of docs
		// at a time (but it would be a fair amount slower to build)
		// could also do ranges in parallel to take advantage of multiple CPUs

		// OPTIONAL: remap the largest df terms to the lowest 128 (single byte)
		// values.  This requires going over the field first to find the most
		// frequent terms ahead of time.

		int termNum = 0;
		mDocsEnum = null;

		// Loop begins with te positioned to first term (we call
		// seek above):
		for (;;) {
			final BytesRef t = te.getTerm();
			if (t == null || (termPrefix != null && !StringHelper.startsWith(t, termPrefix))) 
				break;
			
			if (!testedOrd) {
				try {
					mOrdBase = (int) te.getOrd();
				} catch (UnsupportedOperationException uoe) {
					// Reader cannot provide ord support, so we wrap
					// our own support by creating our own terms index:
					indexedTerms = new ArrayList<BytesRef>();
					indexedTermsBytes = new PagedBytes(15);
				}
				testedOrd = true;
			}

			visitTerm(te, termNum);

			if (indexedTerms != null && (termNum & mIndexIntervalMask) == 0) {
				// Index this term
				mSizeOfIndexedStrings += t.mLength;
				BytesRef indexedTerm = new BytesRef();
				indexedTermsBytes.copy(t, indexedTerm);
				// TODO: really should 1) strip off useless suffix,
				// and 2) use FST not array/PagedBytes
				indexedTerms.add(indexedTerm);
			}

			final int df = te.getDocFreq();
			if (df <= mMaxTermDocFreq) {
				mDocsEnum = (DocsEnum)te.getDocs(liveDocs, mDocsEnum, 0);
				// dF, but takes deletions into account
				int actualDF = 0;

				for (;;) {
					int doc = mDocsEnum.nextDoc();
					if (doc == IDocIdSetIterator.NO_MORE_DOCS) 
						break;
					
					actualDF ++;
					mTermInstances ++;
          
					// add TNUM_OFFSET to the term number to make room for special reserved values:
					// 0 (end term) and 1 (index into byte array follows)
					int delta = termNum - lastTerm[doc] + TNUM_OFFSET;
					lastTerm[doc] = termNum;
					int val = index[doc];

					if ((val & 0xff) == 1) {
						// index into byte array (actually the end of
						// the doc-specific byte[] when building)
						int pos = val >>> 8;
		            	int ilen = vIntSize(delta);
		            	byte[] arr = bytes[doc];
		            	int newend = pos + ilen;
		            	
		            	if (newend > arr.length) {
		            		// We avoid a doubling strategy to lower memory usage.
		            		// this faceting method isn't for docs with many terms.
		            		// In hotspot, objects have 2 words of overhead, then fields, rounded up to a 64-bit boundary.
		            		// TODO: figure out what array lengths we can round up to w/o actually using more memory
		            		// (how much space does a byte[] take up?  Is data preceded by a 32 bit length only?
		            		// It should be safe to round up to the nearest 32 bits in any case.
		            		int newLen = (newend + 3) & 0xfffffffc;  // 4 byte alignment
		            		byte[] newarr = new byte[newLen];
		            		System.arraycopy(arr, 0, newarr, 0, pos);
		            		arr = newarr;
		            		bytes[doc] = newarr;
		            	}
		            	
		            	pos = writeInt(delta, arr, pos);
		            	index[doc] = (pos<<8) | 1;  // update pointer to end index in byte[]
		            	
					} else {
						// OK, this int has data in it... find the end (a zero starting byte - not
						// part of another number, hence not following a byte with the high bit set).
						int ipos;
						if (val == 0) 
							ipos = 0;
						else if ((val & 0x0000ff80) == 0) 
							ipos = 1;
						else if ((val & 0x00ff8000) == 0) 
							ipos = 2;
						else if ((val & 0xff800000) == 0) 
							ipos = 3;
						else 
							ipos = 4;
		
						int endPos = writeInt(delta, tempArr, ipos);
						
						if (endPos <= 4) {
							// value will fit in the integer... move bytes back
							for (int j=ipos; j < endPos; j++) {
								val |= (tempArr[j] & 0xff) << (j<<3);
							}
							index[doc] = val;
							
						} else {
							// value won't fit... move integer into byte[]
							for (int j=0; j < ipos; j++) {
								tempArr[j] = (byte)val;
								val >>>= 8;
							}
							
							// point at the end index in the byte[]
							index[doc] = (endPos<<8) | 1;
							bytes[doc] = tempArr;
							tempArr = new byte[12];
						}
					}
				}
		        setActualDocFreq(termNum, actualDF);
			}

			termNum ++;
			if (te.next() == null) 
				break;
		}

		mNumTermsInField = termNum;

		long midPoint = System.currentTimeMillis();

		if (mTermInstances == 0) {
			// we didn't invert anything
			// lower memory consumption.
			mTnums = null;
			
		} else {
			mIndex = index;

			//
			// transform intermediate form into the final form, building a single byte[]
			// at a time, and releasing the intermediate byte[]s as we go to avoid
			// increasing the memory footprint.
			//

			for (int pass = 0; pass < 256; pass++) {
				byte[] target = mTnums[pass];
				int pos = 0;  // end in target;
				if (target != null) 
					pos = target.length;
				else 
					target = new byte[4096];

				// loop over documents, 0x00ppxxxx, 0x01ppxxxx, 0x02ppxxxx
				// where pp is the pass (which array we are building), and xx is all values.
				// each pass shares the same byte[] for termNumber lists.
				for (int docbase = pass<<16; docbase < maxDoc; docbase += (1<<24)) {
					int lim = Math.min(docbase + (1<<16), maxDoc);
					
					for (int doc=docbase; doc < lim; doc++) {
						int val = index[doc];
						if ((val&0xff) == 1) {
							int len = val >>> 8;
					
							index[doc] = (pos<<8)|1; // change index to point to start of array
							if ((pos & 0xff000000) != 0) {
								// we only have 24 bits for the array index
								throw new IllegalStateException(
										"Too many values for UnInvertedField faceting on field " + mField);
							}
							
							byte[] arr = bytes[doc];
							bytes[doc] = null; // IMPORTANT: allow GC to avoid OOM
							
							if (target.length <= pos + len) {
								int newlen = target.length;
								
								/*** we don't have to worry about the array getting too large
								 * since the "pos" param will overflow first (only 24 bits available)
		                		if ((newlen<<1) <= 0) {
			                  		// overflow...
			                  		newlen = Integer.MAX_VALUE;
			                  		if (newlen <= pos + len) {
			                    		throw new SolrException(400,"Too many terms to uninvert field!");
			                  		}
		                		} else {
		                  			while (newlen <= pos + len) newlen<<=1;  // doubling strategy
		                		}
								 ****/
								
								while (newlen <= pos + len) newlen<<=1;  // doubling strategy 
								
								byte[] newtarget = new byte[newlen];
								System.arraycopy(target, 0, newtarget, 0, pos);
								target = newtarget;
							}
							
							System.arraycopy(arr, 0, target, pos, len);
							pos += len + 1;  // skip single byte at end and leave it 0 for terminator
						}
					}
				}

				// shrink array
				if (pos < target.length) {
					byte[] newtarget = new byte[pos];
					System.arraycopy(target, 0, newtarget, 0, pos);
					target = newtarget;
				}
	        
				mTnums[pass] = target;
	
				if ((pass << 16) > maxDoc)
					break;
			}
		}
		
		if (indexedTerms != null) 
			mIndexedTermsArray = indexedTerms.toArray(new BytesRef[indexedTerms.size()]);

		long endTime = System.currentTimeMillis();

		mTotalTime = (int)(endTime - startTime);
		mPhase1Time = (int)(midPoint - startTime);
	}

	/** Number of bytes to represent an unsigned int as a vint. */
	private static int vIntSize(int x) {
		if ((x & (0xffffffff << (7*1))) == 0 ) 
			return 1;
		
		if ((x & (0xffffffff << (7*2))) == 0 ) 
			return 2;
		
		if ((x & (0xffffffff << (7*3))) == 0 ) 
			return 3;
		
		if ((x & (0xffffffff << (7*4))) == 0 ) 
			return 4;
		
		return 5;
	}

	// todo: if we know the size of the vInt already, we could do
	// a single switch on the size
	private static int writeInt(int x, byte[] arr, int pos) {
		int a;
		a = (x >>> (7*4));
		if (a != 0) {
			arr[pos++] = (byte)(a | 0x80);
		}
		a = (x >>> (7*3));
		if (a != 0) {
			arr[pos++] = (byte)(a | 0x80);
		}
		a = (x >>> (7*2));
		if (a != 0) {
			arr[pos++] = (byte)(a | 0x80);
		}
		a = (x >>> (7*1));
		if (a != 0) {
			arr[pos++] = (byte)(a | 0x80);
		}
		arr[pos++] = (byte)(x & 0x7f);
		return pos;
	}

	public class TermOrdsIterator {
		private int mTnum;
		private int mUpto;
		private byte[] mArr;

		/** 
		 * Buffer must be at least 5 ints long.  Returns number
		 *  of term ords placed into buffer; if this count is
		 *  less than buffer.length then that is the end. 
		 */
		public int read(int[] buffer) {
			int bufferUpto = 0;
			if (mArr == null) {
				// code is inlined into upto
				int code = mUpto;
				int delta = 0;
				for (;;) {
					delta = (delta << 7) | (code & 0x7f);
					if ((code & 0x80)==0) {
						if (delta == 0) break;
						mTnum += delta - TNUM_OFFSET;
						buffer[bufferUpto++] = mOrdBase + mTnum;
						delta = 0;
					}
					code >>>= 8;
				}
			} else {
				// code is a pointer
				for (;;) {
					int delta = 0;
					for (;;) {
						byte b = mArr[mUpto++];
						delta = (delta << 7) | (b & 0x7f);
						if ((b & 0x80) == 0) break;
					}
					
					if (delta == 0) break;
					
					mTnum += delta - TNUM_OFFSET;
					buffer[bufferUpto++] = mOrdBase + mTnum;
					
					if (bufferUpto == buffer.length) 
						break;
				}
			}

			return bufferUpto;
		}

		public TermOrdsIterator reset(int docID) {
			mTnum = 0;
			final int code = mIndex[docID];
			
			if ((code & 0xff)==1) {
				// a pointer
				mUpto = code >>> 8;
				int whichArray = (docID >>> 16) & 0xff;
				mArr = mTnums[whichArray];
				
			} else {
				mArr = null;
				mUpto = code;
			}
			
			return this;
		}
	}

	/** 
	 * Returns an iterator to step through the term ords for
	 *  this document.  It's also possible to subclass this
	 *  class and directly access members. 
	 */
	public TermOrdsIterator lookup(int doc, TermOrdsIterator reuse) {
		final TermOrdsIterator ret;
		if (reuse != null) 
			ret = reuse;
		else 
			ret = new TermOrdsIterator();
		
		return ret.reset(doc);
	}

	/** 
	 * Only used if original IndexReader doesn't implement
	 * ord; in this case we "wrap" our own terms index
	 * around it. 
	 */
	private final class OrdWrappedTermsEnum extends TermsEnum {
		private final TermsEnum mTermsEnum;
		private BytesRef mTerm;
		private long mOrd = -mIndexInterval-1;	// force "real" seek
    
		public OrdWrappedTermsEnum(IAtomicReader reader) throws IOException {
			assert mIndexedTermsArray != null;
			mTermsEnum = (TermsEnum)reader.getFields().getTerms(mField).iterator(null);
		}

		@Override
		public Comparator<BytesRef> getComparator() {
			return mTermsEnum.getComparator();
		}

		@Override 
		public IDocsEnum getDocs(Bits liveDocs, IDocsEnum reuse, int flags) 
				throws IOException {
			return mTermsEnum.getDocs(liveDocs, reuse, flags);
		}

		@Override
		public IDocsAndPositionsEnum getDocsAndPositions(Bits liveDocs, 
				IDocsAndPositionsEnum reuse, int flags) throws IOException {
			return mTermsEnum.getDocsAndPositions(liveDocs, reuse, flags);
		}

		@Override
		public BytesRef getTerm() {
			return mTerm;
		}

		@Override
		public BytesRef next() throws IOException {
			mOrd ++;
			if (mTermsEnum.next() == null) {
				mTerm = null;
				return null;
			}
			return setTerm();  // this is extra work if we know we are in bounds...
		}

		@Override
		public int getDocFreq() throws IOException {
			return mTermsEnum.getDocFreq();
		}

		@Override
		public long getTotalTermFreq() throws IOException {
			return mTermsEnum.getTotalTermFreq();
		}

		@Override
		public long getOrd() throws IOException {
			return mOrdBase + mOrd;
		}

		@Override
		public SeekStatus seekCeil(BytesRef target, boolean useCache) throws IOException {
			// already here
			if (mTerm != null && mTerm.equals(target)) 
				return SeekStatus.FOUND;

			int startIdx = Arrays.binarySearch(mIndexedTermsArray, target);
			if (startIdx >= 0) {
				// we hit the term exactly... lucky us!
				TermsEnum.SeekStatus seekStatus = mTermsEnum.seekCeil(target);
				assert seekStatus == TermsEnum.SeekStatus.FOUND;
				mOrd = startIdx << mIndexIntervalBits;
				setTerm();
				assert mTerm != null;
				return SeekStatus.FOUND;
			}

			// we didn't hit the term exactly
			startIdx = -startIdx-1;
			if (startIdx == 0) {
				// our target occurs *before* the first term
				TermsEnum.SeekStatus seekStatus = mTermsEnum.seekCeil(target);
				assert seekStatus == TermsEnum.SeekStatus.NOT_FOUND;
				mOrd = 0;
				setTerm();
				assert mTerm != null;
				return SeekStatus.NOT_FOUND;
			}

			// back up to the start of the block
			startIdx --;

			if ((mOrd >> mIndexIntervalBits) == startIdx && mTerm != null && mTerm.compareTo(target) <= 0) {
				// we are already in the right block and the current term is before the term we want,
				// so we don't need to seek.
				
			} else {
				// seek to the right block
				TermsEnum.SeekStatus seekStatus = mTermsEnum.seekCeil(mIndexedTermsArray[startIdx]);
				assert seekStatus == TermsEnum.SeekStatus.FOUND;
				mOrd = startIdx << mIndexIntervalBits;
				setTerm();
				assert mTerm != null;  // should be non-null since it's in the index
			}

			while (mTerm != null && mTerm.compareTo(target) < 0) {
				next();
			}

			if (mTerm == null) 
				return SeekStatus.END;
			else if (mTerm.compareTo(target) == 0) 
				return SeekStatus.FOUND;
			else 
				return SeekStatus.NOT_FOUND;
		}

		@Override
		public void seekExact(long targetOrd) throws IOException {
			int delta = (int) (targetOrd - mOrdBase - mOrd);
			if (delta < 0 || delta > mIndexInterval) {
				final int idx = (int) (targetOrd >>> mIndexIntervalBits);
				final BytesRef base = mIndexedTermsArray[idx];
				
				mOrd = idx << mIndexIntervalBits;
				delta = (int) (targetOrd - mOrd);
				final TermsEnum.SeekStatus seekStatus = mTermsEnum.seekCeil(base, true);
				assert seekStatus == TermsEnum.SeekStatus.FOUND;
			}

			while (--delta >= 0) {
				BytesRef br = mTermsEnum.next();
				if (br == null) {
					assert false;
					return;
				}
				mOrd ++;
			}

			setTerm();
			assert mTerm != null;
		}

		private BytesRef setTerm() throws IOException {
			mTerm = mTermsEnum.getTerm();
			if (mPrefix != null && !StringHelper.startsWith(mTerm, mPrefix)) 
				mTerm = null;
			
			return mTerm;
		}
	}

	@Override
	public BytesRef lookupTerm(ITermsEnum termsEnum, int ord) throws IOException {
		termsEnum.seekExact(ord);
		return termsEnum.getTerm();
	}
	
}
