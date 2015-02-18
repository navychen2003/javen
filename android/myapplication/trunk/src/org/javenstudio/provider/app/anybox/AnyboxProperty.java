package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.android.ActionError;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.library.INameValue;
import org.javenstudio.util.StringUtils;

public class AnyboxProperty {
	private static final Logger LOG = Logger.getLogger(AnyboxProperty.class);

	public static class TagItem implements INameValue {
		private final String mName;
		private final String mValue;
		
		public TagItem(String name, String value) {
			mName = name;
			mValue = value;
		}
		
		public String getName() { return mName; }
		public String getValue() { return mValue; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() 
					+ "{name=" + mName + ",value=" + mValue + "}";
		}
	}
	
	private final AnyboxAccount mUser;
	//private final AnyboxData mData;
	//private final ActionError mError;
	private final long mRequestTime;
	private final String mSectionId;
	
	private TagItem[] mMediaInfos = null;
	private TagItem[] mMediaTags = null;
	
	private AnyboxProperty(AnyboxAccount user, 
			AnyboxData data, ActionError error, String sectionId, 
			long requestTime) {
		if (user == null) throw new NullPointerException();
		mUser = user;
		//mData = data;
		//mError = error;
		mRequestTime = requestTime;
		mSectionId = sectionId;
	}
	
	public AnyboxAccount getUser() { return mUser; }
	//public AnyboxData getData() { return mData; }
	//public ActionError getError() { return mError; }
	public long getRequestTime() { return mRequestTime; }
	public String getSectionId() { return mSectionId; }
	
	public TagItem[] getMediaInfos() { return mMediaInfos; }
	public TagItem[] getMediaTags() { return mMediaTags; }
	
	public void getExifs(IMediaDetails details) {
		if (details == null) return;
		
		TagItem[] tags = getMediaInfos();
		if (tags != null) {
			for (TagItem tag : tags) {
				if (tag == null) continue;
				details.add(tag.getName(), tag.getValue());
			}
		}
	}
	
	public void loadData(AnyboxData data) throws IOException {
		if (data == null) return;
		loadDetailsData(data.get("details"));
	}
	
	private void loadDetailsData(AnyboxData data) throws IOException {
		if (data == null) return;
		loadDetailsMedia(data.get("media"));
	}
	
	private void loadDetailsMedia(AnyboxData data) throws IOException {
		if (data == null) return;
		
		String[] names = data.getNames();
		ArrayList<TagItem> infos = new ArrayList<TagItem>();
		ArrayList<TagItem> tags = new ArrayList<TagItem>();
		
		for (int i=0; names != null && i < names.length; i++) {
			String name = names[i];
			String value = data.getString(name);
			if (name != null && name.length() > 0 && value != null) {
				TagItem item = new TagItem(name, value);
				if (AnyboxMetadata.hasTagName(name)) infos.add(item);
				else tags.add(item);
				
				if (LOG.isDebugEnabled())
					LOG.debug("loadDetailsMedia: item=" + item);
			}
		}
		
		mMediaInfos = infos.toArray(new TagItem[infos.size()]);
		mMediaTags = tags.toArray(new TagItem[tags.size()]);
	}
	
	public static AnyboxProperty getProperty(AnyboxAccount user, 
			AnyboxHelper.IRequestWrapper wrapper, ProviderCallback callback, 
			String sectionId) {
		if (user == null || sectionId == null || sectionId.length() == 0) 
			return null;
		
		if (wrapper == null) wrapper = user;
		
		String url = wrapper.getApp().getRequestAddr(wrapper.getHostData(), false) 
				+ "/datum/sectioninfo?wt=secretjson&action=property" 
				+ "&id=" + StringUtils.URLEncode(sectionId) 
				+ "&token=" + StringUtils.URLEncode(wrapper.getAuthToken());
		
		PropertyListener listener = new PropertyListener(user, sectionId);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		AnyboxProperty result = null;
		try {
			AnyboxProperty property = listener.mProperty;
			if (property != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					property.loadData(listener.mData);
					user.getApp().putSectionProperty(property);
					result = property;
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isErrorEnabled())
				LOG.error("getDashboard: error: " + e, e);
		}
		
		if (callback != null && error != null) 
			callback.onActionError(error);
		
		return result;
	}
	
	static class PropertyListener extends AnyboxApi.SecretJSONListener {
		private final AnyboxAccount mUser;
		private final String mSectionId;
		private AnyboxProperty mProperty = null;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public PropertyListener(AnyboxAccount user, String sectionId) {
			if (user == null) throw new NullPointerException();
			mUser = user;
			mSectionId = sectionId;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			mProperty = new AnyboxProperty(mUser, data, error, 
					mSectionId, System.currentTimeMillis());
			if (LOG.isDebugEnabled())
				LOG.debug("handleData: data=" + data);
			
			if (error != null) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("handleData: response error: " + error, 
							error.getException());
				}
			}
		}

		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.SECTION_PROPERTY;
		}
	}
	
}
