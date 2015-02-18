package org.javenstudio.provider.app.picasa;

import android.content.Context;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;

import org.javenstudio.android.account.SystemUser;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.common.util.Logger;

final class PicasaDeleter {
	private static final Logger LOG = Logger.getLogger(PicasaDeleter.class);

	private static boolean isEmpty(String s) { 
		return s == null || s.length() == 0;
	}
	
	public static boolean deletePhoto(final Context context, 
			final SystemUser account, String albumId, String photoId) 
					throws HttpException { 
		if (context == null || account == null || isEmpty(albumId) | isEmpty(photoId))
			return false;
		
		final String location = String.format(GPhotoHelper.ACCOUNT_DELETE_PHOTO_URL, 
				PicasaHelper.canonicalizeUsername(account.getAccountName()), 
				albumId, photoId);
		
		if (LOG.isDebugEnabled())
			LOG.debug("deletePhoto: account=" + account.getAccountName() + " uri=" + location);
		
		final HttpDelete delete = new HttpDelete(location);
		final HttpException[] exceptions = new HttpException[1];
		exceptions[0] = null;
		
		HtmlCallback cb = new HtmlCallback() {
				@Override 
				public void initRequest(HttpUriRequest request) { 
					PicasaHelper.initAuthRequest(context, request, account);
					if (request != null) 
						request.addHeader("If-Match", "*");
				}
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
				}
				@Override
				public void onContentFetched(Object content, HttpException e) { 
					exceptions[0] = e;
				}
			};
		
		cb.setRefetchContent(true);
		cb.setSaveContent(false);
		cb.setFetchContent(true);
		
		FetchHelper.removeFailed(location);
		FetchHelper.fetchHtmlWithRequest(location, delete, cb);
		
		HttpException exception = exceptions[0];
		if (exception != null) { 
			if (LOG.isWarnEnabled()) {
				LOG.warn("deletePhoto: failed: " + exception + ", account=" 
						+ account.getAccountName() + " photoId=" + photoId);
			}
			
			throw exception;
		}
		
		return true;
	}
	
	public static boolean deleteAlbum(final Context context, 
			final SystemUser account, String albumId) throws HttpException { 
		if (context == null || account == null || isEmpty(albumId))
			return false;
		
		final String location = String.format(GPhotoHelper.ACCOUNT_DELETE_ALBUM_URL, 
				PicasaHelper.canonicalizeUsername(account.getAccountName()), 
				albumId);
		
		if (LOG.isDebugEnabled())
			LOG.debug("deleteAlbum: account=" + account.getAccountName() + " uri=" + location);
		
		final HttpDelete delete = new HttpDelete(location);
		final HttpException[] exceptions = new HttpException[1];
		exceptions[0] = null;
		
		HtmlCallback cb = new HtmlCallback() {
				@Override 
				public void initRequest(HttpUriRequest request) { 
					PicasaHelper.initAuthRequest(context, request, account);
					if (request != null) 
						request.addHeader("If-Match", "*");
				}
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
				}
				@Override
				public void onContentFetched(Object content, HttpException e) { 
					exceptions[0] = e;
				}
			};
		
		cb.setRefetchContent(true);
		cb.setSaveContent(false);
		cb.setFetchContent(true);
		
		FetchHelper.removeFailed(location);
		FetchHelper.fetchHtmlWithRequest(location, delete, cb);
		
		HttpException exception = exceptions[0];
		if (exception != null) { 
			if (LOG.isWarnEnabled()) {
				LOG.warn("deleteAlbum: failed: " + exception + ", account=" 
						+ account.getAccountName() + " albumId=" + albumId);
			}
			
			throw exception;
		}
		
		return true;
	}
	
}
