package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.JvmUtil;

/** 
 * Can next() and advance() through the terms in an FST
 *
 */
abstract class FSTEnum<T> {
	
	protected final FST<T> mFst;

	@SuppressWarnings({"unchecked"}) 
	protected FSTArc<T>[] mArcs = new FSTArc[10];
	// outputs are cumulative
	@SuppressWarnings({"unchecked"}) 
	protected T[] mOutput = (T[]) new Object[10];

	protected final T NO_OUTPUT;
	protected final BytesReader mFstReader;
	protected final FSTArc<T> mScratchArc = new FSTArc<T>();

	protected int mUpto;
	protected int mTargetLength;

	/** 
	 * doFloor controls the behavior of advance: if it's true
	 *  doFloor is true, advance positions to the biggest
	 *  term before target.
	 */
	protected FSTEnum(FST<T> fst) {
		mFst = fst;
		mFstReader = fst.getBytesReader(0);
		NO_OUTPUT = fst.getOutputs().getNoOutput();
		fst.getFirstArc(getArc(0));
		mOutput[0] = NO_OUTPUT;
	}

	protected abstract int getTargetLabel();
	protected abstract int getCurrentLabel();

	protected abstract void setCurrentLabel(int label);
	protected abstract void grow();

	/** 
	 * Rewinds enum state to match the shared prefix between
	 *  current term and target term 
	 */
	protected final void rewindPrefix() throws IOException {
		if (mUpto == 0) {
			mUpto = 1;
			mFst.readFirstTargetArc(getArc(0), getArc(1), mFstReader);
			return;
		}

		final int currentLimit = mUpto;
		mUpto = 1;
		
		while (mUpto < currentLimit && mUpto <= mTargetLength+1) {
			final int cmp = getCurrentLabel() - getTargetLabel();
			if (cmp < 0) {
				// seek forward
				break;
			} else if (cmp > 0) {
				// seek backwards -- reset this arc to the first arc
				final FSTArc<T> arc = getArc(mUpto);
				mFst.readFirstTargetArc(getArc(mUpto-1), arc, mFstReader);
				break;
			}
			mUpto ++;
		}
	}

	protected void doNext() throws IOException {
		if (mUpto == 0) {
			mUpto = 1;
			mFst.readFirstTargetArc(getArc(0), getArc(1), mFstReader);
			
		} else {
			// pop
			while (mArcs[mUpto].isLast()) {
				mUpto --;
				if (mUpto == 0) 
					return;
			}
			mFst.readNextArc(mArcs[mUpto], mFstReader);
		}

		pushFirst();
	}

	// TODO: should we return a status here (SEEK_FOUND / SEEK_NOT_FOUND /
	// SEEK_END)?  saves the eq check above?

	/** Seeks to smallest term that's >= target. */
	protected void doSeekCeil() throws IOException {
		// TODO: possibly caller could/should provide common
		// prefix length?  ie this work may be redundant if
		// caller is in fact intersecting against its own
		// automaton

		// Save time by starting at the end of the shared prefix
		// b/w our current term & the target:
		rewindPrefix();

		FSTArc<T> arc = getArc(mUpto);
		int targetLabel = getTargetLabel();

		// Now scan forward, matching the new suffix of the target
		while (true) {
			if (arc.getBytesPerArc() != 0 && arc.getLabel() != -1) {
				// Arcs are fixed array -- use binary search to find
				// the target.
				final BytesReader in = mFst.getBytesReader(0);
				
				int low = arc.getArcIndex();
				int high = arc.getNumArcs() - 1;
				int mid = 0;
				boolean found = false;
				
				while (low <= high) {
					mid = (low + high) >>> 1;
		
          			in.mPos = arc.getPosArcsStart();
          			in.skip(arc.getBytesPerArc()*mid+1);
          			
          			final int midLabel = mFst.readLabel(in);
          			final int cmp = midLabel - targetLabel;
          			
          			if (cmp < 0) {
          				low = mid + 1;
          			} else if (cmp > 0) {
          				high = mid - 1;
          			} else {
          				found = true;
          				break;
          			}
				}

				// NOTE: this code is dup'd w/ the code below (in
				// the outer else clause):
				if (found) {
					// Match
					arc.setArcIndex(mid-1);
					mFst.readNextRealArc(arc, in);
					
					assert arc.getArcIndex() == mid;
					assert arc.getLabel() == targetLabel: "arc.label=" + arc.getLabel() + 
							" vs targetLabel=" + targetLabel + " mid=" + mid;
					
					mOutput[mUpto] = mFst.getOutputs().add(mOutput[mUpto-1], arc.getOutput());
					if (targetLabel == FST.END_LABEL) 
						return;
					
					setCurrentLabel(arc.getLabel());
					incr();
					
					arc = mFst.readFirstTargetArc(arc, getArc(mUpto), mFstReader);
					targetLabel = getTargetLabel();
					
					continue;
					
				} else if (low == arc.getNumArcs()) {
					// Dead end
					arc.setArcIndex(arc.getNumArcs()-2);
					mFst.readNextRealArc(arc, in);
					assert arc.isLast();
					// Dead end (target is after the last arc);
					// rollback to last fork then push
					mUpto --;
					
					while (true) {
						if (mUpto == 0) 
							return;
            
						final FSTArc<T> prevArc = getArc(mUpto);
						if (!prevArc.isLast()) {
							mFst.readNextArc(prevArc, mFstReader);
							pushFirst();
							return;
						}
						mUpto --;
					}
				} else {
					arc.setArcIndex((low > high ? low : high)-1);
					mFst.readNextRealArc(arc, in);
					assert arc.getLabel() > targetLabel;
					pushFirst();
					return;
				}
			} else {
				// Arcs are not array'd -- must do linear scan:
				if (arc.getLabel() == targetLabel) {
					// recurse
					mOutput[mUpto] = mFst.getOutputs().add(mOutput[mUpto-1], arc.getOutput());
					if (targetLabel == FST.END_LABEL) 
						return;
					
					setCurrentLabel(arc.getLabel());
					incr();
					
					arc = mFst.readFirstTargetArc(arc, getArc(mUpto), mFstReader);
					targetLabel = getTargetLabel();
					
				} else if (arc.getLabel() > targetLabel) {
					pushFirst();
					return;
					
				} else if (arc.isLast()) {
					// Dead end (target is after the last arc);
					// rollback to last fork then push
					mUpto --;
					while (true) {
						if (mUpto == 0) 
							return;
						
						final FSTArc<T> prevArc = getArc(mUpto);
						if (!prevArc.isLast()) {
							mFst.readNextArc(prevArc, mFstReader);
							pushFirst();
							return;
						}
						
						mUpto --;
					}
				} else {
					// keep scanning
					mFst.readNextArc(arc, mFstReader);
				}
			}
		}
	}

	// TODO: should we return a status here (SEEK_FOUND / SEEK_NOT_FOUND /
	// SEEK_END)?  saves the eq check above?
	/** Seeks to largest term that's <= target. */
	protected void doSeekFloor() throws IOException {
		// TODO: possibly caller could/should provide common
		// prefix length?  ie this work may be redundant if
		// caller is in fact intersecting against its own
		// automaton

		// Save CPU by starting at the end of the shared prefix
		// b/w our current term & the target:
		rewindPrefix();

		FSTArc<T> arc = getArc(mUpto);
		int targetLabel = getTargetLabel();

		// Now scan forward, matching the new suffix of the target
		while (true) {
			if (arc.getBytesPerArc() != 0 && arc.getLabel() != FST.END_LABEL) {
				// Arcs are fixed array -- use binary search to find
				// the target.
				final BytesReader in = mFst.getBytesReader(0);
				
				int low = arc.getArcIndex();
				int high = arc.getNumArcs() - 1;
				int mid = 0;
				boolean found = false;
				
				while (low <= high) {
					mid = (low + high) >>> 1;
					in.mPos = arc.getPosArcsStart();
					in.skip(arc.getBytesPerArc()*mid+1);
					
					final int midLabel = mFst.readLabel(in);
					final int cmp = midLabel - targetLabel;
					
					if (cmp < 0) {
						low = mid + 1;
					} else if (cmp > 0) {
						high = mid - 1;
					} else {
						found = true;
						break;
					}
				}

				// NOTE: this code is dup'd w/ the code below (in
				// the outer else clause):
				if (found) {
					// Match -- recurse
					arc.setArcIndex(mid-1);
					mFst.readNextRealArc(arc, in);
					
					assert arc.getArcIndex() == mid;
					assert arc.getLabel() == targetLabel: "arc.label=" + arc.getLabel() + 
							" vs targetLabel=" + targetLabel + " mid=" + mid;
					
					mOutput[mUpto] = mFst.getOutputs().add(mOutput[mUpto-1], arc.getOutput());
					if (targetLabel == FST.END_LABEL) 
						return;
					
					setCurrentLabel(arc.getLabel());
					incr();
					
					arc = mFst.readFirstTargetArc(arc, getArc(mUpto), mFstReader);
					targetLabel = getTargetLabel();
					continue;
					
				} else if (high == -1) {
					// Very first arc is after our target
					// TODO: if each arc could somehow read the arc just
					// before, we can save this re-scan.  The ceil case
					// doesn't need this because it reads the next arc
					// instead:
					while (true) {
						// First, walk backwards until we find a first arc
						// that's before our target label:
						mFst.readFirstTargetArc(getArc(mUpto-1), arc, mFstReader);
						if (arc.getLabel() < targetLabel) {
							// Then, scan forwards to the arc just before
							// the targetLabel:
							while (!arc.isLast() && mFst.readNextArcLabel(arc, in) < targetLabel) {
								mFst.readNextArc(arc, mFstReader);
							}
							pushLast();
							return;
						}
						mUpto --;
						if (mUpto == 0) 
							return;
						
						targetLabel = getTargetLabel();
						arc = getArc(mUpto);
					}
				} else {
					// There is a floor arc:
					arc.setArcIndex((low > high ? high : low)-1);
					mFst.readNextRealArc(arc, in);
					
					assert arc.isLast() || mFst.readNextArcLabel(arc, in) > targetLabel;
					assert arc.getLabel() < targetLabel: "arc.label=" + arc.getLabel() + 
							" vs targetLabel=" + targetLabel;
					
					pushLast();
					return;
				}
				
			} else {
				if (arc.getLabel() == targetLabel) {
					// Match -- recurse
					mOutput[mUpto] = mFst.getOutputs().add(mOutput[mUpto-1], arc.getOutput());
					if (targetLabel == FST.END_LABEL) 
						return;
					
					setCurrentLabel(arc.getLabel());
					incr();
					
					arc = mFst.readFirstTargetArc(arc, getArc(mUpto), mFstReader);
					targetLabel = getTargetLabel();
					
				} else if (arc.getLabel() > targetLabel) {
					// TODO: if each arc could somehow read the arc just
					// before, we can save this re-scan.  The ceil case
					// doesn't need this because it reads the next arc
					// instead:
					while (true) {
						// First, walk backwards until we find a first arc
						// that's before our target label:
						mFst.readFirstTargetArc(getArc(mUpto-1), arc, mFstReader);
						if (arc.getLabel() < targetLabel) {
							// Then, scan forwards to the arc just before
							// the targetLabel:
							while (!arc.isLast() && mFst.readNextArcLabel(arc, mFstReader) < targetLabel) {
								mFst.readNextArc(arc, mFstReader);
							}
							pushLast();
							return;
						}
						
						mUpto --;
						if (mUpto == 0) 
							return;
						
						targetLabel = getTargetLabel();
						arc = getArc(mUpto);
					}
				} else if (!arc.isLast()) {
					if (mFst.readNextArcLabel(arc, mFstReader) > targetLabel) {
						pushLast();
						return;
					} else {
						// keep scanning
						mFst.readNextArc(arc, mFstReader);
					}
				} else {
					pushLast();
					return;
				}
			}
		}
	}

	/** Seeks to exactly target term. */
	protected boolean doSeekExact() throws IOException {
		// TODO: possibly caller could/should provide common
		// prefix length?  ie this work may be redundant if
		// caller is in fact intersecting against its own
		// automaton

		// Save time by starting at the end of the shared prefix
		// b/w our current term & the target:
		rewindPrefix();

		FSTArc<T> arc = getArc(mUpto-1);
		int targetLabel = getTargetLabel();

		final BytesReader fstReader = mFst.getBytesReader(0);

		while (true) {
			final FSTArc<T> nextArc = mFst.findTargetArc(targetLabel, arc, getArc(mUpto), fstReader);
			if (nextArc == null) {
				// short circuit
				//upto--;
				//upto = 0;
				mFst.readFirstTargetArc(arc, getArc(mUpto), fstReader);
				return false;
			}
			
			// Match -- recurse:
			mOutput[mUpto] = mFst.getOutputs().add(mOutput[mUpto-1], nextArc.getOutput());
			if (targetLabel == FST.END_LABEL) 
				return true;
			
			setCurrentLabel(targetLabel);
			incr();
			targetLabel = getTargetLabel();
			arc = nextArc;
		}
	}

	private void incr() {
		mUpto ++;
		grow();
		
		if (mArcs.length <= mUpto) {
			@SuppressWarnings({"unchecked"}) 
			final FSTArc<T>[] newArcs = 
					new FSTArc[ArrayUtil.oversize(1+mUpto, JvmUtil.NUM_BYTES_OBJECT_REF)];
			System.arraycopy(mArcs, 0, newArcs, 0, mArcs.length);
			mArcs = newArcs;
		}
		
		if (mOutput.length <= mUpto) {
			@SuppressWarnings({"unchecked"}) 
			final T[] newOutput =
					(T[]) new Object[ArrayUtil.oversize(1+mUpto, JvmUtil.NUM_BYTES_OBJECT_REF)];
			System.arraycopy(mOutput, 0, newOutput, 0, mOutput.length);
			mOutput = newOutput;
		}
	}

	// Appends current arc, and then recurses from its target,
	// appending first arc all the way to the final node
	private void pushFirst() throws IOException {
		FSTArc<T> arc = mArcs[mUpto];
		assert arc != null;

		while (true) {
			mOutput[mUpto] = mFst.getOutputs().add(mOutput[mUpto-1], arc.getOutput());
			if (arc.getLabel() == FST.END_LABEL) {
				// Final node
				break;
			}
      
			setCurrentLabel(arc.getLabel());
			incr();
      
			final FSTArc<T> nextArc = getArc(mUpto);
			mFst.readFirstTargetArc(arc, nextArc, mFstReader);
			arc = nextArc;
		}
	}

	// Recurses from current arc, appending last arc all the
	// way to the first final node
	private void pushLast() throws IOException {
		FSTArc<T> arc = mArcs[mUpto];
		assert arc != null;

		while (true) {
			setCurrentLabel(arc.getLabel());
			mOutput[mUpto] = mFst.getOutputs().add(mOutput[mUpto-1], arc.getOutput());
			if (arc.getLabel() == FST.END_LABEL) {
				// Final node
				break;
			}
			
			incr();
			arc = mFst.readLastTargetArc(arc, getArc(mUpto), mFstReader);
		}
	}

	private FSTArc<T> getArc(int idx) {
		if (mArcs[idx] == null) 
			mArcs[idx] = new FSTArc<T>();
		
		return mArcs[idx];
	}
	
}
