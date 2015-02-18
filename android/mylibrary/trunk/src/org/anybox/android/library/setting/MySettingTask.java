package org.anybox.android.library.setting;

import android.app.Activity;

import org.anybox.android.library.AppActivity;
import org.anybox.android.library.MyApp;
import org.anybox.android.library.R;
import org.javenstudio.android.setting.TaskSetting;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingScreen;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxAccount;

public class MySettingTask extends TaskSetting {
	private static final Logger LOG = Logger.getLogger(MySettingTask.class);

	public MySettingTask(SettingCategory category) {
		super(category); 
	}

	@Override
	public boolean onSettingClick(Setting setting) {
		if (setting == null) return false;
		
		SettingScreen screen = setting.getParentScreen();
		Activity activity = screen != null ? screen.getActivity() : null;
		if (LOG.isDebugEnabled()) {
			LOG.debug("onSettingClick: setting=" + setting 
					+ " activity=" + activity);
		}
		
		if (activity != null) {
			AnyboxAccount account = MyApp.getInstance().getAccountApp().getAccount();
			if (account != null) {
				AppActivity.actionActivity(activity, account.getTaskListProvider());
				activity.overridePendingTransition(R.anim.activity_right_enter, 
						R.anim.activity_left_exit);
			}
		}
		
		return true;
	}
	
}
