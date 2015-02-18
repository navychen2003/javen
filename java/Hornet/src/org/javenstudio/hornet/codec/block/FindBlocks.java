package org.javenstudio.hornet.codec.block;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.IntsRef;
import org.javenstudio.hornet.store.fst.FreezeTail;
import org.javenstudio.hornet.store.fst.UnCompiledNode;

// This class assigns terms to blocks "naturally", ie,
// according to the number of terms under a given prefix
// that we encounter:
final class FindBlocks extends FreezeTail<Object> {

	private final BlockTreeTermsWriter mWriter;
	private final TermsWriterImpl mTermsWriter;
	
	public FindBlocks(BlockTreeTermsWriter writer, TermsWriterImpl termsWriter) { 
		mWriter = writer;
		mTermsWriter = termsWriter;
	}
	
    @Override
    public void freeze(final UnCompiledNode<Object>[] frontier, 
    		int prefixLenPlus1, final IntsRef lastInput) throws IOException {

    	for (int idx=lastInput.getLength(); idx >= prefixLenPlus1; idx--) {
    		final UnCompiledNode<Object> node = frontier[idx];
    		long totCount = 0;

    		if (node.isFinal()) 
    			totCount ++;

    		for (int arcIdx=0; arcIdx < node.getNumArcs(); arcIdx++) {
    			@SuppressWarnings("unchecked") 
    			final UnCompiledNode<Object> target = (UnCompiledNode<Object>) 
    				node.getArcAt(arcIdx).getTarget();
    			totCount += target.getInputCount();
    			target.clear();
    			node.getArcAt(arcIdx).setTarget(null);
    		}
    		node.setNumArcs(0);

    		if (totCount >= mWriter.getMinItemsInBlock() || idx == 0) {
    			// We are on a prefix node that has enough
    			// entries (terms or sub-blocks) under it to let
    			// us write a new block or multiple blocks (main
    			// block + follow on floor blocks):
    			mTermsWriter.writeBlocks(lastInput, idx, (int) totCount);
    			node.setInputCount(1);
    		} else {
    			// stragglers!  carry count upwards
    			node.setInputCount(totCount);
    		}
    		frontier[idx] = new UnCompiledNode<Object>(mTermsWriter.getBlockBuilder(), idx);
    	}
	}
	
}
