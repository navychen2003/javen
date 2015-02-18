package org.javenstudio.falcon.datum;

public interface ISectionSet {

	public int getTotalCount();
	public int getSectionStart();
	
	public int getSectionCount();
	public ISection getSectionAt(int index);
	
	public ISectionGroup[] getGroups();
	public ISectionSort[] getSorts();
	
	public String getSortName();
	
}
