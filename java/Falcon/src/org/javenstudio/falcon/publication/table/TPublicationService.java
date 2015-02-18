package org.javenstudio.falcon.publication.table;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.publication.IPublicationQuery;
import org.javenstudio.falcon.publication.IPublicationService;
import org.javenstudio.falcon.publication.IPublicationSet;
import org.javenstudio.falcon.publication.PublicationHelper;
import org.javenstudio.falcon.publication.PublicationManager;
import org.javenstudio.falcon.user.IMember;

final class TPublicationService implements IPublicationService {
	private static final Logger LOG = Logger.getLogger(TPublicationService.class);
	
	private final Set<String> mChannelSet = new HashSet<String>();
	
	private final Map<String,TNameType> mAttrMap = new HashMap<String,TNameType>();
	private final Map<String,TNameType> mHeaderMap = new HashMap<String,TNameType>();
	private final Map<String,TNameType> mContentMap = new HashMap<String,TNameType>();
	
	private final TNameType[] mAttrNames;
	private final TNameType[] mHeaderNames;
	private final TNameType[] mContentNames;
	
	private final PublicationManager mManager;
	private final TPublicationCache mCache;
	private final String[] mChannelNames;
	private final String mType;
	
	public TPublicationService(PublicationManager manager, 
			String type, String[] channelNames, TNameType[] attrNames, 
			TNameType[] headerNames, TNameType[] contentNames) {
		if (manager == null || type == null || channelNames == null || channelNames.length == 0 ||
			attrNames == null || headerNames == null || contentNames == null) {
			throw new NullPointerException();
		}
		
		mCache = new TPublicationCache(this);
		mManager = manager;
		mChannelNames = channelNames;
		mAttrNames = attrNames;
		mHeaderNames = headerNames;
		mContentNames = contentNames;
		mType = type;
		
		for (String name : channelNames) {
			mChannelSet.add(name);
		}
		for (TNameType nameType : attrNames) {
			mAttrMap.put(nameType.getName(), nameType);
		}
		for (TNameType nameType : headerNames) {
			mHeaderMap.put(nameType.getName(), nameType);
		}
		for (TNameType nameType : contentNames) {
			mContentMap.put(nameType.getName(), nameType);
		}
	}
	
	public PublicationManager getManager() { return mManager; }
	public TPublicationCache getCache() { return mCache; }
	
	public String getType() { return mType; }
	
	protected <T> TNameValue<T> newAttrValue(String name, T val, T oldVal, Class<T> clazz) {
		return newNameValue(mAttrMap, name, val, oldVal, clazz);
	}
	
	protected void checkAttrValue(TNameValue<?> val, TNameValue<?> old) {
		checkNameValue(mAttrMap, val, old);
	}
	
	protected <T> TNameValue<T> newHeaderValue(String name, T val, T oldVal, Class<T> clazz) {
		return newNameValue(mHeaderMap, name, val, oldVal, clazz);
	}
	
	protected void checkHeaderValue(TNameValue<?> val, TNameValue<?> old) {
		checkNameValue(mHeaderMap, val, old);
	}
	
	protected <T> TNameValue<T> newContentValue(String name, T val, T oldVal, Class<T> clazz) {
		return newNameValue(mContentMap, name, val, oldVal, clazz);
	}
	
	protected void checkContentValue(TNameValue<?> val, TNameValue<?> old) {
		checkNameValue(mContentMap, val, old);
	}
	
	private <T> TNameValue<T> newNameValue(Map<String,TNameType> map, 
			String name, T val, T oldVal, Class<T> clazz) {
		if (name == null || val == null) return null;
		synchronized (map) {
			TNameType type = map.get(name);
			if (type == null) 
				throw new IllegalArgumentException("No field: " + name);
			if (clazz != type.getValueClass()) 
				throw new IllegalArgumentException("Wrong value class: " + clazz);
			type.checkChange(this, val, oldVal);
			return new TNameValue<T>(type, val);
		}
	}
	
	private void checkNameValue(Map<String,TNameType> map, 
			TNameValue<?> val, TNameValue<?> old) {
		if (val == null) return;
		synchronized (map) {
			final String name = val.getName();
			TNameType type = map.get(name);
			if (type == null) 
				throw new IllegalArgumentException("No field: " + name);
			if (type != val.getType()) 
				throw new IllegalArgumentException("Wrong value type: " + val.getType());
			if (old != null) {
				Object oldVal = old.getValue();
				type.checkChange(this, val.getValue(), oldVal);
			}
		}
	}
	
	protected TNameType[] getAttrTypes() { return mAttrNames; }
	protected TNameType[] getHeaderTypes() { return mHeaderNames; }
	protected TNameType[] getContentTypes() { return mContentNames; }
	
	public String newPublicationId(String streamKey) throws ErrorException { 
		if (streamKey == null || streamKey.length() == 0)
			streamKey = PublicationHelper.newStreamKey();
		
		return PublicationHelper.newPublicationId(streamKey, 
				PublicationHelper.newPublicationKey(), getType());
	}
	
	@Override
	public String[] getChannelNames() { 
		return mChannelNames;
	}
	
	public boolean hasChannelName(String name) {
		if (name == null || name.length() == 0) return false;
		return mChannelSet.contains(name);
	}

	@Override
	public IPublication.Builder newPublication(IMember owner, 
			String channelName, String streamId) throws ErrorException {
		if (owner == null) throw new NullPointerException();
		if (!hasChannelName(channelName)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Publication channel: " + channelName + " not found");
		}
		
		long current = System.currentTimeMillis();
		
		TPublication publish = new TPublication(this, newPublicationId(streamId));
		publish.setAttr(IPublication.ATTR_PUBLISHID, publish.getId());
		publish.setAttr(IPublication.ATTR_SERVICETYPE, getType());
		publish.setAttr(IPublication.ATTR_OWNER, owner.getUserName());
		publish.setAttr(IPublication.ATTR_CHANNEL, channelName);
		publish.setAttr(IPublication.ATTR_CREATEDTIME, current);
		publish.setAttr(IPublication.ATTR_PUBLISHTIME, current);
		publish.setAttr(IPublication.ATTR_UPDATETIME, current);
		
		if (streamId == null || streamId.length() == 0)
			streamId = PublicationHelper.getStreamKey(publish.getId());
		publish.setAttr(IPublication.ATTR_STREAMID, streamId);
		
		return new TPublicationBuilder(publish);
	}
	
	@Override
	public IPublication.Builder modifyPublication(String publishId) throws ErrorException {
		TPublication found = getCache().getPublication(publishId);
		if (found == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Publication: " + publishId + " not found");
		}
		
		TPublication publish = new TPublication(this, publishId);
		publish.setAttr(IPublication.ATTR_UPDATETIME, System.currentTimeMillis());
		
		return new TPublicationBuilder(publish);
	}
	
	@Override
	public IPublication getPublication(String publishId) throws ErrorException {
		return getCache().getPublication(publishId);
	}
	
	@Override
	public IPublication movePublication(String publishId, String channelTo) 
			throws ErrorException {
		if (publishId == null || channelTo == null)
			return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("movePublication: publishId=" + publishId 
					+ " channelTo=" + channelTo);
		}
		
		if (!hasChannelName(channelTo)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Publication channel: " + channelTo + " not found");
		}
		
		return getCache().movePublication(publishId, channelTo);
	}

	@Override
	public IPublication deletePublication(String publishId) throws ErrorException {
		if (publishId == null) return null;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("deletePublication: publishId=" + publishId);
		
		return getCache().deletePublication(publishId);
	}
	
	@Override
	public IPublicationSet getPublications(IPublicationQuery query) 
			throws ErrorException {
		return getCache().getPublications(query);
	}
	
	@Override
	public IPublicationSet getPublications(String streamId, String channel) 
			throws ErrorException {
		return getCache().getPublications(streamId, channel);
	}
	
	@Override
	public void flushPublications() throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("flushPublications");
		getCache().flushPublications();
	}
	
	@Override
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close: type=" + mType);
		try {
			flushPublications();
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("close: flush error: " + e, e);
		}
		mCache.close();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{type=" + mType + "}";
	}
	
}
