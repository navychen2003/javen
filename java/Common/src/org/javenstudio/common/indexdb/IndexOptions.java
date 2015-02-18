package org.javenstudio.common.indexdb;

/**
 * Controls how much information is stored in the postings lists.
 * 
 */
public enum IndexOptions { 
    // NOTE: order is important here; FieldInfo uses this
    // order to merge two conflicting IndexOptions (always
    // "downgrades" by picking the lowest).
	
    /** only documents are indexed: term frequencies and positions are omitted */
    // TODO: maybe rename to just DOCS?
    DOCS_ONLY,
    
    /** only documents and term frequencies are indexed: positions are omitted */  
    DOCS_AND_FREQS,
    
    /** documents, frequencies and positions */
    DOCS_AND_FREQS_AND_POSITIONS,
    
    /** documents, frequencies, positions and offsets */
    DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
    
}
