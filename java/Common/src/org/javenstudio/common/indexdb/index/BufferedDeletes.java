package org.javenstudio.common.indexdb.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.util.JvmUtil;

/** 
 * Holds buffered deletes, by docID, term or query for a
 * single segment. This is used to hold buffered pending
 * deletes against the to-be-flushed segment.  Once the
 * deletes are pushed (on flush in DocumentsWriter), these
 * deletes are converted to a FrozenDeletes instance.
 * 
 * NOTE: we are sync'd by BufferedDeletes, ie, all access to
 * instances of this class is via sync'd methods on
 * BufferedDeletes
 */
public class BufferedDeletes {

	/** 
	 * Rough logic: HashMap has an array[Entry] w/ varying
	 * load factor (say 2 * POINTER).  Entry is object w/ Term
	 * key, Integer val, int hash, Entry next
	 * (OBJ_HEADER + 3*POINTER + INT).  Term is object w/
	 * String field and String text (OBJ_HEADER + 2*POINTER).
	 * Term's field is String (OBJ_HEADER + 4*INT + POINTER +
	 * OBJ_HEADER + string.length*CHAR).
	 * Term's text is String (OBJ_HEADER + 4*INT + POINTER +
	 * OBJ_HEADER + string.length*CHAR).  Integer is
	 * OBJ_HEADER + INT. 
	 */
	public final static int BYTES_PER_DEL_TERM = 
			9*JvmUtil.NUM_BYTES_OBJECT_REF + 7*JvmUtil.NUM_BYTES_OBJECT_HEADER + 
			10*JvmUtil.NUM_BYTES_INT;

	/** 
	 * Rough logic: del docIDs are List<Integer>.  Say list
	 * allocates ~2X size (2*POINTER).  Integer is OBJ_HEADER
	 * + int 
	 */
	public final static int BYTES_PER_DEL_DOCID = 
			2*JvmUtil.NUM_BYTES_OBJECT_REF + JvmUtil.NUM_BYTES_OBJECT_HEADER + 
			JvmUtil.NUM_BYTES_INT;

	/** 
	 * Rough logic: HashMap has an array[Entry] w/ varying
	 * load factor (say 2 * POINTER).  Entry is object w/
	 * Query key, Integer val, int hash, Entry next
	 * (OBJ_HEADER + 3*POINTER + INT).  Query we often
	 * undercount (say 24 bytes).  Integer is OBJ_HEADER + INT. 
	 */
	public final static int BYTES_PER_DEL_QUERY = 
			5*JvmUtil.NUM_BYTES_OBJECT_REF + 2*JvmUtil.NUM_BYTES_OBJECT_HEADER + 
			2*JvmUtil.NUM_BYTES_INT + 24;

	public static final Integer MAX_INT = Integer.valueOf(Integer.MAX_VALUE);
  
	private final AtomicInteger mNumTermDeletes = new AtomicInteger();
	private final Map<ITerm,Integer> mTerms = new HashMap<ITerm,Integer>();
	private final Map<IQuery,Integer> mQueries = new HashMap<IQuery,Integer>();
	private final List<Integer> mDocIDs = new ArrayList<Integer>();
	private final AtomicLong mBytesUsed;
	private long mGen = 0;
  
	public BufferedDeletes() {
		this(new AtomicLong());
	}

	public BufferedDeletes(AtomicLong bytesUsed) {
		assert bytesUsed != null;
		mBytesUsed = bytesUsed;
	}

	public final List<Integer> getDocIDs() { 
		return mDocIDs;
	}
  
	public AtomicInteger getNumTermDeletes() { 
		return mNumTermDeletes;
	}
  
	public Map<ITerm,Integer> getTerms() { 
		return mTerms;
	}
  
	public Map<IQuery,Integer> getQueries() { 
		return mQueries;
	}
  
	public AtomicLong getBytesUsed() { 
		return mBytesUsed;
	}

	public void addQuery(IQuery query, int docIDUpto) {
		Integer current = mQueries.put(query, docIDUpto);
		// increment bytes used only if the query wasn't added so far.
		if (current == null) 
			mBytesUsed.addAndGet(BYTES_PER_DEL_QUERY);
	}

	public void addDocID(int docID) {
		mDocIDs.add(Integer.valueOf(docID));
		mBytesUsed.addAndGet(BYTES_PER_DEL_DOCID);
	}

	public void addTerm(ITerm term, int docIDUpto) {
		Integer current = mTerms.get(term);
		if (current != null && docIDUpto < current) {
			// Only record the new number if it's greater than the
			// current one.  This is important because if multiple
			// threads are replacing the same doc at nearly the
			// same time, it's possible that one thread that got a
			// higher docID is scheduled before the other
			// threads.  If we blindly replace than we can
			// incorrectly get both docs indexed.
			return;
		}

		mTerms.put(term, Integer.valueOf(docIDUpto));
		mNumTermDeletes.incrementAndGet();
		if (current == null) {
			mBytesUsed.addAndGet(BYTES_PER_DEL_TERM + term.getBytes().getLength() 
					+ (JvmUtil.NUM_BYTES_CHAR * term.getField().length()));
		}
	}
 
	public void clear() {
		mTerms.clear();
		mQueries.clear();
		mDocIDs.clear();
		mNumTermDeletes.set(0);
		mBytesUsed.set(0);
	}
  
	public void clearDocIDs() {
		mBytesUsed.addAndGet(-mDocIDs.size()*BYTES_PER_DEL_DOCID);
		mDocIDs.clear();
	}
  
	public boolean any() {
		return mTerms.size() > 0 || mDocIDs.size() > 0 || mQueries.size() > 0;
	}
	
	@Override
	public String toString() {
		StringBuilder sbuf = new StringBuilder();
		
		sbuf.append("gen=").append(mGen);
		if (mNumTermDeletes.get() != 0) {
			sbuf.append(" ").append(mNumTermDeletes.get())
				.append(" deleted terms (unique count=").append(mTerms.size()).append(")");
		}
		if (mQueries.size() != 0) {
			sbuf.append(" ").append(mQueries.size()).append(" deleted queries");
		}
		if (mDocIDs.size() != 0) {
			sbuf.append(" ").append(mDocIDs.size()).append(" deleted docIDs");
		}
		if (mBytesUsed.get() != 0) {
			sbuf.append(" bytesUsed=").append(mBytesUsed.get());
		}

		return sbuf.toString();
	}
	
}
