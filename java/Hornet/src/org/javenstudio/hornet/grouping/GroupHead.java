package org.javenstudio.hornet.grouping;

import java.io.IOException;

/**
 * Represents a group head. A group head is the most relevant document for a particular group.
 * The relevancy is based is usually based on the sort.
 *
 * The group head contains a group value with its associated most relevant document id.
 */
public abstract class GroupHead<GT> {

    protected final GT mGroupValue;
    protected int mDoc;

    protected GroupHead(GT groupValue, int doc) {
    	mGroupValue = groupValue;
    	mDoc = doc;
    }

    public GT getGroupValue() { return mGroupValue; }
    public int getDoc() { return mDoc; }
    
    /**
     * Compares the specified document for a specified comparator against 
     * the current most relevant document.
     *
     * @param compIDX The comparator index of the specified comparator.
     * @param doc The specified document.
     * @return -1 if the specified document wasn't competitive against 
     * 		   the current most relevant document, 1 if the
     *         specified document was competitive against the current most 
     *         relevant document. Otherwise 0.
     * @throws IOException If I/O related errors occur
     */
    protected abstract int compare(int compIDX, int doc) throws IOException;

    /**
     * Updates the current most relevant document with the specified document.
     *
     * @param doc The specified document
     * @throws IOException If I/O related errors occur
     */
    protected abstract void updateDocHead(int doc) throws IOException;
	
}
