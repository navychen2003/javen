package org.javenstudio.falcon.publication;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IMember;

public interface IPublicationService {

	public PublicationManager getManager();
	
	public String getType();
	public String[] getChannelNames();
	
	public IPublication.Builder newPublication(IMember owner, String channelName, String streamId) throws ErrorException;
	public IPublication.Builder modifyPublication(String publicationId) throws ErrorException;
	
	public IPublicationSet getPublications(IPublicationQuery query) throws ErrorException;
	public IPublication getPublication(String publishId) throws ErrorException;
	
	public IPublication movePublication(String publishId, String channelTo) throws ErrorException;
	public IPublication deletePublication(String publishId) throws ErrorException;
	
	public IPublicationSet getPublications(String streamId, String channel) throws ErrorException;
	
	public void flushPublications() throws ErrorException;
	public void close();
	
}
