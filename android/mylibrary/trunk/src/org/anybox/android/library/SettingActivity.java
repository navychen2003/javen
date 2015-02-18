package org.anybox.android.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.anybox.android.library.setting.MySettingAbout;
import org.anybox.android.library.setting.MySettingAccount;
import org.anybox.android.library.setting.MySettingGeneral;
import org.anybox.android.library.setting.MySettingPlugin;
import org.anybox.android.library.setting.MySettingTask;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingManager;
import org.javenstudio.cocoka.widget.setting.SettingScreen;
import org.javenstudio.provider.activity.AccountSettingActivity;

public class SettingActivity extends AccountSettingActivity {
	
	public static void actionSettings(Activity from) { 
		actionSettings((Context)from);
		from.overridePendingTransition(R.anim.activity_right_enter, R.anim.activity_fade_exit);
	}
	
	public static void actionSettings(Context from) { 
		Intent intent = new Intent(from, SettingActivity.class); 
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
		
		from.startActivity(intent);
	}
	
	public static Intent createIntent(String screenkey) { 
		Context context = ResourceHelper.getContext(); 
		
		Intent intent = new Intent(context, SettingActivity.class); 
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		if (screenkey != null && screenkey.length() > 0)
			intent.putExtra(EXTRA_SCREENKEY, screenkey);
		
		return intent; 
	}
	
	@Override
	public MyApp getDataApp() {
		return MyApp.getInstance();
	}

	@Override
	protected SettingManager createSettingManager() {
		return ((MyImplements)MyImplements.getInstance()).getSettingManager(); 
	}
	
	@Override
	protected SettingScreen createSettingScreen(String key) {
		SettingScreen screen = super.createSettingScreen(key);
		if (screen != null) screen.setTitle(R.string.setting_name);
		return screen;
	}

	@Override
	protected void doOnCreate(Bundle savedInstanceState) { 
		super.doOnCreate(savedInstanceState);
	}
	
	@Override
	protected void initSettingActionBar() {
		setActionBarIcon(R.drawable.ic_home_anybox_dark);
		setHomeAsUpIndicator(R.drawable.ic_ab_back_holo_dark);
		setActionBarTitleColor(getResources().getColor(R.color.actionbar_title_light));
		setActionBarBackgroundColor(getResources().getColor(R.color.actionbar_background_light));
		setContentBackgroundColor(getResources().getColor(R.color.content_background_light));
		
		super.initSettingActionBar();
	}

	@Override
	protected Setting createAboutSetting(SettingCategory category) {
		return new MySettingAbout(category);
	}

	@Override
	protected Setting createAccountSetting(SettingCategory category) {
		return new MySettingAccount(category);
	}
	
	@Override
	protected Setting createGeneralSetting(SettingCategory category) {
		return new MySettingGeneral(category); 
	}
	
	@Override
	protected Setting createPluginSetting(SettingCategory category) {
		return new MySettingPlugin(category); 
	}
	
	@Override
	protected Setting createTaskSetting(SettingCategory category) {
		return new MySettingTask(category); 
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{screen=" + getIntentSettingScreenKey() + "}";
	}
	
}
