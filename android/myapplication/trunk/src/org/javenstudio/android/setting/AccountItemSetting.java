package org.javenstudio.android.setting;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountHelper;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingManager;
import org.javenstudio.cocoka.widget.setting.SettingScreen;
import org.javenstudio.common.util.Logger;

public abstract class AccountItemSetting extends Setting 
		implements Setting.ViewBinder, ImageListener {
	private static final Logger LOG = Logger.getLogger(AccountItemSetting.class);
	
	private final AccountApp mApp;
	private final AccountData mAccount;
	
	private HttpImage mImage = null;
	private String mImageURL = null;
	private int mFetchRequest = 0;
	
	public AccountItemSetting(SettingManager manager, 
			AccountApp app, AccountData account) {
    	super(manager); 
    	mAccount = account;
    	mApp = app;
    }
    
    public AccountItemSetting(SettingManager manager, 
    		AttributeSet attrs, AccountApp app, AccountData account) {
        super(manager, attrs);
        mAccount = account;
        mApp = app;
    }
    
    public AccountData getAccount() { return mAccount; }
    public AccountApp getAccountApp() { return mApp; }
    
    public boolean isSelected() {
    	boolean selected = false;
		AccountUser user = getAccountApp().getAccount();
		if (user != null) {
			String name = user.getMailAddress();
			if (name != null && name.equals(getAccount().getMailAddress()))
				selected = true;
		}
		return selected;
    }
    
    public void onAccountClick() {
    	if (isSelected()) return;
    	if (LOG.isDebugEnabled()) LOG.debug("onAccountClick: account=" + getAccount());
    	
    	final SettingScreen screen = getParentScreen();
    	final Activity activity = screen != null ? screen.getActivity() : null;
    	if (activity == null || activity.isDestroyed())
    		return;
    	
    	AccountHelper.onAccountSwitch(activity, getAccountApp(), getAccount());
    }
    
    public void onAccountRemove() {
    	if (isSelected()) return;
    	if (LOG.isDebugEnabled()) LOG.debug("onAccountRemove: account=" + getAccount());
    	
    	final SettingScreen screen = getParentScreen();
    	final Activity activity = screen != null ? screen.getActivity() : null;
    	if (activity == null || activity.isDestroyed())
    		return;
    	
    	AccountHelper.onAccountRemove(activity, 
    			getAccountApp(), getAccount(), getRemoveListener());
    }
    
    protected abstract AccountHelper.OnRemoveListener getRemoveListener();
    
    @Override
	public void onImageEvent(Image image, ImageEvent event) {
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (!isImageLocation(location)) return;
			if (LOG.isDebugEnabled())
				LOG.debug("onImageEvent: location=" + location + " event=" + event);
		}
		
		if (event instanceof HttpEvent) { 
			HttpEvent e = (HttpEvent)event;
			switch (e.getEventType()) { 
			case FETCH_START: 
				if (mFetchRequest < 0) mFetchRequest = 0;
				mFetchRequest ++;
				break;
			default: 
				mFetchRequest --;
				if (mFetchRequest < 0) mFetchRequest = 0;
				break;
			}
			
			onHttpImageEvent(image, e);
		}
	}
    
	public Drawable getAvatarDrawable(int size, int padding) {
		HttpImage image = getImage();
		if (image != null) return image.getThumbnailDrawable(size, size);
		return null;
	}
	
	private synchronized HttpImage getImage() {
		if (mImage == null) {
			String imageURL = getAccountApp().getAccountAvatarURL(getAccount(), 192);
			
			if (imageURL != null && imageURL.length() > 0) { 
				mImageURL = imageURL;
				mImage = HttpResource.getInstance().getImage(imageURL);
				mImage.addListener(this);
				
				HttpImageItem.requestDownload(mImage, false);
			}
		}
		return mImage;
	}
	
	protected boolean isImageLocation(String location) {
		if (location != null && location.length() > 0) {
			String imageURL = mImageURL;
			if (imageURL != null && imageURL.equals(location))
				return true;
		}
		return false;
	}
    
	protected void onHttpImageEvent(final Image image, HttpEvent event) { 
		if (image == null) return;
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					image.invalidateDrawables();
				}
			});
	}
	
    @Override
    public Setting.ViewBinder getViewBinder() { 
    	return this;
    }

	@Override
	public int getViewResource() {
		return R.layout.setting_accountitem;
	}

	@Override
	public boolean bindSettingView(Setting setting, View view) {
		if (setting != this || view == null) return false;
		
		final ImageView avatarView = (ImageView)view.findViewById(R.id.setting_accountitem_user_avatar);
		if (avatarView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.setting_avatar_background);
			if (backgroundRes != 0) avatarView.setBackgroundResource(backgroundRes);
			
			int size = AppResources.getInstance().getContext().getResources().getDimensionPixelSize(R.dimen.setting_accountitem_avatar_size);
			Drawable d = getAvatarDrawable(size, 0);
			if (d != null) { 
				DataBinder.onImageDrawablePreBind(d, avatarView);
				avatarView.setImageDrawable(d);
				DataBinder.onImageDrawableBinded(d, false);
			}
		}
		
		final ImageView actionView = (ImageView)view.findViewById(R.id.setting_accountitem_action_image);
		if (actionView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.setting_action_background);
			if (backgroundRes != 0) actionView.setBackgroundResource(backgroundRes);
			
			if (isSelected()) {
				int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.setting_account_action_icon_selected);
				if (iconRes != 0) actionView.setImageResource(iconRes);
			} else {
				int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.setting_account_action_icon_remove);
				if (iconRes != 0) actionView.setImageResource(iconRes);
				
				actionView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							onAccountRemove();
						}
					});
			}
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.setting_accountitem_title);
		if (titleView != null) {
			titleView.setText(getAccount().getFullName());
		}
		
		final TextView textView = (TextView)view.findViewById(R.id.setting_accountitem_text);
		if (textView != null) {
			String text = AppResources.getInstance().getContext().getString(R.string.login_timeago_message);
			String timeago = AppResources.getInstance().formatTimeAgo(
					System.currentTimeMillis() - getAccount().getUpdateTime());
			textView.setText(String.format(text, timeago));
		}
		
		return true;
	}

	@Override
	public boolean bindSettingBackground(Setting setting, View view) {
		if (setting != this || view == null) return false;
		return false;
	}
	
}
