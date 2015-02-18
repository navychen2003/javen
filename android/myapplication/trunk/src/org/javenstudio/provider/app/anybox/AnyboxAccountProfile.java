package org.javenstudio.provider.app.anybox;

import java.io.IOException;

import org.javenstudio.android.ActionError;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.util.StringUtils;

public class AnyboxAccountProfile {
	private static final Logger LOG = Logger.getLogger(AnyboxAccountProfile.class);

	private final AnyboxAccount mUser;
	//private final AnyboxData mData;
	//private final ActionError mError;
	private final long mRequestTime;
	
	private String mTags = null;
	private String mRegion = null;
	private String mBirthday = null;
	private String mSex = null;
	private String mNickName = null;
	private String mLastName = null;
	private String mFirstName = null;
	private String mIntro = null;
	private String mTimezone = null;
	private String mBrief = null;
	
	private AnyboxAccountProfile(AnyboxAccount user, 
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
	
	public String getTags() { return mTags; }
	void setTags(String val) { mTags = val; }
	
	public String getRegion() { return mRegion; }
	void setRegion(String val) { mRegion = val; }
	
	public String getBirthday() { return mBirthday; }
	void setBirthday(String val) { mBirthday = val; }
	
	public String getSex() { return mSex; }
	void setSex(String val) { mSex = val; }
	
	public String getNickName() { return mNickName; }
	void setNickName(String val) { mNickName = val; }
	
	public String getLastName() { return mLastName; }
	void setLastName(String val) { mLastName = val; }
	
	public String getFirstName() { return mFirstName; }
	void setFirstName(String val) { mFirstName = val; }
	
	public String getIntro() { return mIntro; }
	void setIntro(String val) { mIntro = val; }
	
	public String getTimezone() { return mTimezone; }
	void setTimezone(String val) { mTimezone = val; }
	
	public String getBrief() { return mBrief; }
	void setBrief(String val) { mBrief = val; }
	
	public void loadData(AnyboxData data) throws IOException {
		if (data == null) return;
		
		AnyboxData profile = data.get("profile");
		if (profile != null) {
			if (LOG.isDebugEnabled())
				LOG.debug("loadData: profile=" + profile);
			
			setTags(profile.getString("tags"));
			setRegion(profile.getString("region"));
			setBirthday(profile.getString("birthday"));
			setSex(profile.getString("sex"));
			setNickName(profile.getString("nickname"));
			setLastName(profile.getString("lastname"));
			setFirstName(profile.getString("firstname"));
			setIntro(profile.getString("intro"));
			setTimezone(profile.getString("timezone"));
			setBrief(profile.getString("brief"));
		}
	}
	
	public static void getProfile(AnyboxAccount user, ProviderCallback callback) {
		if (user == null) return;
		
		String url = user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/user/profile?wt=secretjson&action=info" 
				+ "&token=" + StringUtils.URLEncode(user.getAuthToken());
		
		ProfileListener listener = new ProfileListener(user);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			AnyboxAccountProfile profile = listener.mProfile;
			if (profile != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					error = null;
					profile.loadData(listener.mData);
					user.setProfile(profile);
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isErrorEnabled())
				LOG.error("getProfile: error: " + e, e);
		}
		
		if (callback != null && error != null) 
			callback.onActionError(error);
	}
	
	static class ProfileListener extends AnyboxApi.SecretJSONListener {
		private final AnyboxAccount mUser;
		private AnyboxAccountProfile mProfile = null;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public ProfileListener(AnyboxAccount user) {
			if (user == null) throw new NullPointerException();
			mUser = user;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			mProfile = new AnyboxAccountProfile(mUser, data, error, System.currentTimeMillis());
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
			return ActionError.Action.ACCOUNT_PROFILE;
		}
	}
	
}
