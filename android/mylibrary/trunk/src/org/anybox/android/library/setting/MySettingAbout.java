package org.anybox.android.library.setting;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import org.anybox.android.library.R;
import org.anybox.android.library.SettingActivity;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.setting.AboutSetting;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.common.util.Logger;

public class MySettingAbout extends AboutSetting {
	private static final Logger LOG = Logger.getLogger(MySettingAbout.class);

	public MySettingAbout(SettingCategory category) {
		super(category); 
	}
	
	@Override
	public int getHomeIconRes() {
		return R.drawable.ic_home_anybox_dark;
	}
	
	@Override
	protected Intent createIntent(String screenKey) {
		return SettingActivity.createIntent(screenKey); 
	}
	
	@Override
	protected Setting createVersionSetting(SettingCategory category) {
		final Setting setting = category.createSetting(
				category.getKey() + ".version", 
				R.string.setting_about_version);
		
		String label = getContext().getString(R.string.label_about_version);
		String version = getContext().getString(R.string.app_versionname);
		setting.setSummary(String.format(label, version));
		
		setting.setOnSettingClickListener(new Setting.OnSettingClickListener() {
				@Override
				public boolean onSettingClick(Setting setting) {
					Activity activity = getActivity();
					if (activity != null && activity instanceof IActivity) {
						IActivity iactivity = (IActivity)activity;
						iactivity.getActivityHelper().showAboutDialog();
					}
					return true;
				}
			});
		
		return setting;
	}
	
	@Override
	protected Setting createAgreementSetting(SettingCategory category) {
		final Setting setting = category.createSetting(
				category.getKey() + ".agreement", 
				R.string.setting_about_agreement);
		
		setting.setOnSettingClickListener(new Setting.OnSettingClickListener() {
				@Override
				public boolean onSettingClick(Setting setting) {
					startBrowser(getActivity(), R.string.url_agreement);
					return true;
				}
			});
		
		return setting;
	}

	@Override
	protected Setting createPrivacySetting(SettingCategory category) {
		final Setting setting = category.createSetting(
				category.getKey() + ".privacy", 
				R.string.setting_about_privacy);
		
		setting.setOnSettingClickListener(new Setting.OnSettingClickListener() {
				@Override
				public boolean onSettingClick(Setting setting) {
					startBrowser(getActivity(), R.string.url_privacy);
					return true;
				}
			});
		
		return setting;
	}

	@Override
	protected Setting createTermsSetting(SettingCategory category) {
		final Setting setting = category.createSetting(
				category.getKey() + ".terms", 
				R.string.setting_about_terms);
		
		setting.setOnSettingClickListener(new Setting.OnSettingClickListener() {
				@Override
				public boolean onSettingClick(Setting setting) {
					startBrowser(getActivity(), R.string.url_terms);
					return true;
				}
			});
		
		return setting;
	}

	@Override
	protected Setting createLawSetting(SettingCategory category) {
		final Setting setting = category.createSetting(
				category.getKey() + ".law", 
				R.string.setting_about_law);
		
		setting.setOnSettingClickListener(new Setting.OnSettingClickListener() {
				@Override
				public boolean onSettingClick(Setting setting) {
					startBrowser(getActivity(), R.string.url_law);
					return true;
				}
			});
		
		return setting;
	}
	
	private void startBrowser(Activity activity, int urlRes) {
		if (activity == null || urlRes == 0) return;
		
		try {
			String url = activity.getString(urlRes);
			
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			activity.startActivity(intent);
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("startBrowser: error: " + e, e);
		}
	}
	
}
