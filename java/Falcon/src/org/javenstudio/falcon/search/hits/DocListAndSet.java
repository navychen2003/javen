package org.javenstudio.falcon.search.hits;

/**
 * A struct whose only purpose is to hold both a {@link DocList} and a {@link DocSet}
 * so that both may be returned from a single method.
 * <p>
 * The DocList and DocSet returned should <b>not</b> be modified as they may
 * have been retrieved or inserted into a cache and should be considered shared.
 * <p>
 * Oh, if only java had "out" parameters or multiple return args...
 * <p>
 *
 * @since 0.9
 */
public final class DocListAndSet {
	
	private DocList mDocList;
	private DocSet mDocSet;
	
	public DocListAndSet() {}
	
	public void setDocList(DocList list) { mDocList = list; }
	public DocList getDocList() { return mDocList; }
	
	public void setDocSet(DocSet set) { mDocSet = set; }
	public DocSet getDocSet() { return mDocSet; }
	
}
