package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.util.PriorityQueue;

public final class PhraseQueue extends PriorityQueue<PhrasePositions> {
	
	public PhraseQueue(int size) {
		super(size);
	}

	@Override
	protected final boolean lessThan(PhrasePositions pp1, PhrasePositions pp2) {
		if (pp1.getDoc() == pp2.getDoc()) { 
			if (pp1.getPosition() == pp2.getPosition()) {
				// same doc and pp.position, so decide by actual term positions. 
				// rely on: pp.position == tp.position - offset. 
				if (pp1.getOffset() == pp2.getOffset()) 
					return pp1.getOrd() < pp2.getOrd();
				else 
					return pp1.getOffset() < pp2.getOffset();
				
			} else {
				return pp1.getPosition() < pp2.getPosition();
			}
			
		} else {
			return pp1.getDoc() < pp2.getDoc();
		}
	}
	
}
