package org.javenstudio.falcon.util;

import java.util.Iterator;

public interface ContextList extends Iterator<ContextNode> {

	public int getLength();
	public ContextNode getNodeAt(int index);
	
}
