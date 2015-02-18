package org.javenstudio.hornet.store.fst;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.IntsRef;

/** 
 * Static helper methods.
 */
public final class FSTUtil {
	private FSTUtil() {}

	/** 
	 * Looks up the output for this input, or null if the
	 *  input is not accepted. 
	 */
	public static<T> T get(FST<T> fst, IntsRef input) throws IOException {
		// TODO: would be nice not to alloc this on every lookup
		final FSTArc<T> arc = fst.getFirstArc(new FSTArc<T>());
		final BytesReader fstReader = fst.getBytesReader(0);

		// Accumulate output as we go
		T output = fst.getOutputs().getNoOutput();
		for (int i=0; i < input.getLength(); i++) {
			if (fst.findTargetArc(input.getIntAt(input.getOffset() + i), 
					arc, arc, fstReader) == null) {
				return null;
			}
			output = fst.getOutputs().add(output, arc.getOutput());
		}

		if (arc.isFinal()) 
			return fst.getOutputs().add(output, arc.getNextFinalOutput());
		
		return null;
	}

	// TODO: maybe a CharsRef version for BYTE2

	/** 
	 * Looks up the output for this input, or null if the
	 *  input is not accepted 
	 */
	public static<T> T get(FST<T> fst, BytesRef input) throws IOException {
		assert fst.getInputType() == FST.INPUT_TYPE.BYTE1;
		final BytesReader fstReader = fst.getBytesReader(0);

		// TODO: would be nice not to alloc this on every lookup
		final FSTArc<T> arc = fst.getFirstArc(new FSTArc<T>());

		// Accumulate output as we go
		T output = fst.getOutputs().getNoOutput();
		for (int i=0; i < input.getLength(); i++) {
			if (fst.findTargetArc(input.getByteAt(i+input.getOffset()) & 0xFF, 
					arc, arc, fstReader) == null) {
				return null;
			}
			output = fst.getOutputs().add(output, arc.getOutput());
		}

		if (arc.isFinal()) 
			return fst.getOutputs().add(output, arc.getNextFinalOutput());

		return null;
	}

	/** 
	 * Reverse lookup (lookup by output instead of by input),
	 *  in the special case when your FSTs outputs are
	 *  strictly ascending.  This locates the input/output
	 *  pair where the output is equal to the target, and will
	 *  return null if that output does not exist.
	 *
	 *  <p>NOTE: this only works with FST<Long>, only
	 *  works when the outputs are ascending in order with
	 *  the inputs and only works when you shared
	 *  the outputs (pass doShare=true to {@link
	 *  PositiveIntOutputs#getSingleton}).
	 *  For example, simple ordinals (0, 1,
	 *  2, ...), or file offets (when appending to a file)
	 *  fit this. 
	 */
	public static IntsRef getByOutput(FST<Long> fst, long targetOutput) throws IOException {
		final BytesReader in = fst.getBytesReader(0);

		// TODO: would be nice not to alloc this on every lookup
		FSTArc<Long> arc = fst.getFirstArc(new FSTArc<Long>());
		FSTArc<Long> scratchArc = new FSTArc<Long>();

		final IntsRef result = new IntsRef();

		long output = arc.getOutput();
		int upto = 0;

		while (true) {
			if (arc.isFinal()) {
				final long finalOutput = output + arc.getNextFinalOutput();
				if (finalOutput == targetOutput) {
					result.setLength(upto);
					return result;
				} else if (finalOutput > targetOutput) {
					return null;
				}
			}

			if (FST.targetHasArcs(arc)) {
				if (result.getInts().length == upto) 
					result.grow(1+upto);
        
				fst.readFirstRealTargetArc(arc.getTarget(), arc, in);

				if (arc.getBytesPerArc() != 0) {
					int low = 0;
					int high = arc.getNumArcs() - 1;
					int mid = 0;
					boolean exact = false;
					
					while (low <= high) {
						mid = (low + high) >>> 1;
            			in.mPos = arc.getPosArcsStart();
            			in.skip(arc.getBytesPerArc()*mid);
            			
            			final byte flags = in.readByte();
            			fst.readLabel(in);
            			final long minArcOutput;
            			
            			if ((flags & FST.BIT_ARC_HAS_OUTPUT) != 0) {
            				final long arcOutput = fst.getOutputs().read(in);
            				minArcOutput = output + arcOutput;
            			} else {
            				minArcOutput = output;
            			}
            
            			if (minArcOutput == targetOutput) {
            				exact = true;
            				break;
            			} else if (minArcOutput < targetOutput) {
            				low = mid + 1;
            			} else {
            				high = mid - 1;
            			}
					}

					if (high == -1) 
						return null;
					else if (exact) 
						arc.setArcIndex(mid-1);
					else 
						arc.setArcIndex(low-2);
					

					fst.readNextRealArc(arc, in);
					result.setIntAt(upto++, arc.getLabel());
					output += arc.getOutput();

				} else {
					FSTArc<Long> prevArc = null;

					while (true) {
						// This is the min output we'd hit if we follow
						// this arc:
						final long minArcOutput = output + arc.getOutput();

						if (minArcOutput == targetOutput) {
							// Recurse on this arc:
							output = minArcOutput;
							result.setIntAt(upto++, arc.getLabel());
							break;
							
						} else if (minArcOutput > targetOutput) {
							if (prevArc == null) {
								// Output doesn't exist
								return null;
								
							} else {
								// Recurse on previous arc:
								arc.copyFrom(prevArc);
								result.setIntAt(upto++, arc.getLabel());
								output += arc.getOutput();
								break;
							}
						} else if (arc.isLast()) {
							// Recurse on this arc:
							output = minArcOutput;
							result.setIntAt(upto++, arc.getLabel());
							break;
							
						} else {
							// Read next arc in this node:
							prevArc = scratchArc;
							prevArc.copyFrom(arc);
							fst.readNextRealArc(arc, in);
						}
					}
				}
			} else {
				return null;
			}
		}    
	}

	/** 
	 * Starting from node, find the top N min cost 
	 * completions to a final node.
	 *
	 *  <p>NOTE: you must share the outputs when you build the
	 *  FST (pass doShare=true to {@link
	 *  PositiveIntOutputs#getSingleton}). 
	 */
	public static <T> MinResult<T>[] shortestPaths(FST<T> fst, FSTArc<T> fromNode, 
			Comparator<T> comparator, int topN) throws IOException {
		return new TopNSearcher<T>(fst, fromNode, topN, comparator).search();
	} 

	/**
	 * Dumps an {@link FST} to a GraphViz's <code>dot</code> language description
	 * for visualization. Example of use:
	 * 
	 * <pre class="prettyprint">
	 * PrintWriter pw = new PrintWriter(&quot;out.dot&quot;);
	 * Util.toDot(fst, pw, true, true);
	 * pw.close();
	 * </pre>
	 * 
	 * and then, from command line:
	 * 
	 * <pre>
	 * dot -Tpng -o out.png out.dot
	 * </pre>
	 * 
	 * <p>
	 * Note: larger FSTs (a few thousand nodes) won't even render, don't bother.
	 * 
	 * @param sameRank
	 *          If <code>true</code>, the resulting <code>dot</code> file will try
	 *          to order states in layers of breadth-first traversal. This may
	 *          mess up arcs, but makes the output FST's structure a bit clearer.
	 * 
	 * @param labelStates
	 *          If <code>true</code> states will have labels equal to their offsets in their
	 *          binary format. Expands the graph considerably. 
	 * 
	 * @see "http://www.graphviz.org/"
	 */
	public static <T> void toDot(FST<T> fst, Writer out, boolean sameRank, 
			boolean labelStates) throws IOException {    
		final String expandedNodeColor = "blue";

		// This is the start arc in the automaton (from the epsilon state to the first state 
		// with outgoing transitions.
		final FSTArc<T> startArc = fst.getFirstArc(new FSTArc<T>());

		// A queue of transitions to consider for the next level.
		final List<FSTArc<T>> thisLevelQueue = new ArrayList<FSTArc<T>>();

		// A queue of transitions to consider when processing the next level.
		final List<FSTArc<T>> nextLevelQueue = new ArrayList<FSTArc<T>>();
		nextLevelQueue.add(startArc);
    
		// A list of states on the same level (for ranking).
		final List<Integer> sameLevelStates = new ArrayList<Integer>();

		// A bitset of already seen states (target offset).
		final BitSet seen = new BitSet();
		seen.set(startArc.getTarget());

		// Shape for states.
		final String stateShape = "circle";
		final String finalStateShape = "doublecircle";

		// Emit DOT prologue.
		out.write("digraph FST {\n");
		out.write("  rankdir = LR; splines=true; concentrate=true; ordering=out; ranksep=2.5; \n");

		if (!labelStates) {
			out.write("  node [shape=circle, width=.2, height=.2, style=filled]\n");      
		}

		emitDotState(out, "initial", "point", "white", "");

		final T NO_OUTPUT = fst.getOutputs().getNoOutput();
		final BytesReader r = fst.getBytesReader(0);

		// final FST.Arc<T> scratchArc = new FST.Arc<T>();

		{
			final String stateColor;
			if (fst.isExpandedTarget(startArc, r)) 
				stateColor = expandedNodeColor;
			else 
				stateColor = null;
			
			final boolean isFinal;
			final T finalOutput;
			if (startArc.isFinal()) {
				isFinal = true;
				finalOutput = startArc.getNextFinalOutput() == NO_OUTPUT ? null : 
						startArc.getNextFinalOutput();
			} else {
				isFinal = false;
				finalOutput = null;
			}
      
			emitDotState(out, Integer.toString(startArc.getTarget()), 
					isFinal ? finalStateShape : stateShape, stateColor, 
					finalOutput == null ? "" : fst.getOutputs().outputToString(finalOutput));
		}

		out.write("  initial -> " + startArc.getTarget() + "\n");

		int level = 0;

		while (!nextLevelQueue.isEmpty()) {
			// we could double buffer here, but it doesn't matter probably.
			thisLevelQueue.addAll(nextLevelQueue);
			nextLevelQueue.clear();

			level ++;
			out.write("\n  // Transitions and states at level: " + level + "\n");
			
			while (!thisLevelQueue.isEmpty()) {
				final FSTArc<T> arc = thisLevelQueue.remove(thisLevelQueue.size() - 1);
				if (FST.targetHasArcs(arc)) {
					// scan all target arcs
					final int node = arc.getTarget();
					fst.readFirstRealTargetArc(arc.getTarget(), arc, r);

					while (true) {
						// Emit the unseen state and add it to the queue for the next level.
						if (arc.getTarget() >= 0 && !seen.get(arc.getTarget())) {
							/*
              				boolean isFinal = false;
              				T finalOutput = null;
              				fst.readFirstTargetArc(arc, scratchArc);
              				if (scratchArc.isFinal() && fst.targetHasArcs(scratchArc)) {
                				// target is final
                				isFinal = true;
                				finalOutput = scratchArc.output == NO_OUTPUT ? null : scratchArc.output;
              				}
							 */
							
							final String stateColor;
							if (fst.isExpandedTarget(arc, r)) 
								stateColor = expandedNodeColor;
							else 
								stateColor = null;
							
							final String finalOutput;
							if (arc.getNextFinalOutput() != null && arc.getNextFinalOutput() != NO_OUTPUT) 
								finalOutput = fst.getOutputs().outputToString(arc.getNextFinalOutput());
							else 
								finalOutput = "";

							emitDotState(out, Integer.toString(arc.getTarget()), 
									stateShape, stateColor, finalOutput);
							
							// To see the node address, use this instead:
							//emitDotState(out, Integer.toString(arc.target), 
							//		stateShape, stateColor, String.valueOf(arc.target));
							
							seen.set(arc.getTarget());
							nextLevelQueue.add(new FSTArc<T>().copyFrom(arc));
							sameLevelStates.add(arc.getTarget());
						}

						String outs;
						if (arc.getOutput() != NO_OUTPUT) 
							outs = "/" + fst.getOutputs().outputToString(arc.getOutput());
						else 
							outs = "";

						if (!FST.targetHasArcs(arc) && arc.isFinal() && arc.getNextFinalOutput() != NO_OUTPUT) {
							// Tricky special case: sometimes, due to
							// pruning, the builder can [sillily] produce
							// an FST with an arc into the final end state
							// (-1) but also with a next final output; in
							// this case we pull that output up onto this
							// arc
							outs = outs + "/[" + fst.getOutputs().outputToString(arc.getNextFinalOutput()) + "]";
						}

						final String arcColor;
						if (arc.flag(FST.BIT_TARGET_NEXT)) 
							arcColor = "red";
						else 
							arcColor = "black";

						assert arc.getLabel() != FST.END_LABEL;
						out.write("  " + node + " -> " + arc.getTarget() + " [label=\"" + 
								printableLabel(arc.getLabel()) + outs + "\"" + (arc.isFinal() ? 
									" style=\"bold\"" : "" ) + " color=\"" + arcColor + "\"]\n");
                   
						// Break the loop if we're on the last arc of this state.
						if (arc.isLast()) 
							break;
						
						fst.readNextRealArc(arc, r);
					}
				}
			}

			// Emit state ranking information.
			if (sameRank && sameLevelStates.size() > 1) {
				out.write("  {rank=same; ");
				for (int state : sameLevelStates) {
					out.write(state + "; ");
				}
				out.write(" }\n");
			}
			sameLevelStates.clear();                
		}

		// Emit terminating state (always there anyway).
		out.write("  -1 [style=filled, color=black, shape=doublecircle, label=\"\"]\n\n");
		out.write("  {rank=sink; -1 }\n");
    
		out.write("}\n");
		out.flush();
	}

	/**
	 * Emit a single state in the <code>dot</code> language. 
	 */
	private static void emitDotState(Writer out, String name, String shape,
			String color, String label) throws IOException {
		out.write("  " + name 
				+ " [" 
				+ (shape != null ? "shape=" + shape : "") + " "
				+ (color != null ? "color=" + color : "") + " "
				+ (label != null ? "label=\"" + label + "\"" : "label=\"\"") + " "
				+ "]\n");
	}

	/**
	 * Ensures an arc's label is indeed printable (dot uses US-ASCII). 
	 */
	private static String printableLabel(int label) {
		if (label >= 0x20 && label <= 0x7d) 
			return Character.toString((char) label);
		else 
			return "0x" + Integer.toHexString(label);
		
	}

	/** 
	 * Decodes the Unicode codepoints from the provided
	 *  CharSequence and places them in the provided scratch
	 *  IntsRef, which must not be null, returning it. 
	 */
	public static IntsRef toUTF32(CharSequence s, IntsRef scratch) {
		int charIdx = 0;
		int intIdx = 0;
		final int charLimit = s.length();
		
		while (charIdx < charLimit) {
			scratch.grow(intIdx+1);
			final int utf32 = Character.codePointAt(s, charIdx);
			scratch.mInts[intIdx] = utf32;
			charIdx += Character.charCount(utf32);
			intIdx ++;
		}
		
		scratch.mLength = intIdx;
		return scratch;
	}

	/** 
	 * Decodes the Unicode codepoints from the provided
	 *  char[] and places them in the provided scratch
	 *  IntsRef, which must not be null, returning it. 
	 */
	public static IntsRef toUTF32(char[] s, int offset, int length, IntsRef scratch) {
		int charIdx = offset;
		int intIdx = 0;
		final int charLimit = offset + length;
		
		while (charIdx < charLimit) {
			scratch.grow(intIdx+1);
			final int utf32 = Character.codePointAt(s, charIdx);
			scratch.mInts[intIdx] = utf32;
			charIdx += Character.charCount(utf32);
			intIdx ++;
		}
		
		scratch.mLength = intIdx;
		return scratch;
	}

	/** 
	 * Just takes unsigned byte values from the BytesRef and
	 *  converts into an IntsRef. 
	 */
	public static IntsRef toIntsRef(BytesRef input, IntsRef scratch) {
		scratch.grow(input.getLength());
		for (int i=0; i < input.getLength(); i++) {
			scratch.mInts[i] = input.getByteAt(i+input.getOffset()) & 0xFF;
		}
		scratch.mLength = input.getLength();
		return scratch;
	}

	/** 
	 * Just converts IntsRef to BytesRef; you must ensure the
	 *  int values fit into a byte. 
	 */
	public static BytesRef toBytesRef(IntsRef input, BytesRef scratch) {
		scratch.grow(input.getLength());
		for (int i=0; i < input.getLength(); i++) {
			scratch.mBytes[i] = (byte) input.mInts[i+input.mOffset];
		}
		scratch.mLength = input.mLength;
		return scratch;
	}
	
}
