package org.javenstudio.hornet.store.fst;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.IntsRef;
import org.javenstudio.common.indexdb.util.JvmUtil;

/**
 * Builds a minimal FST (maps an IntsRef term to an arbitrary
 * output) from pre-sorted terms with outputs.  The FST
 * becomes an FSA if you use NoOutputs.  The FST is written
 * on-the-fly into a compact serialized format byte array, which can
 * be saved to / loaded from a Directory or used directly
 * for traversal.  The FST is always finite (no cycles).
 *
 * <p>NOTE: The algorithm is described at
 * http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.24.3698</p>
 *
 * The parameterized type T is the output type.  See the
 * subclasses of {@link Outputs}.
 */
public class Builder<T> {
	
	private final NodeHash<T> mDedupHash;
	private final FST<T> mFst;
	public final T NO_OUTPUT;

	// simplistic pruning: we prune node (and all following
	// nodes) if less than this number of terms go through it:
	private final int mMinSuffixCount1;

	// better pruning: we prune node (and all following
	// nodes) if the prior node has less than this number of
	// terms go through it:
	private final int mMinSuffixCount2;

	private final boolean mDoShareNonSingletonNodes;
	private final int mShareMaxTailLength;

	private final IntsRef mLastInput = new IntsRef();

	// NOTE: cutting this over to ArrayList instead loses ~6%
	// in build performance on 9.8M Wikipedia terms; so we
	// left this as an array:
	// current "frontier"
	private UnCompiledNode<T>[] mFrontier;

	private final FreezeTail<T> mFreezeTail;

	final FST<T> getFst() { return mFst; }
	
	/**
	 * Instantiates an FST/FSA builder without any pruning. A shortcut
	 * to {@link #Builder(FST.INPUT_TYPE, int, int, boolean,
	 * boolean, int, Outputs, FreezeTail, boolean)} with
	 * pruning options turned off.
	 */
	public Builder(FST.INPUT_TYPE inputType, Outputs<T> outputs) {
		this(inputType, 0, 0, true, true, Integer.MAX_VALUE, 
				outputs, null, false, IIntsReader.COMPACT);
	}

	/**
	 * Instantiates an FST/FSA builder with {@link PackedInts#DEFAULT}
	 * <code>acceptableOverheadRatio</code>.
	 */
	public Builder(FST.INPUT_TYPE inputType, int minSuffixCount1, int minSuffixCount2, 
			boolean doShareSuffix, boolean doShareNonSingletonNodes, int shareMaxTailLength, 
			Outputs<T> outputs, FreezeTail<T> freezeTail, boolean willPackFST) {
		this(inputType, minSuffixCount1, minSuffixCount2, doShareSuffix, doShareNonSingletonNodes,
				shareMaxTailLength, outputs, freezeTail, willPackFST, IIntsReader.DEFAULT);
	}

	/**
	 * Instantiates an FST/FSA builder with all the possible tuning and construction
	 * tweaks. Read parameter documentation carefully.
	 * 
	 * @param inputType 
	 *    The input type (transition labels). Can be anything from {@link INPUT_TYPE}
	 *    enumeration. Shorter types will consume less memory. Strings (character sequences) are 
	 *    represented as {@link INPUT_TYPE#BYTE4} (full unicode codepoints). 
	 *     
	 * @param minSuffixCount1
	 *    If pruning the input graph during construction, this threshold is used for telling
	 *    if a node is kept or pruned. If transition_count(node) &gt;= minSuffixCount1, the node
	 *    is kept. 
	 *    
	 * @param minSuffixCount2
	 *    (Note: only Mike McCandless knows what this one is really doing...) 
	 * 
	 * @param doShareSuffix 
	 *    If <code>true</code>, the shared suffixes will be compacted into unique paths.
	 *    This requires an additional hash map for lookups in memory. Setting this parameter to
	 *    <code>false</code> creates a single path for all input sequences. This will result in a larger
	 *    graph, but may require less memory and will speed up construction.  
	 *
	 * @param doShareNonSingletonNodes
	 *    Only used if doShareSuffix is true.  Set this to
	 *    true to ensure FST is fully minimal, at cost of more
	 *    CPU and more RAM during building.
	 *
	 * @param shareMaxTailLength
	 *    Only used if doShareSuffix is true.  Set this to
	 *    Integer.MAX_VALUE to ensure FST is fully minimal, at cost of more
	 *    CPU and more RAM during building.
	 *
	 * @param outputs The output type for each input sequence. Applies only if building an FST. For
	 *    FSA, use {@link NoOutputs#getSingleton()} and {@link NoOutputs#getNoOutput()} as the
	 *    singleton output object.
	 *
	 * @param willPackFST Pass true if you will pack the FST before saving.  This
	 *    causes the FST to create additional data structures internally to facilitate packing, but
	 *    it means the resulting FST cannot be saved: it must
	 *    first be packed using {@link FST#pack(int, int, float)}
	 *
	 * @param acceptableOverheadRatio How to trade speed for space when building the FST. This option
	 *    is only relevant when willPackFST is true. @see PackedInts#getMutable(int, int, float)
	 */
	public Builder(FST.INPUT_TYPE inputType, int minSuffixCount1, int minSuffixCount2, 
			boolean doShareSuffix, boolean doShareNonSingletonNodes, int shareMaxTailLength, 
			Outputs<T> outputs, FreezeTail<T> freezeTail, boolean willPackFST, 
			float acceptableOverheadRatio) {
		mMinSuffixCount1 = minSuffixCount1;
		mMinSuffixCount2 = minSuffixCount2;
		mFreezeTail = freezeTail;
		mDoShareNonSingletonNodes = doShareNonSingletonNodes;
		mShareMaxTailLength = shareMaxTailLength;
		
		mFst = new FST<T>(inputType, outputs, willPackFST, acceptableOverheadRatio);
		if (doShareSuffix) 
			mDedupHash = new NodeHash<T>(mFst);
		else 
			mDedupHash = null;
		
		NO_OUTPUT = outputs.getNoOutput();

		@SuppressWarnings({"unchecked"}) 
		final UnCompiledNode<T>[] f = (UnCompiledNode<T>[]) new UnCompiledNode[10];
		mFrontier = f;
		for (int idx=0; idx < mFrontier.length; idx++) {
			mFrontier[idx] = new UnCompiledNode<T>(this, idx);
		}
	}

	public int getTotStateCount() {
		return mFst.getFstNodeCount();
	}

	public long getTermCount() {
		return mFrontier[0].getInputCount();
	}

	public int getMappedStateCount() {
		return mDedupHash == null ? 0 : mFst.getFstNodeCount();
	}

	/** 
	 * Pass false to disable the array arc optimization
	 *  while building the FST; this will make the resulting
	 *  FST smaller but slower to traverse. 
	 */
	public void setAllowArrayArcs(boolean b) {
		mFst.setAllowArrayArcs(b);
	}

	private CompiledNode compileNode(UnCompiledNode<T> nodeIn, int tailLength) 
			throws IOException {
		final int node;
		if (mDedupHash != null && (mDoShareNonSingletonNodes || nodeIn.getNumArcs() <= 1) && 
			tailLength <= mShareMaxTailLength) {
			if (nodeIn.getNumArcs() == 0) 
				node = mFst.addNode(nodeIn);
			else 
				node = mDedupHash.add(nodeIn);
			
		} else {
			node = mFst.addNode(nodeIn);
		}
		
		assert node != -2;
		nodeIn.clear();

		final CompiledNode fn = new CompiledNode();
		fn.setNode(node);
		
		return fn;
	}

	private void freezeTail(int prefixLenPlus1) throws IOException {
		if (mFreezeTail != null) {
			// Custom plugin:
			mFreezeTail.freeze(mFrontier, prefixLenPlus1, mLastInput);
			
		} else {
			final int downTo = Math.max(1, prefixLenPlus1);
			for (int idx=mLastInput.mLength; idx >= downTo; idx--) {
				boolean doPrune = false;
				boolean doCompile = false;

				final UnCompiledNode<T> node = mFrontier[idx];
				final UnCompiledNode<T> parent = mFrontier[idx-1];

				if (node.getInputCount() < mMinSuffixCount1) {
					doPrune = true;
					doCompile = true;
					
				} else if (idx > prefixLenPlus1) {
					// prune if parent's inputCount is less than suffixMinCount2
					if (parent.getInputCount() < mMinSuffixCount2 || (mMinSuffixCount2 == 1 && 
						parent.getInputCount() == 1 && idx > 1)) {
						// my parent, about to be compiled, doesn't make the cut, so
						// I'm definitely pruned 

						// if minSuffixCount2 is 1, we keep only up
						// until the 'distinguished edge', ie we keep only the
						// 'divergent' part of the FST. if my parent, about to be
						// compiled, has inputCount 1 then we are already past the
						// distinguished edge.  NOTE: this only works if
						// the FST outputs are not "compressible" (simple
						// ords ARE compressible).
						doPrune = true;
					} else {
						// my parent, about to be compiled, does make the cut, so
						// I'm definitely not pruned 
						doPrune = false;
					}
					doCompile = true;
				} else {
					// if pruning is disabled (count is 0) we can always
					// compile current node
					doCompile = mMinSuffixCount2 == 0;
				}

				if (node.getInputCount() < mMinSuffixCount2 || (mMinSuffixCount2 == 1 && 
					node.getInputCount() == 1 && idx > 1)) {
					// drop all arcs
					for (int arcIdx=0; arcIdx < node.getNumArcs(); arcIdx++) {
						@SuppressWarnings({"unchecked"}) 
						final UnCompiledNode<T> target = (UnCompiledNode<T>) node.getArcAt(arcIdx).getTarget();
						target.clear();
					}
					node.setNumArcs(0);
				}

				if (doPrune) {
					// this node doesn't make it -- deref it
					node.clear();
					parent.deleteLast(mLastInput.mInts[mLastInput.mOffset+idx-1], node);
					
				} else {
					if (mMinSuffixCount2 != 0) 
						compileAllTargets(node, mLastInput.mLength-idx);
					
					final T nextFinalOutput = node.getOutput();

					// We "fake" the node as being final if it has no
					// outgoing arcs; in theory we could leave it
					// as non-final (the FST can represent this), but
					// FSTEnum, Util, etc., have trouble w/ non-final
					// dead-end states:
					final boolean isFinal = node.isFinal() || node.getNumArcs() == 0;

					if (doCompile) {
						// this node makes it and we now compile it.  first,
						// compile any targets that were previously
						// undecided:
						parent.replaceLast(mLastInput.mInts[mLastInput.mOffset + idx-1],
								compileNode(node, 1+mLastInput.mLength-idx),
								nextFinalOutput, isFinal);
						
					} else {
						// replaceLast just to install
						// nextFinalOutput/isFinal onto the arc
						parent.replaceLast(mLastInput.mInts[mLastInput.mOffset + idx-1],
								node, nextFinalOutput, isFinal);
						
						// this node will stay in play for now, since we are
						// undecided on whether to prune it.  later, it
						// will be either compiled or pruned, so we must
						// allocate a new node:
						mFrontier[idx] = new UnCompiledNode<T>(this, idx);
					}
				}
			}
		}
	}

	/** 
	 * It's OK to add the same input twice in a row with
	 *  different outputs, as long as outputs impls the merge
	 *  method. Note that input is fully consumed after this
	 *  method is returned (so caller is free to reuse), but
	 *  output is not.  So if your outputs are changeable (eg
	 *  {@link ByteSequenceOutputs} or {@link
	 *  IntSequenceOutputs}) then you cannot reuse across
	 *  calls. 
	 */
	public void add(IntsRef input, T output) throws IOException {
		// De-dup NO_OUTPUT since it must be a singleton:
		if (output.equals(NO_OUTPUT)) 
			output = NO_OUTPUT;

		assert mLastInput.mLength == 0 || input.compareTo(mLastInput) >= 0: 
				"inputs are added out of order lastInput=" + mLastInput + " vs input=" + input;
		assert validOutput(output);

		if (input.mLength == 0) {
			// empty input: only allowed as first input.  we have
			// to special case this because the packed FST
			// format cannot represent the empty input since
			// 'finalness' is stored on the incoming arc, not on
			// the node
			mFrontier[0].increaseInputCount(1);
			mFrontier[0].setIsFinal(true);
			mFst.setEmptyOutput(output);
			return;
		}

		// compare shared prefix length
		int pos1 = 0;
		int pos2 = input.mOffset;
		
		final int pos1Stop = Math.min(mLastInput.mLength, input.mLength);
		
		while (true) {
			mFrontier[pos1].increaseInputCount(1);
			if (pos1 >= pos1Stop || mLastInput.mInts[pos1] != input.mInts[pos2]) 
				break;
			
			pos1 ++;
			pos2 ++;
		}
		
		final int prefixLenPlus1 = pos1+1;
      
		if (mFrontier.length < input.mLength+1) {
			@SuppressWarnings({"unchecked"}) 
			final UnCompiledNode<T>[] next = new UnCompiledNode[ArrayUtil.oversize(
					input.mLength+1, JvmUtil.NUM_BYTES_OBJECT_REF)];
			
			System.arraycopy(mFrontier, 0, next, 0, mFrontier.length);
			
			for (int idx=mFrontier.length; idx < next.length; idx++) {
				next[idx] = new UnCompiledNode<T>(this, idx);
			}
			
			mFrontier = next;
		}

		// minimize/compile states from previous input's
		// orphan'd suffix
		freezeTail(prefixLenPlus1);

		// init tail states for current input
		for (int idx=prefixLenPlus1; idx <= input.mLength; idx++) {
			mFrontier[idx-1].addArc(input.mInts[input.mOffset + idx - 1], mFrontier[idx]);
			mFrontier[idx].increaseInputCount(1);
		}

		final UnCompiledNode<T> lastNode = mFrontier[input.mLength];
		lastNode.setIsFinal(true);
		lastNode.setOutput(NO_OUTPUT);

		// push conflicting outputs forward, only as far as
		// needed
		for (int idx=1; idx < prefixLenPlus1; idx++) {
			final UnCompiledNode<T> node = mFrontier[idx];
			final UnCompiledNode<T> parentNode = mFrontier[idx-1];

			final T lastOutput = parentNode.getLastOutput(input.mInts[input.mOffset + idx - 1]);
			assert validOutput(lastOutput);

			final T commonOutputPrefix;
			final T wordSuffix;

			if (lastOutput != NO_OUTPUT) {
				commonOutputPrefix = mFst.getOutputs().common(output, lastOutput);
				assert validOutput(commonOutputPrefix);
				
				wordSuffix = mFst.getOutputs().subtract(lastOutput, commonOutputPrefix);
				assert validOutput(wordSuffix);
				
				parentNode.setLastOutput(input.mInts[input.mOffset + idx - 1], commonOutputPrefix);
				node.prependOutput(wordSuffix);
				
			} else {
				commonOutputPrefix = wordSuffix = NO_OUTPUT;
			}

			output = mFst.getOutputs().subtract(output, commonOutputPrefix);
			assert validOutput(output);
		}

		if (mLastInput.mLength == input.mLength && prefixLenPlus1 == 1+input.mLength) {
			// same input more than 1 time in a row, mapping to
			// multiple outputs
			lastNode.setOutput(mFst.getOutputs().merge(lastNode.getOutput(), output));
			
		} else {
			// this new arc is private to this new input; set its
			// arc output to the leftover output:
			mFrontier[prefixLenPlus1-1].setLastOutput(
					input.mInts[input.mOffset + prefixLenPlus1-1], output);
		}

		// save last input
		mLastInput.copyInts(input);
	}

	protected boolean validOutput(T output) {
		return output == NO_OUTPUT || !output.equals(NO_OUTPUT);
	}

	/** 
	 * Returns final FST.  NOTE: this will return null if
	 *  nothing is accepted by the FST. 
	 */
	public FST<T> finish() throws IOException {
		final UnCompiledNode<T> root = mFrontier[0];

		// minimize nodes in the last word's suffix
		freezeTail(0);
		
		if (root.getInputCount() < mMinSuffixCount1 || root.getInputCount() < mMinSuffixCount2 || 
			root.getNumArcs() == 0) {
			if (mFst.getEmptyOutput() == null) {
				return null;
			} else if (mMinSuffixCount1 > 0 || mMinSuffixCount2 > 0) {
				// empty string got pruned
				return null;
			}
		} else {
			if (mMinSuffixCount2 != 0) 
				compileAllTargets(root, mLastInput.mLength);
		}
		
		mFst.finish(compileNode(root, mLastInput.mLength).getNode());

		return mFst;
	}

	private void compileAllTargets(UnCompiledNode<T> node, int tailLength) throws IOException {
		for (int arcIdx=0; arcIdx < node.getNumArcs(); arcIdx++) {
			final BuilderArc<T> arc = node.getArcAt(arcIdx);
			if (!arc.getTarget().isCompiled()) {
				// not yet compiled
				@SuppressWarnings({"unchecked"}) 
				final UnCompiledNode<T> n = (UnCompiledNode<T>) arc.getTarget();
				if (n.getNumArcs() == 0) {
					arc.mIsFinal = true;
					n.setIsFinal(true);
				}
				arc.mTarget = compileNode(n, tailLength-1);
			}
		}
	}

}
