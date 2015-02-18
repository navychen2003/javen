package org.javenstudio.provider.app.picasa;

import java.util.StringTokenizer;

import android.content.Context;
import android.text.TextUtils;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;

import org.javenstudio.android.account.SystemUser;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.common.util.Logger;

final class PicasaCreater {
	private static final Logger LOG = Logger.getLogger(PicasaCreater.class);

	private static boolean isEmpty(String s) { 
		return s == null || s.length() == 0;
	}
	
	public static boolean createAlbum(final Context context, 
			final SystemUser account, String name, String place, 
			String keywords, String summary, String access) throws HttpException { 
		if (context == null || account == null || isEmpty(name))
			return false;
		
		if (access == null || !access.equals("public"))
			access = "private";
		
		keywords = normalizeKeywords(keywords);
		
		String content = "<entry xmlns=\"http://www.w3.org/2005/Atom\"\n" +
				"   xmlns:media=\"http://search.yahoo.com/mrss/\"\n" +
				"   xmlns:gphoto=\"http://schemas.google.com/photos/2007\">\n" +
				" <title type=\"text\">" + htmlEncode(name) + "</title>\n" +
				" <summary type=\"text\">" + htmlEncode(summary) + "</summary>\n" +
				" <gphoto:location>" + htmlEncode(place) + "</gphoto:location>\n" +
				" <gphoto:access>" + access + "</gphoto:access>\n" +
				" <gphoto:timestamp>" + System.currentTimeMillis() + "</gphoto:timestamp>\n" +
				" <media:group>\n" +
				"  <media:keywords>" + htmlEncode(keywords) + "</media:keywords>\n" +
				" </media:group>\n" +
				" <category scheme=\"http://schemas.google.com/g/2005#kind\"" +
				"   term=\"http://schemas.google.com/photos/2007#album\"></category>\n" +
				"</entry>";
		
		if (LOG.isDebugEnabled())
			LOG.debug("createAlbum: account=" + account.getAccountName() + " content=" + content);
		
		final String location = GPhotoHelper.ACCOUNT_CREATE_ALBUM_URL + 
				PicasaHelper.canonicalizeUsername(account.getAccountName());
		
		final HttpPost post = new HttpPost(location);
		final HttpException[] exceptions = new HttpException[1];
		exceptions[0] = null;
		
		try { 
			StringEntity entity = new StringEntity(content, HTTP.UTF_8);
			entity.setContentType("application/atom+xml; charset=\"utf-8\"");
			entity.setContentEncoding("utf-8");
			post.setEntity(entity);
		} catch (Throwable ex) { 
			exceptions[0] = new HttpException(-1, ex.toString(), ex);
		}
		
		if (exceptions[0] == null) { 
			HtmlCallback cb = new HtmlCallback() {
					@Override 
					public void initRequest(HttpUriRequest request) { 
						PicasaHelper.initAuthRequest(context, request, account);
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
			FetchHelper.fetchHtmlWithRequest(location, post, cb);
		}
		
		HttpException exception = exceptions[0];
		if (exception != null) { 
			if (LOG.isWarnEnabled()) {
				LOG.warn("createAlbum: failed: " + exception + ", account=" 
						+ account.getAccountName() + " name=" + name);
			}
			
			throw exception;
		}
		
		return true;
	}
	
	static String normalizeKeywords(String str) { 
		if (str == null) return "";
		
		StringBuilder sbuf = new StringBuilder();
		StringTokenizer st = new StringTokenizer(str, " \t\r\n,\"\';:\\|£¬£»¡£¡°");
		
		while (st.hasMoreTokens()) { 
			String token = st.nextToken();
			if (token == null || token.length() == 0) 
				continue;
			
			if (sbuf.length() > 0) sbuf.append(", ");
			sbuf.append(token);
		}
		
		return sbuf.toString();
	}
	
	static String htmlEncode(String str) { 
		return str != null ? TextUtils.htmlEncode(str) : "";
	}
	
}
