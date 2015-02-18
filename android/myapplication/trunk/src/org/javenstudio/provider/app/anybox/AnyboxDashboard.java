package org.javenstudio.provider.app.anybox;

import java.io.IOException;

import org.javenstudio.android.ActionError;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.notify.IAnnouncementNotifyData;
import org.javenstudio.util.StringUtils;

public class AnyboxDashboard {
	private static final Logger LOG = Logger.getLogger(AnyboxDashboard.class);
	
	public class HistoriesData {
		private final long mTotalCount;
		
		private HistoriesData(long totalcount) {
			mTotalCount = totalcount;
		}
		
		public long getTotalCount() { return mTotalCount; }
		
		public AnyboxHistory.SectionData[] getSections() { 
			return AnyboxDashboard.this.getSections();
		}
	}
	
	public class AnnouncementNotifyData implements IAnnouncementNotifyData {
		private final AnyboxAnnouncement.AnnouncementData[] mDatas;
		
		private AnnouncementNotifyData(AnyboxAnnouncement.AnnouncementData[] datas) {
			mDatas = datas;
		}

		@Override
		public IAnnouncementItem[] getItems() {
			return mDatas;
		}

		@Override
		public int getTotalCount() {
			return mDatas != null ? mDatas.length : 0;
		}
	}
	
	private final AnyboxAccount mUser;
	//private final AnyboxData mData;
	//private final ActionError mError;
	private final long mRequestTime;
	
	private AnnouncementNotifyData mAnnouncementData = null;
	private AnyboxAnnouncement.AnnouncementData[] mAnnouncements = null;
	private AnyboxHistory.SectionData[] mSections = null;
	private HistoriesData mHistories = null;
	
	private AnyboxDashboard(AnyboxAccount user, 
			AnyboxData data, ActionError error, long requestTime) {
		if (user == null) throw new NullPointerException();
		mUser = user;
		//mData = data;
		//mError = error;
		mRequestTime = requestTime;
	}
	
	public AnyboxAccount getUser() { return mUser; }
	//public AnyboxData getData() { return mData; }
	//public ActionError getError() { return mError; }
	public long getRequestTime() { return mRequestTime; }
	
	public AnyboxAnnouncement.AnnouncementData[] getAnnouncements() { return mAnnouncements; }
	public IAnnouncementNotifyData getAnnouncementData() { return mAnnouncementData; }
	
	public AnyboxHistory.SectionData[] getSections() { return mSections; }
	public HistoriesData getHistories() { return mHistories; }
	
	public void loadData(AnyboxData data) throws IOException {
		if (data == null) return;
		
		AnyboxAnnouncement.AnnouncementData[] anns = 
				AnyboxAnnouncement.loadAnnouncements(getUser(), 
						data.get("announcements"));
		
		loadHistories(data.get("histories"));
		
		mAnnouncementData = new AnnouncementNotifyData(anns);
		mAnnouncements = anns;
	}
	
	private void loadHistories(AnyboxData data) throws IOException {
		if (data == null) return;
		if (LOG.isDebugEnabled()) LOG.debug("loadHistories: data=" + data);
		
		AnyboxHistory.SectionData[] sections = 
				AnyboxHistory.loadSections(this, data.get("sections"));
		
		int totalcount = data.getInt("totalcount", 0);
		HistoriesData h = new HistoriesData(totalcount);
		
		mSections = sections;
		mHistories = h;
	}
	
	public static void getDashboard(AnyboxAccount user, ProviderCallback callback) {
		if (user == null) return;
		
		String url = user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/datum/dashboard?wt=secretjson&action=list&count=50&subfilecount=3" 
				+ "&lang=" + StringUtils.URLEncode(user.getApp().getLocalString()) 
				+ "&token=" + StringUtils.URLEncode(user.getAuthToken());
		
		DashboardListener listener = new DashboardListener(user);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			AnyboxDashboard dashboard = listener.mDashboard;
			if (dashboard != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					dashboard.loadData(listener.mData);
					user.setDashboard(dashboard);
					
					AnyboxLibrary.loadLibraries(user, listener.mData);
					AnyboxStorage.loadStorages(user, listener.mData);
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
	}
	
	static class DashboardListener extends AnyboxApi.SecretJSONListener {
		private final AnyboxAccount mUser;
		private AnyboxDashboard mDashboard = null;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public DashboardListener(AnyboxAccount user) {
			if (user == null) throw new NullPointerException();
			mUser = user;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			mDashboard = new AnyboxDashboard(mUser, data, error, System.currentTimeMillis());
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
			return ActionError.Action.ACCOUNT_DASHBOARD;
		}
	}
	
}
