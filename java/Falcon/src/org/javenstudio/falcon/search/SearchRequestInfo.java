package org.javenstudio.falcon.search;

import java.io.Closeable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.TimeZoneUtils;

public class SearchRequestInfo {
	private static Logger LOG = Logger.getLogger(SearchRequestInfo.class);
	
	static final ThreadLocal<SearchRequestInfo> sThreadLocal = 
			new ThreadLocal<SearchRequestInfo>();

	protected ResponseBuilder mBuilder;
	protected List<Closeable> mCloseHooks;
	protected ISearchRequest mRequest;
	protected ISearchResponse mResponse;
	protected Date mNow;
	protected TimeZone mTimeZone;
	
	public static SearchRequestInfo getRequestInfo() {
		return sThreadLocal.get();
	}

	public static void setRequestInfo(SearchRequestInfo info) {
		// TODO: temporary sanity check... this can be changed to just an assert in the future
		SearchRequestInfo prev = sThreadLocal.get();
		if (prev != null) {
			LOG.error("Previous RequestInfo was not closed! request=" 
					+ prev.mRequest.getOriginalParams().toString());  
		}
		assert prev == null;

		sThreadLocal.set(info);
	}

	public static void clearRequestInfo() {
		try {
			SearchRequestInfo info = sThreadLocal.get();
			if (info != null && info.mCloseHooks != null) {
				for (Closeable hook : info.mCloseHooks) {
					try {
						hook.close();
					} catch (Throwable throwable) {
						LOG.error("Exception during close hook", throwable);
					}
				}
			}
		} finally {
			sThreadLocal.remove();
		}
	}

	public SearchRequestInfo(ISearchRequest req, ISearchResponse rsp) {
		mRequest = req;
		mResponse = rsp;    
	}

	public Date getNOW() throws ErrorException {    
		if (mNow != null) return mNow;

		long ms = 0;
		String nowStr = mRequest.getParams().get(CommonParams.NOW);

		if (nowStr != null) 
			ms = Long.parseLong(nowStr);
		else 
			ms = mRequest.getStartTime();
		
		mNow = new Date(ms);
		
		return mNow;
	}

	/** The TimeZone specified by the request, or null if none was specified */
	public TimeZone getClientTimeZone() throws ErrorException {    
		if (mTimeZone == null)  {
			String tzStr = mRequest.getParams().get(CommonParams.TZ);
			if (tzStr != null) {
				mTimeZone = TimeZoneUtils.getTimeZone(tzStr);
				if (mTimeZone == null) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
							"JVM does not support TZ: " + tzStr);
				}
			} 
		}
		return mTimeZone;
	}

	public ISearchRequest getRequest() { return mRequest; }
	public ISearchResponse getResponse() { return mResponse; }

	/** May return null if the request handler is not based on SearchHandler */
	public ResponseBuilder getResponseBuilder() {
		return mBuilder;
	}

	public void setResponseBuilder(ResponseBuilder rb) {
		mBuilder = rb;
	}

	public void addCloseHook(Closeable hook) {
		// is this better here, or on Request?
		synchronized (this) {
			if (mCloseHooks == null) 
				mCloseHooks = new LinkedList<Closeable>();
			
			mCloseHooks.add(hook);
		}
	}
	
}
