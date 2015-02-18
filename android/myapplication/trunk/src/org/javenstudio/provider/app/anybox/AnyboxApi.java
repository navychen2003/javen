package org.javenstudio.provider.app.anybox;

import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.javenstudio.android.ActionError;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.HttpResult;
import org.javenstudio.cocoka.net.http.HttpResultException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.util.Base64Util;

final class AnyboxApi {
	private static final Logger LOG = Logger.getLogger(AnyboxApi.class);

	//static final String PREFIX = "anybox";
	//static final String DEVICE_TYPE = "android";
	
	//static final String SITE_ADDRESS = "www.anybox.org";
	//static final String LIGHTNING_ADDRESS = SITE_ADDRESS;
	
	static interface ResponseListener {
		public void onContentFetched(String content, Throwable e);
	}
	
	static abstract class SecretJSONListener extends JSONListener {
		@Override
		protected String decodeContent(String content) throws IOException {
			if (LOG.isDebugEnabled())
				LOG.debug("decodeContent: content=" + content);
			
			return Base64Util.decodeSecret(content);
		}
	}
	
	static abstract class JSONListener implements ResponseListener {
		protected String decodeContent(String content) throws IOException {
			return content;
		}
		@Override
		public final void onContentFetched(String content, Throwable e) {
			JSONObject json = null;
			ActionError error = null;
			if (e != null) {
				error = new ActionError(getErrorAction(), -1, 
						e.toString(), null, e);
			}
			if (content != null && content.length() > 0) {
				try {
					JSONTokener parser = new JSONTokener(decodeContent(content));
					json = (JSONObject)parser.nextValue();
					if (json != null && json.has("error")) {
						JSONObject err = (JSONObject)json.get("error");
						if (err != null) {
							int code = err.getInt("code");
							String message = err.getString("msg");
							String trace = err.getString("trace");
							
							if (code != 0) {
								error = new ActionError(getErrorAction(), 
										code, message, trace);
							}
							
							if (LOG.isDebugEnabled()) {
								LOG.debug("onContentFetched: response error: code=" 
										+ code + " message=" + message);
							}
						}
					}
				} catch (Throwable ee) {
					if (error == null) {
						error = new ActionError(getErrorAction(), -1, 
								ee.toString(), null, ee);
					}
					
					if (LOG.isWarnEnabled())
						LOG.warn("onContentFetched: error: " + e, e);
				}
			}
			handleData(json != null ? new AnyboxData(json) : null, error);
		}
		
		public abstract void handleData(AnyboxData data, ActionError error);
		public abstract ActionError.Action getErrorAction();
	}
	
	static void request(String location, final ResponseListener listener) { 
		if (location == null) throw new NullPointerException();
		
		HtmlCallback cb = new HtmlCallback() {
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
				}
				@Override
				public void onHtmlFetched(String content) {
					if (listener != null)
						listener.onContentFetched(content, null);
				}
				@Override
				public void onHttpException(HttpException e) { 
					if (e != null && e instanceof HttpResultException) {
						HttpResultException hre = (HttpResultException)e;
						HttpResult result = hre.getHttpResult();
						if (result != null) {
							try {
								String content = result.getContentAsString();
								if (listener != null)
									listener.onContentFetched(content, e);
								return;
							} catch (Throwable ee) {
								if (LOG.isWarnEnabled())
									LOG.warn("onHttpException: error: " + ee, ee);
							}
						}
					}
					if (listener != null)
						listener.onContentFetched(null, e);
				}
			};
		
		cb.setRefetchContent(true);
		cb.setSaveContent(false);
		
		FetchHelper.removeFailed(location);
		
		cb.setFetchContent(true);
		FetchHelper.fetchHtml(location, cb);
	}
	
}
