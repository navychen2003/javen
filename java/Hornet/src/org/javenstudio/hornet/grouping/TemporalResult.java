package org.javenstudio.hornet.grouping;

/**
 * Contains the result of group head retrieval.
 * To prevent new object creations of this class for every collect.
 */
public class TemporalResult<GH extends GroupHead<?>> {

	private GH mGroupHead = null;
	private boolean mStop = false;
	
	public void setGroupHead(GH head) { mGroupHead = head; }
	public GH getGroupHead() { return mGroupHead; }
	
	public boolean isStop() { return mStop; }
	public void setStop(boolean stop) { mStop = stop; }
	
}
