package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.provider.media.photo.PhotoSource;
import org.javenstudio.provider.people.BaseGroupInfoProvider;
import org.javenstudio.provider.people.group.GroupBinder;
import org.javenstudio.provider.people.group.IGroupData;

public class FlickrGroupInfoProvider extends BaseGroupInfoProvider {

	private final DataApp mApplication;
	private final FlickrGroup mGroup;
	private final FlickrGroupBinder mBinder;
	
	public FlickrGroupInfoProvider(DataApp app, IGroupData data, 
			String name, int iconRes, FlickrUserClickListener listener,
			FlickrPhotoProvider.FlickrPhotoFactory factory) { 
		super(name, iconRes);
		mApplication = app;
		mGroup = new FlickrGroup(this, (YGroupEntry)data, iconRes, listener, factory);
		mBinder = new FlickrGroupBinder(this);
	}
	
	public DataApp getApplication() { return mApplication; }
	public FlickrGroup getGroupItem() { return mGroup; }
	
	public FlickrPhotoProvider getPhotoProvider() { return getGroupItem().getPhotoProvider(); }
	public PhotoSource getPhotoSource() { return getPhotoProvider().getSource(); }
	
	public FlickrTopicProvider getTopicProvider() { return getGroupItem().getTopicProvider(); }
	
	@Override
	public GroupBinder getBinder() {
		return mBinder;
	}
	
}
