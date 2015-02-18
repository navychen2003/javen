package org.javenstudio.falcon.publication;

public interface IPublicationSet {

	public int getTotalCount();
	public int getStart();
	
	public IPublication[] getPublications();
	
	public void first();
	public IPublication next();
	
}
