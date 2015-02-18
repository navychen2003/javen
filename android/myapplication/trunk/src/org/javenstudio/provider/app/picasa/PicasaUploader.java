package org.javenstudio.provider.app.picasa;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.image.FileImageInfo;
import org.javenstudio.android.data.image.FileInfo;
import org.javenstudio.android.data.image.FileVideoInfo;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.local.LocalHelper;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.UploadData;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.task.upload.UploadHandler;
import org.javenstudio.provider.task.upload.UploadDataInfo;
import org.javenstudio.provider.task.upload.UploaderBase;

public class PicasaUploader extends UploaderBase {
	private static final Logger LOG = Logger.getLogger(PicasaUploader.class);

	private static final String PREFIX = "picasa";
	
	public static void enqueueUpload(Context context, 
			PicasaAlbumPhotoSet album, int type, Object data) { 
		if (context == null || album == null || data == null) 
			return;
		
		switch (type) { 
		case MediaItem.MEDIA_TYPE_IMAGE: 
			enqueueUploadImage(context, album, data);
			break;
			
		case MediaItem.MEDIA_TYPE_VIDEO: 
			enqueueUploadVideo(context, album, data);
			break;
			
		default:
			if (LOG.isDebugEnabled()) {
				LOG.debug("enqueueUpload: unknown type, type=" + type 
						+ " account=" + album.getAccount() + " data=" + data);
			}
			break;
		}
	}
	
	public static void enqueueUploadVideo(Context context, 
			PicasaAlbumPhotoSet album, Object data) { 
		if (context == null || album == null || data == null) 
			return;
		
		SystemUser account = album.getAccount();
		if (account == null) 
			return;
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("enqueueUploadVideo: account=" + account + " data=" + data);
		
		if (data instanceof Intent) { 
			Intent intent = (Intent)data;
			Uri contentUri = intent.getData();
			
			String filepath = LocalHelper.getFilePath(context, contentUri);
			FileInfo file = filepath != null && filepath.length() > 0 ? 
					new FileVideoInfo(filepath) : null;
			
			if (contentUri != null && file != null) { 
				insertQueue(album, contentUri, file);
				return;
			}
		}
		
		if (LOG.isWarnEnabled())
			LOG.warn("enqueueUploadVideo: unknown data: " + data);
	}
	
	public static void enqueueUploadImage(Context context, 
			PicasaAlbumPhotoSet album, Object data) { 
		if (context == null || album == null || data == null) 
			return;
		
		SystemUser account = album.getAccount();
		if (account == null) 
			return;
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("enqueueUploadImage: account=" + account + " data=" + data);
		
		if (data instanceof Intent) { 
			Intent intent = (Intent)data;
			Uri contentUri = intent.getData();
			
			String filepath = LocalHelper.getFilePath(context, contentUri);
			FileInfo file = filepath != null && filepath.length() > 0 ? 
					new FileImageInfo(filepath) : null;
			
			if (contentUri != null && file != null) { 
				insertQueue(album, contentUri, file);
				return;
			}
		}
		
		if (LOG.isWarnEnabled())
			LOG.warn("enqueueUploadImage: unknown data: " + data);
	}

	private static void insertQueue(PicasaAlbumPhotoSet album, 
			Uri contentUri, FileInfo file) { 
		if (album == null || contentUri == null || file == null) 
			return;
		
		SystemUser account = album.getAccount();
		if (account == null) 
			return;
		
		String uriString = contentUri.toString();
		UploadData[] uploads = ContentHelper.getInstance().queryUploadByUri(uriString);
		if (uploads != null && uploads.length > 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("insertQueue: already exists, account=" + account 
						+ " uri=" + contentUri + " file=" + file);
			}
			return;
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("insertQueue: account=" + account 
					+ " uri=" + contentUri + " file=" + file);
		}
		
		String datapath = uriString;
        int pos = datapath.indexOf("://");
        if (pos >= 0) datapath = datapath.substring(pos+2); 
        if (datapath == null || datapath.length() == 0)
        	datapath = file.getDataPath().toString();
        
		UploadData entity = ContentHelper.getInstance().newUpload();
		entity.setContentName(file.getFileName());
		entity.setContentUri(uriString);
		entity.setContentType(file.getContentType());
		entity.setDataPath(datapath);
		entity.setFilePath(file.getFilePath());
		entity.setDestPrefix(PREFIX);
		entity.setDestAccount(account.getAccountName());
		entity.setDestPath(album.getTopSetLocation());
		entity.setDestPathId(album.getAlbumId());
		entity.setStatus(UploadData.STATUS_ADDED);
		entity.setStartTime(System.currentTimeMillis());
		
		entity.commitUpdates();
		//UploadHandler.actionReschedule(context); // start at AppActivity.start
	}
	
	public PicasaUploader(DataApp app, UploadHandler handler, int iconRes) { 
		super(app, handler, iconRes);
	}
	
	@Override
	public String getPrefix() {
		return PREFIX;
	}
	
	@Override
	public boolean startUploadThread(final UploadDataInfo info) {
		if (info == null) return false;
		
		try { 
			return doStartUploadThread(info); 
		} catch (Throwable e) { 
			onUploadException(info, e);
			return false;
		}
	}
	
	private boolean doStartUploadThread(final UploadDataInfo info) throws Throwable {
		final String accountName = info.getUploadData().getDestAccount();
		final SystemUser account = PicasaHelper.getAccount(
				getApplication().getMainActivity(), accountName);
		
		if (account == null) {
			if (LOG.isDebugEnabled())
				LOG.debug("startUploadThread: no account: " + accountName);
			
			onUploadFailed(info, UploadData.CODE_NOACCOUNT, null);
			return false;
		}
		
		final String location = info.getUploadData().getDestPath();
		final String filepath = info.getUploadData().getFilePath();
		final String filename = info.getUploadData().getContentName();
		final String contentType = info.getUploadData().getContentType();
		
		final HttpPost post = createHttpPost(info, location);
		if (contentType != null && contentType.startsWith("video/")) { 
			String entryType = "application/atom+xml";
			String entryXml = createEntryXml(filename, null);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("startUploadThread: multipart post, file=" + filepath 
						+ " contentType=" + contentType + " entry=" + entryXml);
			}
			
			post.setEntity(createMultipartEntity("multipart/related", 
					createStringPart("EntryInfo", entryXml, entryType), 
					createFilePart(info, filepath, contentType, filename)
				));
			
		} else { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("startUploadThread: normal post, file=" + filepath 
						+ " contentType=" + contentType);
			}
			
			post.setEntity(createFileEntity(info, filepath, contentType));
			post.setHeader("Slug", filename);
		}
		
		HtmlCallback cb = new HtmlCallback() {
				@Override 
				public void initRequest(HttpUriRequest request) { 
					PicasaHelper.initAuthRequest(getApplication().getContext(), 
							request, account);
				}
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
					onUploadFetching(info);
				}
				@Override
				public void onContentFetched(Object content, HttpException e) { 
					onUploadException(info, e);
				}
			};
		
		cb.setRefetchContent(true);
		cb.setSaveContent(false);
		cb.setFetchContent(true);
		
		onUploadStarting(info, post);
		
		FetchHelper.removeFailed(location);
		FetchHelper.scheduleFetchHtmlWithRequest(location, post, cb);
		
		return true;
	}
	
	private String createEntryXml(String title, String summary) { 
		return "<entry xmlns=\"http://www.w3.org/2005/Atom\">\n" +
				" <title>" + htmlEncode(title) + "</title>\n" + 
				" <summary>" + htmlEncode(summary) + "</summary>\n" +
				" <category scheme=\"http://schemas.google.com/g/2005#kind\"\n" +
				"   term=\"http://schemas.google.com/photos/2007#photo\"/>\n" +
				"</entry>";
	}
	
	static String htmlEncode(String str) { 
		return str != null ? TextUtils.htmlEncode(str) : "";
	}
	
}
