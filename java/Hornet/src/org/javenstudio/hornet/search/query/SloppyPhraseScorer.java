package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.ISloppySimilarityScorer;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.hornet.search.OpenBitSet;

public final class SloppyPhraseScorer extends PhraseScorer {
  
	private final PhraseQueue mQueue; // for advancing min position
	private final int mSlop;
	private final int mNumPostings;
  
	private int mEnd; // current largest phrase position  

	// in each group are PPs that repeats each other (i.e. same term), sorted by (query) offset 
	private PhrasePositions[][] mRptGroups; 
	// temporary stack for switching colliding repeating pps 
	private PhrasePositions[] mRptStack; 
	
	// flag indicating that there are repetitions (as checked in first candidate doc)
	private boolean mHasRpts; 
	// flag to only check for repetitions in first candidate doc
	private boolean mCheckedRpts; 
	private boolean mHasMultiTermRpts;
	
	public SloppyPhraseScorer(Weight weight, PostingsAndFreq[] postings,
			int slop, ISloppySimilarityScorer docScorer) {
		super(weight, postings, docScorer);
		
		mSlop = slop;
		mNumPostings = postings == null ? 0 : postings.length;
		mQueue = new PhraseQueue(postings.length);
	}

	/**
	 * Score a candidate doc for all slop-valid position-combinations (matches) 
	 * encountered while traversing/hopping the PhrasePositions.
	 * <br> The score contribution of a match depends on the distance: 
	 * <br> - highest score for distance=0 (exact match).
	 * <br> - score gets lower as distance gets higher.
	 * <br>Example: for query "a b"~2, a document "x a b a y" can be scored twice: 
	 * once for "a b" (distance=0), and once for "b a" (distance=2).
	 * <br>Possibly not all valid combinations are encountered, because for efficiency  
	 * we always propagate the least PhrasePosition. This allows to base on 
  	* PriorityQueue and move forward faster. 
  	* As result, for example, document "a b c b a"
  	* would score differently for queries "a b c"~4 and "c b a"~4, although 
  	* they really are equivalent. 
  	* Similarly, for doc "a b c b a f g", query "c b"~2 
  	* would get same score as "g f"~2, although "c b"~2 could be matched twice.
  	* We may want to fix this in the future (currently not, for performance reasons).
  	*/
	@Override
	protected float phraseFreq() throws IOException {
		if (!initPhrasePositions()) 
			return 0.0f;
		
		float freq = 0.0f;
		PhrasePositions pp = mQueue.pop();
		int matchLength = mEnd - pp.getPosition();
		int next = mQueue.top().getPosition(); 
		
		while (advancePP(pp)) {
			if (mHasRpts && !advanceRpts(pp)) 
				break; // pps exhausted
      
			if (pp.getPosition() > next) { 
				// done minimizing current match-length 
				if (matchLength <= mSlop) 
					freq += mDocScorer.computeSlopFactor(matchLength); // score match
        
				mQueue.add(pp);
				pp = mQueue.pop();
				next = mQueue.top().getPosition();
				matchLength = mEnd - pp.getPosition();
				
			} else {
				int matchLength2 = mEnd - pp.getPosition();
				if (matchLength2 < matchLength) 
					matchLength = matchLength2;
			}
		}
		
		if (matchLength <= mSlop) 
			freq += mDocScorer.computeSlopFactor(matchLength); // score match
    
		return freq;
	}

	/** advance a PhrasePosition and update 'end', return false if exhausted */
	private boolean advancePP(PhrasePositions pp) throws IOException {
		if (!pp.nextPosition()) 
			return false;
		
		if (pp.getPosition() > mEnd) 
			mEnd = pp.getPosition();
		
		return true;
	}
  
	/** 
	 * pp was just advanced. If that caused a repeater collision, resolve by advancing the lesser
	 * of the two colliding pps. Note that there can only be one collision, as by the initialization
	 * there were no collisions before pp was advanced. 
	 */
	private boolean advanceRpts(PhrasePositions pp) throws IOException {
		if (pp.mRptGroup < 0) 
			return true; // not a repeater
		
		PhrasePositions[] rg = mRptGroups[pp.mRptGroup];
		OpenBitSet bits = new OpenBitSet(rg.length); // for re-queuing after collisions are resolved
		
		int k0 = pp.mRptInd;
		int k;
		
		while ((k = collide(pp)) >= 0) {
			// always advance the lesser of the (only) two colliding pps
			pp = lesser(pp, rg[k]); 
			if (!advancePP(pp)) 
				return false; // exhausted
			
			// careful: mark only those currently in the queue
			if (k != k0) 
				bits.set(k); // mark that pp2 need to be re-queued
		}
		
		// collisions resolved, now re-queue
		// empty (partially) the queue until seeing all pps advanced for resolving collisions
		int n = 0;
		
		while (bits.cardinality() > 0) {
			PhrasePositions pp2 = mQueue.pop();
			mRptStack[n++] = pp2;
			
			if (pp2.mRptGroup >= 0 && bits.get(pp2.mRptInd)) 
				bits.clear(pp2.mRptInd);
		}
		
		// add back to queue
		for (int i=n-1; i>=0; i--) {
			mQueue.add(mRptStack[i]);
		}
		
		return true;
	}

	/** compare two pps, but only by position and offset */
	private PhrasePositions lesser(PhrasePositions pp, PhrasePositions pp2) {
		if (pp.getPosition() < pp2.getPosition() || 
		   (pp.getPosition() == pp2.getPosition() && pp.getOffset() < pp2.getOffset())) 
			return pp;
		
		return pp2;
	}

	/** index of a pp2 colliding with pp, or -1 if none */
	private int collide(PhrasePositions pp) {
		int tpPos = tpPos(pp);
		PhrasePositions[] rg = mRptGroups[pp.mRptGroup];
		
		for (int i=0; i < rg.length; i++) {
			PhrasePositions pp2 = rg[i];
			if (pp2 != pp && tpPos(pp2) == tpPos) 
				return pp2.mRptInd;
		}
		
		return -1;
	}

	/**
	 * Initialize PhrasePositions in place.
	 * A one time initialization for this scorer (on first doc matching all terms):
	 * <ul>
	 *  <li>Check if there are repetitions
	 *  <li>If there are, find groups of repetitions.
	 * </ul>
	 * Examples:
	 * <ol>
	 *  <li>no repetitions: <b>"ho my"~2</b>
	 *  <li>repetitions: <b>"ho my my"~2</b>
	 *  <li>repetitions: <b>"my ho my"~2</b>
	 * </ol>
	 * @return false if PPs are exhausted (and so current doc will not be a match) 
	 */
	private boolean initPhrasePositions() throws IOException {
		mEnd = Integer.MIN_VALUE;
		if (!mCheckedRpts) 
			return initFirstTime();
		
		if (!mHasRpts) {
			initSimple();
			return true; // PPs available
		}
		
		return initComplex();
	}
  
	/** 
	 * no repeats: simplest case, and most common. 
	 * It is important to keep this piece of the code simple and efficient 
	 */
	private void initSimple() throws IOException {
		mQueue.clear();
		
		// position pps and build queue from list
		for (PhrasePositions pp = mMin, prev = null; prev != mMax; pp = (prev=pp).mNext) { 
			// iterate cyclic list: done once handled max
			pp.firstPosition();
			
			if (pp.getPosition() > mEnd) 
				mEnd = pp.getPosition();
			
			mQueue.add(pp);
		}
	}
  
	/** with repeats: not so simple. */
	private boolean initComplex() throws IOException {
		placeFirstPositions();
		
		if (!advanceRepeatGroups()) 
			return false; // PPs exhausted
		
		fillQueue();
		return true; // PPs available
	}

	/** move all PPs to their first position */
	private void placeFirstPositions() throws IOException {
		for (PhrasePositions pp = mMin, prev = null; prev != mMax; pp = (prev=pp).mNext) { 
			// iterate cyclic list: done once handled max
			pp.firstPosition();
		}
	}

	/** Fill the queue (all pps are already placed */
	private void fillQueue() {
		mQueue.clear();
		
		for (PhrasePositions pp = mMin, prev = null; prev != mMax; pp = (prev=pp).mNext) { 
			// iterate cyclic list: done once handled max
			if (pp.getPosition() > mEnd) 
				mEnd = pp.getPosition();
			
			mQueue.add(pp);
		}
	}

	/** 
	 * At initialization (each doc), each repetition group is sorted by (query) offset.
	 * This provides the start condition: no collisions.
	 * <p>Case 1: no multi-term repeats<br>
	 * It is sufficient to advance each pp in the group by one less than its group index.
	 * So lesser pp is not advanced, 2nd one advance once, 3rd one advanced twice, etc.
	 * <p>Case 2: multi-term repeats<br>
	 * 
	 * @return false if PPs are exhausted. 
	 */
	private boolean advanceRepeatGroups() throws IOException {
		for (PhrasePositions[] rg: mRptGroups) { 
			if (mHasMultiTermRpts) {
				// more involved, some may not collide
				int incr;
				
				for (int i=0; i < rg.length; i += incr) {
					PhrasePositions pp = rg[i];
					incr = 1;
					int k;
					
					while ((k = collide(pp)) >= 0) {
						PhrasePositions pp2 = lesser(pp, rg[k]);
						if (!advancePP(pp2)) // at initialization always advance pp with higher offset
							return false; // exhausted
						
						if (pp2.mRptInd < i) { // should not happen?
							incr = 0;
							break;
						}
					}
				}
				
			} else {
				// simpler, we know exactly how much to advance
				for (int j=1; j < rg.length; j++) {
					for (int k=0; k<j; k++) {
						if (!rg[j].nextPosition()) 
							return false; // PPs exhausted
					}
				}
			}
		}
		
		return true; // PPs available
	}
  
	/** 
	 * initialize with checking for repeats. Heavy work, but done only for the first candidate doc.<p>
	 * If there are repetitions, check if multi-term postings (MTP) are involved.<p>
	 * Without MTP, once PPs are placed in the first candidate doc, repeats (and groups) are visible.<br>
	 * With MTP, a more complex check is needed, up-front, as there may be "hidden collisions".<br>
	 * For example P1 has {A,B}, P1 has {B,C}, and the first doc is: "A C B". At start, P1 would point
	 * to "A", p2 to "C", and it will not be identified that P1 and P2 are repetitions of each other.<p>
	 * The more complex initialization has two parts:<br>
	 * (1) identification of repetition groups.<br>
	 * (2) advancing repeat groups at the start of the doc.<br>
	 * For (1), a possible solution is to just create a single repetition group, 
	 * made of all repeating pps. But this would slow down the check for collisions, 
	 * as all pps would need to be checked. Instead, we compute "connected regions" 
	 * on the bipartite graph of postings and terms.  
	 */
	private boolean initFirstTime() throws IOException {
		mCheckedRpts = true;
		placeFirstPositions();

		LinkedHashMap<ITerm,Integer> rptTerms = repeatingTerms(); 
		mHasRpts = !rptTerms.isEmpty();

		if (mHasRpts) {
			mRptStack = new PhrasePositions[mNumPostings]; // needed with repetitions
			
			ArrayList<ArrayList<PhrasePositions>> rgs = gatherRptGroups(rptTerms);
			sortRptGroups(rgs);
			
			if (!advanceRepeatGroups()) 
				return false; // PPs exhausted
		}
    
		fillQueue();
		return true; // PPs available
	}

	/** 
	 * sort each repetition group by (query) offset. 
	 * Done only once (at first doc) and allows to initialize faster for each doc. 
	 */
	private void sortRptGroups(ArrayList<ArrayList<PhrasePositions>> rgs) {
		mRptGroups = new PhrasePositions[rgs.size()][];
		
		Comparator<PhrasePositions> cmprtr = new Comparator<PhrasePositions>() {
				public int compare(PhrasePositions pp1, PhrasePositions pp2) {
					return pp1.mOffset - pp2.mOffset;
				}
			};
		
		for (int i=0; i < mRptGroups.length; i++) {
			PhrasePositions[] rg = rgs.get(i).toArray(new PhrasePositions[0]);
			Arrays.sort(rg, cmprtr);
			mRptGroups[i] = rg;
			
			for (int j=0; j < rg.length; j++) {
				rg[j].mRptInd = j; // we use this index for efficient re-queuing
			}
		}
	}

	/** Detect repetition groups. Done once - for first doc */
	private ArrayList<ArrayList<PhrasePositions>> gatherRptGroups(
			LinkedHashMap<ITerm,Integer> rptTerms) throws IOException {
		PhrasePositions[] rpp = repeatingPPs(rptTerms); 
		ArrayList<ArrayList<PhrasePositions>> res = new ArrayList<ArrayList<PhrasePositions>>();
		
		if (!mHasMultiTermRpts) {
			// simpler - no multi-terms - can base on positions in first doc
			for (int i=0; i < rpp.length; i++) {
				PhrasePositions pp = rpp[i];
				if (pp.mRptGroup >=0) continue; // already marked as a repetition
				
				int tpPos = tpPos(pp);
				
				for (int j=i+1; j < rpp.length; j++) {
					PhrasePositions pp2 = rpp[j];
					if (pp2.mRptGroup >=0        	// already marked as a repetition
						|| pp2.mOffset == pp.mOffset // not a repetition: two PPs are originally in same offset in the query! 
						|| tpPos(pp2) != tpPos)  	// not a repetition
						continue; 
					
					// a repetition
					int g = pp.mRptGroup;
					
					if (g < 0) {
						g = res.size();
						pp.mRptGroup = g;
						
						ArrayList<PhrasePositions> rl = new ArrayList<PhrasePositions>(2);
						rl.add(pp);
						res.add(rl); 
					}
					
					pp2.mRptGroup = g;
					res.get(g).add(pp2);
				}
			}
			
		} else {
			// more involved - has multi-terms
			ArrayList<HashSet<PhrasePositions>> tmp = new ArrayList<HashSet<PhrasePositions>>();
			List<OpenBitSet> bb = ppTermsBitSets(rpp, rptTerms);
			unionTermGroups(bb);
			
			Map<ITerm,Integer> tg = termGroups(rptTerms, bb);
			HashSet<Integer> distinctGroupIDs = new HashSet<Integer>(tg.values());
			
			for (int i=0; i < distinctGroupIDs.size(); i++) {
				tmp.add(new HashSet<PhrasePositions>());
			}
			
			for (PhrasePositions pp : rpp) {
				for (ITerm t: pp.getTerms()) {
					if (rptTerms.containsKey(t)) {
						int g = tg.get(t);
						tmp.get(g).add(pp);
						
						assert pp.mRptGroup==-1 || pp.mRptGroup==g;  
						pp.mRptGroup = g;
					}
				}
			}
			
			for (HashSet<PhrasePositions> hs : tmp) {
				res.add(new ArrayList<PhrasePositions>(hs));
			}
		}
		
		return res;
	}

	/** Actual position in doc of a PhrasePosition, relies on that position = tpPos - offset) */
	private final int tpPos(PhrasePositions pp) {
		return pp.getPosition() + pp.getOffset();
	}

	/** find repeating terms and assign them ordinal values */
	private LinkedHashMap<ITerm,Integer> repeatingTerms() {
		LinkedHashMap<ITerm,Integer> tord = new LinkedHashMap<ITerm,Integer>();
		HashMap<ITerm,Integer> tcnt = new HashMap<ITerm,Integer>();
		
		for (PhrasePositions pp = mMin, prev = null; prev != mMax; pp = (prev=pp).mNext) { 
			// iterate cyclic list: done once handled max
			for (ITerm t : pp.getTerms()) {
				Integer cnt0 = tcnt.get(t);
				Integer cnt = cnt0 == null ? new Integer(1) : new Integer(1+cnt0.intValue());
				tcnt.put(t, cnt);
				
				if (cnt == 2) 
					tord.put(t, tord.size());
			}
		}
		
		return tord;
	}

	/** find repeating pps, and for each, if has multi-terms, update this.hasMultiTermRpts */
	private PhrasePositions[] repeatingPPs(Map<ITerm,Integer> rptTerms) {
		ArrayList<PhrasePositions> rp = new ArrayList<PhrasePositions>(); 
		
		for (PhrasePositions pp = mMin, prev = null; prev != mMax; pp = (prev=pp).mNext) {
			// iterate cyclic list: done once handled max
			for (ITerm t : pp.getTerms()) {
				if (rptTerms.containsKey(t)) {
					rp.add(pp);
					mHasMultiTermRpts |= (pp.getTerms().length > 1);
					break;
				}
			}
		}
		
		return rp.toArray(new PhrasePositions[0]);
	}
  
	/** 
	 * bit-sets - for each repeating pp, for each of its repeating terms, 
	 * the term ordinal values is set 
	 */
	private List<OpenBitSet> ppTermsBitSets(PhrasePositions[] rpp, Map<ITerm,Integer> tord) {
		ArrayList<OpenBitSet> bb = new ArrayList<OpenBitSet>(rpp.length);
		
		for (PhrasePositions pp : rpp) {
			OpenBitSet b = new OpenBitSet(tord.size());
			Integer ord;
			
			for (ITerm t: pp.getTerms()) {
				if ((ord = tord.get(t)) != null) 
					b.set(ord);
			}
			
			bb.add(b);
		}
		
		return bb;
	}
  
	/** 
	 * union (term group) bit-sets until they are disjoint (O(n^^2)), 
	 * and each group have different terms 
	 */
	private void unionTermGroups(List<OpenBitSet> bb) {
		int incr;
		
		for (int i=0; i<bb.size()-1; i+=incr) {
			incr = 1;
			int j = i+1;
			
			while (j < bb.size()) {
				if (bb.get(i).intersects(bb.get(j))) {
					bb.get(i).union(bb.get(j));
					bb.remove(j);
					incr = 0;
					
				} else 
					++j;
			}
		}
	}
  
	/** map each term to the single group that contains it */ 
	private Map<ITerm,Integer> termGroups(LinkedHashMap<ITerm,Integer> tord, 
			List<OpenBitSet> bb) throws IOException {
		HashMap<ITerm,Integer> tg = new HashMap<ITerm,Integer>();
		ITerm[] t = tord.keySet().toArray(new ITerm[0]);
		
		for (int i=0; i < bb.size(); i++) { // i is the group no.
			DocIdSetIterator bits = bb.get(i).iterator();
			int ord;
			
			while ((ord = bits.nextDoc()) != NO_MORE_DOCS) {
				tg.put(t[ord],i);
			}
		}
		
		return tg;
	}
  
//  private void printQueue(PrintStream ps, PhrasePositions ext, String title) {
//    //if (min.doc != ?) return;
//    ps.println();
//    ps.println("---- "+title);
//    ps.println("EXT: "+ext);
//    PhrasePositions[] t = new PhrasePositions[pq.size()];
//    if (pq.size()>0) {
//      t[0] = pq.pop();
//      ps.println("  " + 0 + "  " + t[0]);
//      for (int i=1; i<t.length; i++) {
//        t[i] = pq.pop();
//        assert t[i-1].position <= t[i].position;
//        ps.println("  " + i + "  " + t[i]);
//      }
//      // add them back
//      for (int i=t.length-1; i>=0; i--) {
//        pq.add(t[i]);
//      }
//    }
//  }
  
}
