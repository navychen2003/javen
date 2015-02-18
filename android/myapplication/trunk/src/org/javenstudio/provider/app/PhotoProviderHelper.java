package org.javenstudio.provider.app;

import android.content.res.Resources;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.flickr.FlickrAlbumPhotoSet;
import org.javenstudio.provider.app.flickr.FlickrAlbumProvider;
import org.javenstudio.provider.app.flickr.FlickrAlbumSet;
import org.javenstudio.provider.app.flickr.FlickrGroupInfoProvider;
import org.javenstudio.provider.app.flickr.FlickrPhotoProvider;
import org.javenstudio.provider.app.flickr.FlickrTopicReplyProvider;
import org.javenstudio.provider.app.flickr.FlickrUserClickListener;
import org.javenstudio.provider.app.flickr.FlickrUserInfoProvider;
import org.javenstudio.provider.app.flickr.FlickrAlbumProvider.PicasaAlbumFactory;
import org.javenstudio.provider.app.flickr.FlickrPhotoProvider.FlickrPhotoFactory;
import org.javenstudio.provider.app.local.LocalPhotoProvider;
import org.javenstudio.provider.app.picasa.PicasaAlbum;
import org.javenstudio.provider.app.picasa.PicasaAlbumPhotoSet;
import org.javenstudio.provider.app.picasa.PicasaAlbumProvider;
import org.javenstudio.provider.app.picasa.PicasaAlbumSet;
import org.javenstudio.provider.app.picasa.PicasaPhotoProvider;
import org.javenstudio.provider.app.picasa.PicasaUserClickListener;
import org.javenstudio.provider.app.picasa.PicasaUserInfoProvider;
import org.javenstudio.provider.media.album.AlbumItem;
import org.javenstudio.provider.media.photo.PhotoItem;
import org.javenstudio.provider.people.group.GroupItem;
import org.javenstudio.provider.publish.discuss.TopicItem;

public class PhotoProviderHelper {
	private static final Logger LOG = Logger.getLogger(PhotoProviderHelper.class);

	private static boolean isEmpty(String str) { 
		return str == null || str.length() == 0;
	}
	
	public static LocalPhotoProvider getLocalAlbum(DataApp app, 
			AlbumItem item, LocalPhotoProvider.LocalPhotoFactory factory) { 
		if (item == null) return null;
		
		PhotoSet photoSet = item.getAlbum().getPhotoSet();
		if (photoSet != null) {
			LocalPhotoProvider lp = new LocalPhotoProvider(
					photoSet.getName(), 0, photoSet, factory);
			
			String title = item.getAlbum().getAlbumInfo().getTitle();
			if (title == null) title = "";
			lp.setTitle(ResourceHelper.getResources().getString(R.string.label_title_album) + title);
			
			return lp;
		}
		
		return null;
	}
	
	public static PicasaPhotoProvider getPicasaAlbum(DataApp app, 
			AlbumItem item, int iconRes, PicasaUserClickListener listener, 
			PicasaPhotoProvider.PicasaPhotoFactory factory) { 
		if (item == null) return null;
		
		final Resources res = app.getContext().getResources();
		
		String userId = item.getAlbum().getAlbumInfo().getUserId();
		String albumId = item.getAlbum().getAlbumInfo().getAlbumId();
		String author = item.getAlbum().getAlbumInfo().getAuthor();
		String avatarURL = item.getAlbum().getAlbumInfo().getAvatarLocation();
		String title = item.getAlbum().getAlbumInfo().getTitle();
		
		if (!isEmpty(userId) && !isEmpty(albumId)) { 
			SystemUser account = null;
			PhotoSet albumSet = item.getAlbum().getPhotoSet();
			if (albumSet instanceof PicasaAlbum) 
				account = ((PicasaAlbum)albumSet).getSource().getAccount();
			
			PicasaAlbumPhotoSet photoSet = PicasaAlbumPhotoSet.newPhotoSet(
					app, account, userId, albumId, author, avatarURL, iconRes);
			photoSet.setUserClickListener(listener);
			
			PicasaPhotoProvider p = new PicasaPhotoProvider(
					title, iconRes, photoSet, factory);
			
			if (title == null) title = "";
			p.setTitle(res.getString(R.string.label_title_album) + title);
			
			return p;
		} else { 
			if (LOG.isDebugEnabled()) { 
				LOG.debug("getPicasaAlbum: item=" + item 
						+ " userId=" + userId + " albumId=" + albumId);
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("unused")
	private static PicasaAlbumProvider getPicasaAlbums(AccountApp app, 
			PhotoItem item, int iconRes, PicasaAlbumProvider.PicasaAlbumFactory factory) { 
		if (item == null) return null;
		
		String userId = item.getPhoto().getPhotoInfo().getUserId();
		String author = item.getPhoto().getPhotoInfo().getAuthor();
		String avatarURL = item.getPhoto().getPhotoInfo().getAvatarLocation();
		
		if (!isEmpty(userId)) { 
			PicasaAlbumProvider p = new PicasaAlbumProvider(author, iconRes, 
					PicasaAlbumSet.newAlbumSet(app, userId, author, avatarURL, iconRes), factory);
			p.setTitle(author);
			return p;
		}
		
		return null;
	}
	
	public static PicasaUserInfoProvider getPicasaUser(AccountApp app, 
			PhotoItem item, int iconRes, PicasaUserClickListener listener, 
			PicasaAlbumProvider.PicasaAlbumFactory factory) { 
		if (item == null) return null;
		
		String userId = item.getPhoto().getPhotoInfo().getUserId();
		String author = item.getPhoto().getPhotoInfo().getAuthor();
		String avatarURL = item.getPhoto().getPhotoInfo().getAvatarLocation();
		
		PicasaUserInfoProvider p = getPicasaUser(app, 
				userId, author, avatarURL, iconRes, listener, factory);
		if (p != null) 
			p.setPhotoItem(item);
		
		return p;
	}
	
	public static PicasaUserInfoProvider getPicasaUser(AccountApp app, 
			String userId, String userName, String avatarURL, int iconRes, 
			PicasaUserClickListener listener, PicasaAlbumProvider.PicasaAlbumFactory factory) { 
		if (!isEmpty(userId)) { 
			PicasaUserInfoProvider p = new PicasaUserInfoProvider(app, 
					userId, userName, avatarURL, iconRes, listener, factory);
			p.setTitle(userName);
			return p;
		}
		
		return null;
	}
	
	public static FlickrPhotoProvider getFlickrAlbum(DataApp app, 
			AlbumItem item, int iconRes, FlickrUserClickListener listener, 
			FlickrPhotoFactory factory) { 
		if (item == null) return null;
		
		final Resources res = app.getContext().getResources();
		
		String albumId = item.getAlbum().getAlbumInfo().getAlbumId();
		String title = item.getAlbum().getAlbumInfo().getTitle();
		
		if (!isEmpty(albumId)) { 
			FlickrAlbumPhotoSet photoSet= FlickrAlbumPhotoSet.newPhotoSet(
					app, albumId, iconRes);
			photoSet.setUserClickListener(listener);
			
			FlickrPhotoProvider p = new FlickrPhotoProvider(
					title, iconRes, photoSet, factory);
			
			if (title == null) title = "";
			p.setTitle(res.getString(R.string.label_title_album) + title);
			
			return p;
		}
		
		return null;
	}
	
	@SuppressWarnings("unused")
	private static FlickrAlbumProvider getFlickrAlbums(DataApp app, 
			PhotoItem item, int iconRes, PicasaAlbumFactory factory) { 
		if (item == null) return null;
		
		String userId = item.getPhoto().getPhotoInfo().getUserId();
		
		if (!isEmpty(userId)) { 
			FlickrAlbumProvider p = new FlickrAlbumProvider(userId, iconRes, 
					FlickrAlbumSet.newAlbumSet(app, userId, iconRes), factory);
			p.setTitle(userId);
			return p;
		}
		
		return null;
	}
	
	public static FlickrUserInfoProvider getFlickrUser(DataApp app, 
			PhotoItem item, int iconRes, FlickrUserClickListener listener, 
			FlickrAlbumProvider.PicasaAlbumFactory albumFactory, 
			FlickrPhotoProvider.FlickrPhotoFactory photoFactory) { 
		if (item == null) return null;
		
		String userId = item.getPhoto().getPhotoInfo().getUserId();
		String author = item.getPhoto().getPhotoInfo().getAuthor();
		
		FlickrUserInfoProvider p = getFlickrUser(app, 
				userId, author, iconRes, listener, albumFactory, photoFactory);
		if (p != null) 
			p.setPhotoItem(item);
		
		return p;
	}
	
	public static FlickrUserInfoProvider getFlickrUser(DataApp app, 
			String userId, String userName, int iconRes, FlickrUserClickListener listener, 
			FlickrAlbumProvider.PicasaAlbumFactory albumFactory, 
			FlickrPhotoProvider.FlickrPhotoFactory photoFactory) { 
		if (!isEmpty(userId)) { 
			FlickrUserInfoProvider p = new FlickrUserInfoProvider(app, 
					userId, iconRes, listener, albumFactory, photoFactory);
			p.setTitle(userName);
			return p;
		}
		
		return null;
	}
	
	public static FlickrGroupInfoProvider getFlickrGroup(DataApp app, 
			GroupItem item, int iconRes, FlickrUserClickListener listener, 
			FlickrPhotoProvider.FlickrPhotoFactory factory) { 
		if (item == null) return null;
		
		FlickrGroupInfoProvider p = new FlickrGroupInfoProvider(app, 
				item.getGroupData(), item.getGroupName(), iconRes, listener, factory);
		
		return p;
	}
	
	public static FlickrTopicReplyProvider getFlickrDiscuss(DataApp app, 
			TopicItem item, int iconRes, FlickrUserClickListener listener) { 
		if (item == null) return null;
		
		FlickrTopicReplyProvider p = new FlickrTopicReplyProvider(
				item.getTopicData().getTopicId(), item.getTopicData().getTopicSubject(), 
				iconRes, listener);
		
		return p;
	}
	
}
