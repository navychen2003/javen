package org.javenstudio.falcon.setting.cluster;

import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.javenstudio.common.util.Logger;
import org.javenstudio.mime.Base64Util;

final class AnyboxApi {
	private static final Logger LOG = Logger.getLogger(AnyboxApi.class);

	static abstract class SecretJSONListener extends JSONListener {
		@Override
		protected String decodeContent(String content) throws IOException {
			if (LOG.isDebugEnabled())
				LOG.debug("decodeContent: content=" + content);
			
			return Base64Util.decodeSecret(content);
		}
	}
	
	static abstract class JSONListener implements IFetchListener {
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
	
}
