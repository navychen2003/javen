package org.javenstudio.hornet.store.fst;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.IIntsMutable;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.IIntsWriter;
import org.javenstudio.common.indexdb.store.InputStreamDataInput;
import org.javenstudio.common.indexdb.store.OutputStreamDataOutput;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.indexdb.util.IntsRef;
import org.javenstudio.common.indexdb.util.PriorityQueue;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.store.packed.GrowableWriter;
import org.javenstudio.hornet.store.packed.PackedInts;

/** 
 * Represents an finite state machine (FST), using a
 *  compact byte[] format.
 *  <p> The format is similar to what's used by Morfologik
 *  (http://sourceforge.net/projects/morfologik).
 *  
 *  <p> See the {@link org.apache.lucene.util.fst package
 *      documentation} for some simple examples.
 *  <p><b>NOTE</b>: the FST cannot be larger than ~2.1 GB
 *  because it uses int to address the byte[].
 *
 * TODO: break this into WritableFST and ReadOnlyFST.. then
 * we can have subclasses of ReadOnlyFST to handle the
 * different byte[] level encodings (packed or
 * not)... and things like nodeCount, arcCount are read only
 * 
 * TODO: if FST is pure prefix trie we can do a more compact
 * job, ie, once we are at a 'suffix only', just store the
 * completion labels as a string not as a series of arcs.
 * 
 * TODO: maybe make an explicit thread state that holds
 * reusable stuff eg BytesReader, a scratch arc
 * 
 * NOTE: while the FST is able to represent a non-final
 * dead-end state (NON_FINAL_END_NODE=0), the layers above
 * (FSTEnum, Util) have problems with this!!
 * 
 */
@SuppressWarnings("unused")
public final class FST<T> {
	/** 
	 * Specifies allowed range of each int input label for
	 *  this FST. 
	 */
	public static enum INPUT_TYPE {BYTE1, BYTE2, BYTE4};

	final static int BIT_FINAL_ARC = 1 << 0;
	final static int BIT_LAST_ARC = 1 << 1;
	final static int BIT_TARGET_NEXT = 1 << 2;

	// TODO: we can free up a bit if we can nuke this:
	final static int BIT_STOP_NODE = 1 << 3;
	final static int BIT_ARC_HAS_OUTPUT = 1 << 4;
	final static int BIT_ARC_HAS_FINAL_OUTPUT = 1 << 5;

	// Arcs are stored as fixed-size (per entry) array, so
	// that we can find an arc using binary search.  We do
	// this when number of arcs is > NUM_ARCS_ARRAY:

	// If set, the target node is delta coded vs current
	// position:
	private final static int BIT_TARGET_DELTA = 1 << 6;

	private final static byte ARCS_AS_FIXED_ARRAY = BIT_ARC_HAS_FINAL_OUTPUT;

	/**
	 * @see #shouldExpand(UnCompiledNode)
	 */
	final static int FIXED_ARRAY_SHALLOW_DISTANCE = 3; // 0 => only root node.

	/**
	 * @see #shouldExpand(UnCompiledNode)
	 */
	final static int FIXED_ARRAY_NUM_ARCS_SHALLOW = 5;

	/**
	 * @see #shouldExpand(UnCompiledNode)
	 */
	final static int FIXED_ARRAY_NUM_ARCS_DEEP = 10;

	// Increment version to change it
	private final static String FILE_FORMAT_NAME = "FST";
	private final static int VERSION_START = 0;

	/** Changed numBytesPerArc for array'd case from byte to int. */
	private final static int VERSION_INT_NUM_BYTES_PER_ARC = 1;

	/** Write BYTE2 labels as 2-byte short, not vInt. */
	private final static int VERSION_SHORT_BYTE2_LABELS = 2;

	/** Added optional packed format. */
	private final static int VERSION_PACKED = 3;

	private final static int VERSION_CURRENT = VERSION_PACKED;

	// Never serialized; just used to represent the virtual
	// final node w/ no arcs:
	private final static int FINAL_END_NODE = -1;

	// Never serialized; just used to represent the virtual
	// non-final node w/ no arcs:
	private final static int NON_FINAL_END_NODE = 0;

	// If arc has this label then that arc is final/accepted
	public static final int END_LABEL = -1;
	
	private final INPUT_TYPE mInputType;
	private int[] mBytesPerArc = new int[0];
	
	// if non-null, this FST accepts the empty string and
	// produces this output
	private T mEmptyOutput;
	private byte[] mEmptyOutputBytes;

	// Not private to avoid synthetic access$NNN methods:
	protected byte[] mBytes;
	private int mByteUpto = 0;

	private int mStartNode = -1;

	private final Outputs<T> mOutputs;

	private int mLastFrozenNode;

	public final T NO_OUTPUT;

	private int mNodeCount;
	private int mArcCount;
	private int mArcWithOutputCount;

	private final boolean mPacked;
	private IIntsReader mNodeRefToAddress;

	private boolean mAllowArrayArcs = true;

	private FSTArc<T> mCachedRootArcs[];

	private final BytesWriter mWriter;

	private IIntsWriter mNodeAddress;

	// TODO: we could be smarter here, and prune periodically
	// as we go; high in-count nodes will "usually" become
	// clear early on:
	private IIntsWriter mInCounts;

	public final int getFstNodeCount() { return mNodeCount; }
	public final Outputs<T> getOutputs() { return mOutputs; }
	public final byte[] getBytes() { return mBytes; }
	
	protected static boolean flag(int flags, int bit) {
		return (flags & bit) != 0;
	}
	
	// make a new empty FST, for building; Builder invokes
	// this ctor
	FST(INPUT_TYPE inputType, Outputs<T> outputs, boolean willPackFST, 
			float acceptableOverheadRatio) {
		mInputType = inputType;
		mOutputs = outputs;
		mBytes = new byte[128];
		NO_OUTPUT = outputs.getNoOutput();
		
		if (willPackFST) {
			mNodeAddress = new GrowableWriter(PackedInts.bitsRequired(mBytes.length - 1), 
					8, acceptableOverheadRatio);
			mInCounts = new GrowableWriter(1, 8, acceptableOverheadRatio);
		} else {
			mNodeAddress = null;
			mInCounts = null;
		}
    
		mWriter = new BytesWriter(this);

		mEmptyOutput = null;
		mPacked = false;
		mNodeRefToAddress = null;
	}

	/** Load a previously saved FST. */
	public FST(IDataInput in, Outputs<T> outputs) throws IOException {
		mOutputs = outputs;
		mWriter = null;
		
		// NOTE: only reads most recent format; we don't have
		// back-compat promise for FSTs (they are experimental):
		CodecUtil.checkHeader(in, FILE_FORMAT_NAME, VERSION_PACKED, VERSION_PACKED);
		mPacked = in.readByte() == 1;
		
		if (in.readByte() == 1) {
			// accepts empty string
			int numBytes = in.readVInt();
			// messy
			mBytes = new byte[numBytes];
			in.readBytes(mBytes, 0, numBytes);
			if (mPacked) 
				mEmptyOutput = outputs.read(getBytesReader(0));
			else 
				mEmptyOutput = outputs.read(getBytesReader(numBytes-1));
			
		} else {
			mEmptyOutput = null;
		}
		
		final byte t = in.readByte();
		switch(t) {
		case 0:
			mInputType = INPUT_TYPE.BYTE1;
			break;
		case 1:
			mInputType = INPUT_TYPE.BYTE2;
			break;
		case 2:
			mInputType = INPUT_TYPE.BYTE4;
			break;
		default:
			throw new IllegalStateException("invalid input type " + t);
		}
		
		if (mPacked) 
			mNodeRefToAddress = null; //PackedInts.getReader(in);
		else 
			mNodeRefToAddress = null;
		
		mStartNode = in.readVInt();
		mNodeCount = in.readVInt();
		mArcCount = in.readVInt();
		mArcWithOutputCount = in.readVInt();

		mBytes = new byte[in.readVInt()];
		in.readBytes(mBytes, 0, mBytes.length);
		
		NO_OUTPUT = outputs.getNoOutput();

		cacheRootArcs();
	}

	public INPUT_TYPE getInputType() {
		return mInputType;
	}

	/** Returns bytes used to represent the FST */
	public int sizeInBytes() {
		int size = mBytes.length;
		if (mPacked) {
			size += mNodeRefToAddress.ramBytesUsed();
		} else if (mNodeAddress != null) {
			size += mNodeAddress.ramBytesUsed();
			size += mInCounts.ramBytesUsed();
		}
		return size;
	}

	protected void finish(int startNode) throws IOException {
		if (startNode == FINAL_END_NODE && mEmptyOutput != null) 
			startNode = 0;
		
		if (mStartNode != -1) 
			throw new IllegalStateException("already finished");
		
		byte[] finalBytes = new byte[mWriter.getPosWrite()];
		System.arraycopy(mBytes, 0, finalBytes, 0, mWriter.getPosWrite());
		
		mBytes = finalBytes;
		mStartNode = startNode;

		cacheRootArcs();
	}

	private int getNodeAddress(int node) {
		if (mNodeAddress != null) {
			// Deref
			return (int) mNodeAddress.get(node);
		} else {
			// Straight
			return node;
		}
	}

	// Caches first 128 labels
	@SuppressWarnings({"unchecked"})
	private void cacheRootArcs() throws IOException {
		mCachedRootArcs = (FSTArc<T>[]) new FSTArc[0x80];
		
		final FSTArc<T> arc = new FSTArc<T>();
		getFirstArc(arc);
		
		final BytesReader in = getBytesReader(0);
		
		if (targetHasArcs(arc)) {
			readFirstRealTargetArc(arc.getTarget(), arc, in);
			
			while (true) {
				assert arc.getLabel() != END_LABEL;
				if (arc.getLabel() < mCachedRootArcs.length) 
					mCachedRootArcs[arc.getLabel()] = new FSTArc<T>().copyFrom(arc);
				else 
					break;
				
				if (arc.isLast()) 
					break;
				
				readNextRealArc(arc, in);
			}
		}
	}

	public T getEmptyOutput() {
		return mEmptyOutput;
	}

	protected void setEmptyOutput(T v) throws IOException {
		if (mEmptyOutput != null) 
			mEmptyOutput = mOutputs.merge(mEmptyOutput, v);
		else 
			mEmptyOutput = v;
		
		// TODO: this is messy -- replace with sillyBytesWriter; maybe make
		// bytes private
		final int posSave = mWriter.getPosWrite();
		mOutputs.write(mEmptyOutput, mWriter);
		mEmptyOutputBytes = new byte[mWriter.getPosWrite()-posSave];

		if (!mPacked) {
			// reverse
			final int stopAt = (mWriter.getPosWrite() - posSave)/2;
			int upto = 0;
			while (upto < stopAt) {
				final byte b = mBytes[posSave + upto];
				mBytes[posSave+upto] = mBytes[mWriter.getPosWrite()-upto-1];
				mBytes[mWriter.getPosWrite()-upto-1] = b;
				upto ++;
			}
		}
		
		System.arraycopy(mBytes, posSave, mEmptyOutputBytes, 0, mWriter.getPosWrite()-posSave);
		mWriter.mPosWrite = posSave;
	}

	public void save(IDataOutput out) throws IOException {
		if (mStartNode == -1) 
			throw new IllegalStateException("call finish first");
		
		if (mNodeAddress != null) 
			throw new IllegalStateException("cannot save an FST pre-packed FST; it must first be packed");
		
		if (mPacked && !(mNodeRefToAddress instanceof IIntsMutable)) 
			throw new IllegalStateException("cannot save a FST which has been loaded from disk ");
		
		CodecUtil.writeHeader(out, FILE_FORMAT_NAME, VERSION_CURRENT);
		if (mPacked) 
			out.writeByte((byte) 1);
		else 
			out.writeByte((byte) 0);
		
		// TODO: really we should encode this as an arc, arriving
		// to the root node, instead of special casing here:
		if (mEmptyOutput != null) {
			out.writeByte((byte) 1);
			out.writeVInt(mEmptyOutputBytes.length);
			out.writeBytes(mEmptyOutputBytes, 0, mEmptyOutputBytes.length);
		} else {
			out.writeByte((byte) 0);
		}
		
		final byte t;
		if (mInputType == INPUT_TYPE.BYTE1) 
			t = 0;
		else if (mInputType == INPUT_TYPE.BYTE2) 
			t = 1;
		else 
			t = 2;
		
		out.writeByte(t);
		if (mPacked) {
			((IIntsMutable) mNodeRefToAddress).save(out);
		}
		
		out.writeVInt(mStartNode);
		out.writeVInt(mNodeCount);
		out.writeVInt(mArcCount);
		out.writeVInt(mArcWithOutputCount);
		out.writeVInt(mBytes.length);
		out.writeBytes(mBytes, 0, mBytes.length);
	}
  
	/**
	 * Writes an automaton to a file. 
	 */
	public void save(final File file) throws IOException {
		boolean success = false;
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		try {
			save(new OutputStreamDataOutput(os));
			success = true;
		} finally { 
			if (success) 
				IOUtils.close(os);
			else 
				IOUtils.closeWhileHandlingException(os); 
		}
	}

	/**
	 * Reads an automaton from a file. 
	 */
	public static <T> FST<T> read(File file, Outputs<T> outputs) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		boolean success = false;
		try {
			FST<T> fst = new FST<T>(new InputStreamDataInput(is), outputs);
			success = true;
			return fst;
		} finally {
			if (success) 
				IOUtils.close(is);
			else 
				IOUtils.closeWhileHandlingException(is); 
		}
	}

	private void writeLabel(int v) throws IOException {
		assert v >= 0: "v=" + v;
		if (mInputType == INPUT_TYPE.BYTE1) {
			assert v <= 255: "v=" + v;
			mWriter.writeByte((byte) v);
		} else if (mInputType == INPUT_TYPE.BYTE2) {
			assert v <= 65535: "v=" + v;
			mWriter.writeShort((short) v);
		} else {
			//writeInt(v);
			mWriter.writeVInt(v);
		}
	}

	protected int readLabel(IDataInput in) throws IOException {
		final int v;
		if (mInputType == INPUT_TYPE.BYTE1) {
			// Unsigned byte:
			v = in.readByte()&0xFF;
		} else if (mInputType == INPUT_TYPE.BYTE2) {
			// Unsigned short:
			v = in.readShort()&0xFFFF;
		} else { 
			v = in.readVInt();
		}
		return v;
	}

	// returns true if the node at this address has any
	// outgoing arcs
	public static<T> boolean targetHasArcs(FSTArc<T> arc) {
		return arc.getTarget() > 0;
	}

	// serializes new node by appending its bytes to the end
	// of the current byte[]
	protected int addNode(UnCompiledNode<T> nodeIn) throws IOException {
		if (nodeIn.getNumArcs() == 0) {
			if (nodeIn.isFinal()) 
				return FINAL_END_NODE;
			else 
				return NON_FINAL_END_NODE;
		}

		int startAddress = mWriter.getPosWrite();

		final boolean doFixedArray = shouldExpand(nodeIn);
		final int fixedArrayStart;
		
		if (doFixedArray) {
			if (mBytesPerArc.length < nodeIn.getNumArcs()) 
				mBytesPerArc = new int[ArrayUtil.oversize(nodeIn.getNumArcs(), 1)];
			
			// write a "false" first arc:
			mWriter.writeByte(ARCS_AS_FIXED_ARRAY);
			mWriter.writeVInt(nodeIn.getNumArcs());
			// placeholder -- we'll come back and write the number
			// of bytes per arc (int) here:
			// TODO: we could make this a vInt instead
			mWriter.writeInt(0);
			fixedArrayStart = mWriter.getPosWrite();
			
		} else {
			fixedArrayStart = 0;
		}

		mArcCount += nodeIn.getNumArcs();
    
		final int lastArc = nodeIn.getNumArcs()-1;

		int lastArcStart = mWriter.getPosWrite();
		int maxBytesPerArc = 0;
		
		for (int arcIdx=0; arcIdx < nodeIn.getNumArcs(); arcIdx++) {
			final BuilderArc<T> arc = nodeIn.getArcAt(arcIdx);
			final CompiledNode target = (CompiledNode) arc.getTarget();
			int flags = 0;

			if (arcIdx == lastArc) 
				flags += BIT_LAST_ARC;

			if (mLastFrozenNode == target.getNode() && !doFixedArray) {
				// TODO: for better perf (but more RAM used) we
				// could avoid this except when arc is "near" the
				// last arc:
				flags += BIT_TARGET_NEXT;
			}

			if (arc.isFinal()) {
				flags += BIT_FINAL_ARC;
				if (arc.getNextFinalOutput() != NO_OUTPUT) 
					flags += BIT_ARC_HAS_FINAL_OUTPUT;
				
			} else {
				assert arc.getNextFinalOutput() == NO_OUTPUT;
			}

			boolean targetHasArcs = target.getNode() > 0;

			if (!targetHasArcs) {
				flags += BIT_STOP_NODE;
			} else if (mInCounts != null) {
				mInCounts.set(target.getNode(), mInCounts.get(target.getNode()) + 1);
			}

			if (arc.getOutput() != NO_OUTPUT) 
				flags += BIT_ARC_HAS_OUTPUT;

			mWriter.writeByte((byte) flags);
			writeLabel(arc.getLabel());

			if (arc.getOutput() != NO_OUTPUT) {
				mOutputs.write(arc.getOutput(), mWriter);
				mArcWithOutputCount ++;
			}

			if (arc.getNextFinalOutput() != NO_OUTPUT) 
				mOutputs.write(arc.getNextFinalOutput(), mWriter);

			if (targetHasArcs && (flags & BIT_TARGET_NEXT) == 0) {
				assert target.getNode() > 0;
				mWriter.writeInt(target.getNode());
			}

			// just write the arcs "like normal" on first pass,
			// but record how many bytes each one took, and max
			// byte size:
			if (doFixedArray) {
				mBytesPerArc[arcIdx] = mWriter.getPosWrite() - lastArcStart;
				lastArcStart = mWriter.getPosWrite();
				maxBytesPerArc = Math.max(maxBytesPerArc, mBytesPerArc[arcIdx]);
			}
		}

		// TODO: if arc'd arrays will be "too wasteful" by some
		// measure, eg if arcs have vastly different sized
		// outputs, then we should selectively disable array for
		// such cases

		if (doFixedArray) {
			assert maxBytesPerArc > 0;
			// 2nd pass just "expands" all arcs to take up a fixed
			// byte size
			final int sizeNeeded = fixedArrayStart + nodeIn.getNumArcs() * maxBytesPerArc;
			
			mBytes = ArrayUtil.grow(mBytes, sizeNeeded);
			// TODO: we could make this a vInt instead
			mBytes[fixedArrayStart-4] = (byte) (maxBytesPerArc >> 24);
			mBytes[fixedArrayStart-3] = (byte) (maxBytesPerArc >> 16);
			mBytes[fixedArrayStart-2] = (byte) (maxBytesPerArc >> 8);
			mBytes[fixedArrayStart-1] = (byte) maxBytesPerArc;

			// expand the arcs in place, backwards
			int srcPos = mWriter.getPosWrite();
			int destPos = fixedArrayStart + nodeIn.getNumArcs()*maxBytesPerArc;
			mWriter.mPosWrite = destPos;
			
			for (int arcIdx=nodeIn.getNumArcs()-1; arcIdx >= 0; arcIdx--) {
				destPos -= maxBytesPerArc;
				srcPos -= mBytesPerArc[arcIdx];
				if (srcPos != destPos) {
					assert destPos > srcPos;
					System.arraycopy(mBytes, srcPos, mBytes, destPos, mBytesPerArc[arcIdx]);
				}
			}
		}

		// reverse bytes in-place; we do this so that the
		// "BIT_TARGET_NEXT" opto can work, ie, it reads the
		// node just before the current one
		final int endAddress = mWriter.getPosWrite() - 1;

		int left = startAddress;
		int right = endAddress;
		
		while (left < right) {
			final byte b = mBytes[left];
			mBytes[left++] = mBytes[right];
			mBytes[right--] = b;
		}

		mNodeCount ++;
		
		final int node;
		if (mNodeAddress != null) {
			// Nodes are addressed by 1+ord:
			if (mNodeCount == mNodeAddress.size()) {
				mNodeAddress = mNodeAddress.resize(
						ArrayUtil.oversize(mNodeAddress.size() + 1, mNodeAddress.getBitsPerValue()));
				mInCounts = mInCounts.resize(
						ArrayUtil.oversize(mInCounts.size() + 1, mInCounts.getBitsPerValue()));
			}
			mNodeAddress.set(mNodeCount, endAddress);
			node = mNodeCount;
			
		} else {
			node = endAddress;
		}
		
		mLastFrozenNode = node;

		return node;
	}

	/** 
	 * Fills virtual 'start' arc, ie, an empty incoming arc to
	 *  the FST's start node 
	 */
	public FSTArc<T> getFirstArc(FSTArc<T> arc) {
		if (mEmptyOutput != null) {
			arc.setFlags(BIT_FINAL_ARC | BIT_LAST_ARC);
			arc.setNextFinalOutput(mEmptyOutput);
		} else {
			arc.setFlags(BIT_LAST_ARC);
			arc.setNextFinalOutput(NO_OUTPUT);
		}
		
		arc.setOutput(NO_OUTPUT);

		// If there are no nodes, ie, the FST only accepts the
		// empty string, then startNode is 0
		arc.setTarget(mStartNode);
		
		return arc;
	}

	/** 
	 * Follows the <code>follow</code> arc and reads the last
	 *  arc of its target; this changes the provided
	 *  <code>arc</code> (2nd arg) in-place and returns it.
	 * 
	 * @return Returns the second argument
	 * (<code>arc</code>). 
	 */
	public FSTArc<T> readLastTargetArc(FSTArc<T> follow, FSTArc<T> arc, 
			BytesReader in) throws IOException {
		if (!targetHasArcs(follow)) {
			assert follow.isFinal();
			
			arc.setLabel(END_LABEL);
			arc.setTarget(FINAL_END_NODE);
			arc.setOutput(follow.getNextFinalOutput());
			arc.setFlags(BIT_LAST_ARC);
			
			return arc;
			
		} else {
			in.mPos = getNodeAddress(follow.getTarget());
			arc.setNode(follow.getTarget());
			
			final byte b = in.readByte();
			if (b == ARCS_AS_FIXED_ARRAY) {
				// array: jump straight to end
				arc.setNumArcs(in.readVInt());
				if (mPacked) 
					arc.setBytesPerArc(in.readVInt());
				else 
					arc.setBytesPerArc(in.readInt());
				
				arc.setPosArcsStart(in.getPosition());
				arc.setArcIndex(arc.getNumArcs() - 2);
				
			} else {
				arc.setFlags(b);
				// non-array: linear scan
				arc.setBytesPerArc(0);
				
				while (!arc.isLast()) {
					// skip this arc:
					readLabel(in);
					
					if (arc.flag(BIT_ARC_HAS_OUTPUT)) 
						mOutputs.read(in);
					
					if (arc.flag(BIT_ARC_HAS_FINAL_OUTPUT)) 
						mOutputs.read(in);
					
					if (arc.flag(BIT_STOP_NODE)) {
					} else if (arc.flag(BIT_TARGET_NEXT)) {
					} else {
						if (mPacked) 
							in.readVInt();
						else 
							in.skip(4);
					}
					
					arc.setFlags(in.readByte());
				}
				
				// Undo the byte flags we read: 
				in.skip(-1);
				arc.setNextArc(in.getPosition());
			}
			
			readNextRealArc(arc, in);
			assert arc.isLast();
			
			return arc;
		}
	}

	/**
	 * Follow the <code>follow</code> arc and read the first arc of its target;
	 * this changes the provided <code>arc</code> (2nd arg) in-place and returns
	 * it.
	 * 
	 * @return Returns the second argument (<code>arc</code>).
	 */
	public FSTArc<T> readFirstTargetArc(FSTArc<T> follow, FSTArc<T> arc, 
			BytesReader in) throws IOException {
		if (follow.isFinal()) {
			// Insert "fake" final first arc:
			arc.setLabel(END_LABEL);
			arc.setOutput(follow.getNextFinalOutput());
			arc.setFlags(BIT_FINAL_ARC);
			
			if (follow.getTarget() <= 0) {
				arc.setFlags(arc.getFlags() | BIT_LAST_ARC);
			} else {
				arc.setNode(follow.getTarget());
				// NOTE: nextArc is a node (not an address!) in this case:
				arc.setNextArc(follow.getTarget());
			}
			
			arc.setTarget(FINAL_END_NODE);
			return arc;
			
		} else {
			return readFirstRealTargetArc(follow.getTarget(), arc, in);
		}
	}

	public FSTArc<T> readFirstRealTargetArc(int node, FSTArc<T> arc, 
			final BytesReader in) throws IOException {
		assert in.mBytes == mBytes;
		
		final int address = getNodeAddress(node);
		in.mPos = address;
		arc.setNode(node);

		if (in.readByte() == ARCS_AS_FIXED_ARRAY) {
			// this is first arc in a fixed-array
			arc.setNumArcs(in.readVInt());
			if (mPacked) 
				arc.setBytesPerArc(in.readVInt());
			else 
				arc.setBytesPerArc(in.readInt());
			
			arc.setArcIndex(-1);
			arc.setPosArcsStart(in.getPosition());
			arc.setNextArc(arc.getPosArcsStart());
			
		} else {
			//arc.flags = b;
			arc.setNextArc(address);
			arc.setBytesPerArc(0);
		}

		return readNextRealArc(arc, in);
	}

	/**
	 * Checks if <code>arc</code>'s target state is in expanded (or vector) format. 
	 * 
	 * @return Returns <code>true</code> if <code>arc</code> points to a state in an
	 * expanded array format.
	 */
	protected boolean isExpandedTarget(FSTArc<T> follow, BytesReader in) throws IOException {
		if (!targetHasArcs(follow)) {
			return false;
		} else {
			in.mPos = getNodeAddress(follow.getTarget());
			return in.readByte() == ARCS_AS_FIXED_ARRAY;
		}
	}

	/** In-place read; returns the arc. */
	public FSTArc<T> readNextArc(FSTArc<T> arc, BytesReader in) throws IOException {
		if (arc.getLabel() == END_LABEL) {
			// This was a fake inserted "final" arc
			if (arc.getNextArc() <= 0) 
				throw new IllegalArgumentException("cannot readNextArc when arc.isLast()=true");
			
			return readFirstRealTargetArc(arc.getNextArc(), arc, in);
			
		} else {
			return readNextRealArc(arc, in);
		}
	}

	/** 
	 * Peeks at next arc's label; does not alter arc.  Do
	 *  not call this if arc.isLast()! 
	 */
	public int readNextArcLabel(FSTArc<T> arc, BytesReader in) throws IOException {
		assert !arc.isLast();

		if (arc.getLabel() == END_LABEL) {
			in.mPos = getNodeAddress(arc.getNextArc());
			final byte b = mBytes[in.getPosition()];
			
			if (b == ARCS_AS_FIXED_ARRAY) {
				in.skip(1);
				in.readVInt();
				if (mPacked) 
					in.readVInt();
				else 
					in.readInt();
			}
		} else {
			if (arc.getBytesPerArc() != 0) {
				// arcs are at fixed entries
				in.mPos = arc.getPosArcsStart();
				in.skip((1+arc.getArcIndex())*arc.getBytesPerArc());
			} else {
				// arcs are packed
				in.mPos = arc.getNextArc();
			}
		}
		
		// skip flags
		in.readByte();
		
		return readLabel(in);
	}

	/** 
	 * Never returns null, but you should never call this if
	 *  arc.isLast() is true. 
	 */
	public FSTArc<T> readNextRealArc(FSTArc<T> arc, final BytesReader in) throws IOException {
		assert in.mBytes == mBytes;

		// TODO: can't assert this because we call from readFirstArc
		// assert !flag(arc.flags, BIT_LAST_ARC);

		// this is a continuing arc in a fixed array
		if (arc.getBytesPerArc() != 0) {
			// arcs are at fixed entries
			arc.increaseArcIndex(1);
			assert arc.getArcIndex() < arc.getNumArcs();
			in.skip(arc.getPosArcsStart(), arc.getArcIndex()*arc.getBytesPerArc());
		} else {
			// arcs are packed
			in.mPos = arc.getNextArc();
		}
		
		arc.setFlags(in.readByte());
		arc.setLabel(readLabel(in));

		if (arc.flag(BIT_ARC_HAS_OUTPUT)) 
			arc.setOutput(mOutputs.read(in));
		else 
			arc.setOutput(mOutputs.getNoOutput());

		if (arc.flag(BIT_ARC_HAS_FINAL_OUTPUT)) 
			arc.setNextFinalOutput(mOutputs.read(in));
		else 
			arc.setNextFinalOutput(mOutputs.getNoOutput());

		if (arc.flag(BIT_STOP_NODE)) {
			if (arc.flag(BIT_FINAL_ARC)) 
				arc.setTarget(FINAL_END_NODE);
			else 
				arc.setTarget(NON_FINAL_END_NODE);
			
			arc.setNextArc(in.getPosition());
			
		} else if (arc.flag(BIT_TARGET_NEXT)) {
			arc.setNextArc(in.getPosition());
			
			// TODO: would be nice to make this lazy -- maybe
			// caller doesn't need the target and is scanning arcs...
			if (mNodeAddress == null) {
				if (!arc.flag(BIT_LAST_ARC)) {
					if (arc.getBytesPerArc() == 0) {
						// must scan
						seekToNextNode(in);
					} else {
						in.skip(arc.getPosArcsStart(), arc.getBytesPerArc() * arc.getNumArcs());
					}
				}
				arc.setTarget(in.getPosition());
			} else {
				arc.setTarget(arc.getNode() - 1);
				assert arc.getTarget() > 0;
			}
			
		} else {
			if (mPacked) {
				final int pos = in.getPosition();
				final int code = in.readVInt();
				
				if (arc.flag(BIT_TARGET_DELTA)) {
					// Address is delta-coded from current address:
					arc.setTarget(pos + code);
				} else if (code < mNodeRefToAddress.size()) {
					// Deref
					arc.setTarget((int) mNodeRefToAddress.get(code));
				} else {
					// Absolute
					arc.setTarget(code);
				}
			} else {
				arc.setTarget(in.readInt());
			}
			
			arc.setNextArc(in.getPosition());
		}
		
		return arc;
	}

	/** 
	 * Finds an arc leaving the incoming arc, replacing the arc in place.
	 *  This returns null if the arc was not found, else the incoming arc. 
	 */
	public FSTArc<T> findTargetArc(int labelToMatch, FSTArc<T> follow, FSTArc<T> arc, 
			BytesReader in) throws IOException {
		assert mCachedRootArcs != null;
		assert in.mBytes == mBytes;

		if (labelToMatch == END_LABEL) {
			if (follow.isFinal()) {
				if (follow.getTarget() <= 0) {
					arc.setFlags(BIT_LAST_ARC);
				} else {
					arc.setFlags(0);
					// NOTE: nextArc is a node (not an address!) in this case:
					arc.setNextArc(follow.getTarget());
					arc.setNode(follow.getTarget());
				}
				
				arc.setOutput(follow.getNextFinalOutput());
				arc.setLabel(END_LABEL);
				
				return arc;
				
			} else {
				return null;
			}
		}

		// Short-circuit if this arc is in the root arc cache:
		if (follow.getTarget() == mStartNode && labelToMatch < mCachedRootArcs.length) {
			final FSTArc<T> result = mCachedRootArcs[labelToMatch];
			if (result == null) {
				return result;
			} else {
				arc.copyFrom(result);
				return arc;
			}
		}

		if (!targetHasArcs(follow)) 
			return null;

		in.mPos = getNodeAddress(follow.getTarget());
		arc.setNode(follow.getTarget());

		if (in.readByte() == ARCS_AS_FIXED_ARRAY) {
			// Arcs are full array; do binary search:
			arc.setNumArcs(in.readVInt());
			if (mPacked) 
				arc.setBytesPerArc(in.readVInt());
			else 
				arc.setBytesPerArc(in.readInt());
			
			arc.setPosArcsStart(in.getPosition());
			
			int low = 0;
			int high = arc.getNumArcs()-1;
			
			while (low <= high) {
				int mid = (low + high) >>> 1;
        		in.skip(arc.getPosArcsStart(), arc.getBytesPerArc()*mid + 1);
        		
        		int midLabel = readLabel(in);
        		final int cmp = midLabel - labelToMatch;
        		
        		if (cmp < 0) {
        			low = mid + 1;
        		} else if (cmp > 0) {
        			high = mid - 1;
        		} else {
        			arc.setArcIndex(mid-1);
        			return readNextRealArc(arc, in);
        		}
			}

			return null;
		}

		// Linear scan
		readFirstRealTargetArc(follow.getTarget(), arc, in);

		while (true) {
			// TODO: we should fix this code to not have to create
			// object for the output of every arc we scan... only
			// for the matching arc, if found
			if (arc.getLabel() == labelToMatch) {
				return arc;
			} else if (arc.getLabel() > labelToMatch) {
				return null;
			} else if (arc.isLast()) {
				return null;
			} else {
				readNextRealArc(arc, in);
			}
		}
	}

	private void seekToNextNode(BytesReader in) throws IOException {
		while (true) {
			final int flags = in.readByte();
			readLabel(in);

			if (flag(flags, BIT_ARC_HAS_OUTPUT)) 
				mOutputs.read(in);

			if (flag(flags, BIT_ARC_HAS_FINAL_OUTPUT)) 
				mOutputs.read(in);

			if (!flag(flags, BIT_STOP_NODE) && !flag(flags, BIT_TARGET_NEXT)) {
				if (mPacked) 
					in.readVInt();
				else 
					in.readInt();
			}

			if (flag(flags, BIT_LAST_ARC)) 
				return;
		}
	}

	public int getNodeCount() {
		// 1+ in order to count the -1 implicit final node
		return 1 + mNodeCount;
	}
  
	public int getArcCount() {
		return mArcCount;
	}

	public int getArcWithOutputCount() {
		return mArcWithOutputCount;
	}

	public void setAllowArrayArcs(boolean v) {
		mAllowArrayArcs = v;
	}
  
	/**
	 * Nodes will be expanded if their depth (distance from the root node) is
	 * &lt;= this value and their number of arcs is &gt;=
	 * {@link #FIXED_ARRAY_NUM_ARCS_SHALLOW}.
	 * 
	 * <p>
	 * Fixed array consumes more RAM but enables binary search on the arcs
	 * (instead of a linear scan) on lookup by arc label.
	 * 
	 * @return <code>true</code> if <code>node</code> should be stored in an
	 *         expanded (array) form.
	 * 
	 * @see #FIXED_ARRAY_NUM_ARCS_DEEP
	 * @see Builder.UnCompiledNode#depth
	 */
	private boolean shouldExpand(UnCompiledNode<T> node) {
		return mAllowArrayArcs &&
				((node.getDepth() <= FIXED_ARRAY_SHALLOW_DISTANCE && 
				node.getNumArcs() >= FIXED_ARRAY_NUM_ARCS_SHALLOW) || 
				node.getNumArcs() >= FIXED_ARRAY_NUM_ARCS_DEEP);
	}

	public BytesReader getBytesReader(int pos) {
		// TODO: maybe re-use via ThreadLocal?
		if (mPacked) {
			return new ForwardBytesReader(mBytes, pos);
		} else {
			return new ReverseBytesReader(mBytes, pos);
		}
	}

	// Creates a packed FST
	private FST(INPUT_TYPE inputType, IIntsReader nodeRefToAddress, Outputs<T> outputs) {
		mPacked = true;
		mInputType = inputType;
		mBytes = new byte[128];
		mNodeRefToAddress = nodeRefToAddress;
		mOutputs = outputs;
		NO_OUTPUT = outputs.getNoOutput();
		mWriter = new BytesWriter(this);
	}

	/** 
	 * Expert: creates an FST by packing this one.  This
	 *  process requires substantial additional RAM (currently
	 *  up to ~8 bytes per node depending on
	 *  <code>acceptableOverheadRatio</code>), but then should
	 *  produce a smaller FST.
	 *
	 *  <p>The implementation of this method uses ideas from
	 *  <a target="_blank" href="http://www.cs.put.poznan.pl/dweiss/site/publications/download/fsacomp.pdf">Smaller Representation of Finite State Automata</a>,
	 *  which describes techniques to reduce the size of a FST.
	 *  However, this is not a strict implementation of the
	 *  algorithms described in this paper.
	 */
	public FST<T> pack(int minInCountDeref, int maxDerefNodes, 
			float acceptableOverheadRatio) throws IOException {

		// TODO: other things to try
		//   - renumber the nodes to get more next / better locality?
		//   - allow multiple input labels on an arc, so
		//     singular chain of inputs can take one arc (on
		//     wikipedia terms this could save another ~6%)
		//   - in the ord case, the output '1' is presumably
		//     very common (after NO_OUTPUT)... maybe use a bit
		//     for it..?
		//   - use spare bits in flags.... for top few labels /
		//     outputs / targets

		if (mNodeAddress == null) 
			throw new IllegalArgumentException("this FST was not built with willPackFST=true");
		
		FSTArc<T> arc = new FSTArc<T>();

		final BytesReader r = getBytesReader(0);
		final int topN = Math.min(maxDerefNodes, mInCounts.size());

		// Find top nodes with highest number of incoming arcs:
		NodeQueue q = new NodeQueue(topN);

		// TODO: we could use more RAM efficient selection algo here...
		NodeAndInCount bottom = null;
		
		for (int node=0; node < mInCounts.size(); node++) {
			if (mInCounts.get(node) >= minInCountDeref) {
				if (bottom == null) {
					q.add(new NodeAndInCount(node, (int) mInCounts.get(node)));
					if (q.size() == topN) 
						bottom = q.top();
				} else if (mInCounts.get(node) > bottom.getCount()) {
					q.insertWithOverflow(
							new NodeAndInCount(node, (int) mInCounts.get(node)));
				}
			}
		}

		// Free up RAM:
		mInCounts = null;

		final Map<Integer,Integer> topNodeMap = new HashMap<Integer,Integer>();
		for (int downTo=q.size()-1; downTo >= 0; downTo--) {
			NodeAndInCount n = q.pop();
			topNodeMap.put(n.getNode(), downTo);
		}

		final FST<T> fst = new FST<T>(mInputType, null, mOutputs);
		final BytesWriter writer = fst.mWriter;

		// +1 because node ords start at 1 (0 is reserved as stop node):
		final IIntsWriter newNodeAddress = new GrowableWriter(
				PackedInts.bitsRequired(mBytes.length), 1 + mNodeCount, acceptableOverheadRatio);

		// Fill initial coarse guess:
		for (int node=1; node <= mNodeCount; node++) {
			newNodeAddress.set(node, 1 + mBytes.length - mNodeAddress.get(node));
		}

		int absCount;
		int deltaCount;
		int topCount;
		int nextCount;

		// Iterate until we converge:
		while (true) {
			boolean changed = false;
			// for assert:
			boolean negDelta = false;

			writer.mPosWrite = 0;
			// Skip 0 byte since 0 is reserved target:
			writer.writeByte((byte) 0);

			fst.mArcWithOutputCount = 0;
			fst.mNodeCount = 0;
			fst.mArcCount = 0;

			absCount = deltaCount = topCount = nextCount = 0;

			int changedCount = 0;
			int addressError = 0;
			//int totWasted = 0;

			// Since we re-reverse the bytes, we now write the
			// nodes backwards, so that BIT_TARGET_NEXT is
			// unchanged:
			for (int node=mNodeCount; node >= 1; node--) {
				fst.mNodeCount ++;
				
				final int address = writer.getPosWrite();
				if (address != newNodeAddress.get(node)) {
					addressError = address - (int) newNodeAddress.get(node);
					changed = true;
					newNodeAddress.set(node, address);
					changedCount++;
				}

				int nodeArcCount = 0;
				int bytesPerArc = 0;
				
				boolean retry = false;
				// for assert:
				boolean anyNegDelta = false;

				// Retry loop: possibly iterate more than once, if
				// this is an array'd node and bytesPerArc changes:
				writeNode:
				while (true) { // retry writing this node
					readFirstRealTargetArc(node, arc, r);

					final boolean useArcArray = arc.getBytesPerArc() != 0;
					if (useArcArray) {
						// Write false first arc:
						if (bytesPerArc == 0) 
							bytesPerArc = arc.getBytesPerArc();
						
						writer.writeByte(ARCS_AS_FIXED_ARRAY);
						writer.writeVInt(arc.getNumArcs());
						writer.writeVInt(bytesPerArc);
					}

					int maxBytesPerArc = 0;
					//int wasted = 0;
					while (true) {  // iterate over all arcs for this node
						final int arcStartPos = writer.getPosWrite();
						nodeArcCount ++;

						byte flags = 0;
						if (arc.isLast()) 
							flags += BIT_LAST_ARC;
						
						/*
            			if (!useArcArray && nodeUpto < nodes.length-1 && arc.target == nodes[nodeUpto+1]) {
              				flags += BIT_TARGET_NEXT;
            			}
						 */
						if (!useArcArray && node != 1 && arc.getTarget() == node-1) {
							flags += BIT_TARGET_NEXT;
							if (!retry) 
								nextCount ++;
						}
						
						if (arc.isFinal()) {
							flags += BIT_FINAL_ARC;
							if (arc.getNextFinalOutput() != NO_OUTPUT) 
								flags += BIT_ARC_HAS_FINAL_OUTPUT;
							
						} else {
							assert arc.getNextFinalOutput() == NO_OUTPUT;
						}
						
						if (!targetHasArcs(arc)) 
							flags += BIT_STOP_NODE;

						if (arc.getOutput() != NO_OUTPUT) 
							flags += BIT_ARC_HAS_OUTPUT;

						final Integer ptr;
						final int absPtr;
						final boolean doWriteTarget = targetHasArcs(arc) && 
								(flags & BIT_TARGET_NEXT) == 0;
						
						if (doWriteTarget) {
							ptr = topNodeMap.get(arc.getTarget());
							if (ptr != null) {
								absPtr = ptr;
							} else {
								absPtr = topNodeMap.size() + 
										(int) newNodeAddress.get(arc.getTarget()) + addressError;
							}

							int delta = (int) newNodeAddress.get(
									arc.getTarget()) + addressError - writer.getPosWrite() - 2;
							if (delta < 0) {
								anyNegDelta = true;
								delta = 0;
							}

							if (delta < absPtr) 
								flags |= BIT_TARGET_DELTA;
							
						} else {
							ptr = null;
							absPtr = 0;
						}

						writer.writeByte(flags);
						fst.writeLabel(arc.getLabel());

						if (arc.getOutput() != NO_OUTPUT) {
							mOutputs.write(arc.getOutput(), writer);
							if (!retry) 
								fst.mArcWithOutputCount ++;
						}
						if (arc.getNextFinalOutput() != NO_OUTPUT) 
							mOutputs.write(arc.getNextFinalOutput(), writer);

						if (doWriteTarget) {
							int delta = (int) newNodeAddress.get(
									arc.getTarget()) + addressError - writer.getPosWrite();
							if (delta < 0) {
								anyNegDelta = true;
								delta = 0;
							}

							if (flag(flags, BIT_TARGET_DELTA)) {
								writer.writeVInt(delta);
								if (!retry) 
									deltaCount ++;
								
							} else {
								writer.writeVInt(absPtr);
								if (!retry) {
									if (absPtr >= topNodeMap.size()) 
										absCount ++;
									else 
										topCount ++;
								}
							}
						}

						if (useArcArray) {
							final int arcBytes = writer.getPosWrite() - arcStartPos;
							
							maxBytesPerArc = Math.max(maxBytesPerArc, arcBytes);
							// NOTE: this may in fact go "backwards", if
							// somehow (rarely, possibly never) we use
							// more bytesPerArc in this rewrite than the
							// incoming FST did... but in this case we
							// will retry (below) so it's OK to ovewrite
							// bytes:
							//wasted += bytesPerArc - arcBytes;
							writer.setPosWrite(arcStartPos + bytesPerArc);
						}

						if (arc.isLast()) 
							break;

						readNextRealArc(arc, r);
					}

					if (useArcArray) {
						if (maxBytesPerArc == bytesPerArc || (retry && maxBytesPerArc <= bytesPerArc)) {
							// converged
							//totWasted += wasted;
							break;
						}
					} else 
						break;

					// Retry:
					bytesPerArc = maxBytesPerArc;
					writer.mPosWrite = address;
					nodeArcCount = 0;
					retry = true;
					anyNegDelta = false;
				}
				
				negDelta |= anyNegDelta;
				fst.mArcCount += nodeArcCount;
			}

			if (!changed) {
				// We don't renumber the nodes (just reverse their
				// order) so nodes should only point forward to
				// other nodes because we only produce acyclic FSTs
				// w/ nodes only pointing "forwards":
				assert !negDelta;
				// Converged!
				break;
			}
		}

		long maxAddress = 0;
		for (int key : topNodeMap.keySet()) {
			maxAddress = Math.max(maxAddress, newNodeAddress.get(key));
		}

		IIntsMutable nodeRefToAddressIn = PackedInts.getMutable(
				topNodeMap.size(), PackedInts.bitsRequired(maxAddress), acceptableOverheadRatio);
		
		for (Map.Entry<Integer,Integer> ent : topNodeMap.entrySet()) {
			nodeRefToAddressIn.set(ent.getValue(), newNodeAddress.get(ent.getKey()));
		}
		
		fst.mNodeRefToAddress = nodeRefToAddressIn;
		fst.mStartNode = (int) newNodeAddress.get(mStartNode);

		if (mEmptyOutput != null) 
			fst.setEmptyOutput(mEmptyOutput);

		assert fst.mNodeCount == mNodeCount: "fst.nodeCount=" + fst.mNodeCount + 
				" nodeCount=" + mNodeCount;
		assert fst.mArcCount == mArcCount;
		assert fst.mArcWithOutputCount == mArcWithOutputCount: "fst.arcWithOutputCount=" + 
				fst.mArcWithOutputCount + " arcWithOutputCount=" + mArcWithOutputCount;
    
		final byte[] finalBytes = new byte[writer.getPosWrite()];
		System.arraycopy(fst.mBytes, 0, finalBytes, 0, writer.getPosWrite());
		fst.mBytes = finalBytes;
		fst.cacheRootArcs();

		//final int size = fst.sizeInBytes();
		return fst;
	}

}
