package org.javenstudio.falcon.datum;

public interface ISectionCollector {

	public void addSection(ISection section);
	public void addModified(ISection section);
	public void addDeleted(String sectionId);
	
}
