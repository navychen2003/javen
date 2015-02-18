package org.anybox.android.library;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.text.Html;
import android.view.View;

import org.anybox.android.library.app.MyRefreshHelper;
import org.javenstudio.android.ActionError;
import org.javenstudio.android.SourceHelper;
import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.app.AlertDialogHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.PhotoHelper;
import org.javenstudio.android.app.SortType;
import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.information.InformationBinderFactory;
import org.javenstudio.android.information.InformationOne;
import org.javenstudio.android.information.InformationOperation;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.BaseResources;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.host.HostListClusterItem;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.ISectionSearch;
import org.javenstudio.provider.library.select.LocalFolderItem;

public class MyResources extends AppResources {
	private static final Logger LOG = Logger.getLogger(MyResources.class);

	private final MyDialogHelper mDialogHelper = new MyDialogHelper();
	
	@Override
	public AlertDialogHelper getDialogHelper() {
		return mDialogHelper;
	}
	
	@Override
	protected void initialize(Context context) { 
		super.initialize(context);
		
		int glowDrawableId = context.getResources().getIdentifier("overscroll_glow", "drawable", "android");
		int edgeDrawableId = context.getResources().getIdentifier("overscroll_edge", "drawable", "android");
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("initialize: change overscroll color: glowId=" + glowDrawableId 
					+ " edgeId=" + edgeDrawableId);
		}
		
		Drawable androidGlow = context.getResources().getDrawable(glowDrawableId);
		Drawable androidEdge = context.getResources().getDrawable(edgeDrawableId);
		
		int filterColor = context.getResources().getColor(R.color.overscroll_filter);
		if (androidGlow != null) {
			androidGlow.setColorFilter(filterColor, PorterDuff.Mode.MULTIPLY);
			//androidGlow.setColorFilter(filterColor, PorterDuff.Mode.LIGHTEN);
			androidGlow.setAlpha(1);
		}
		if (androidEdge != null) {
			androidEdge.setColorFilter(filterColor, PorterDuff.Mode.MULTIPLY);
			//androidEdge.setColorFilter(filterColor, PorterDuff.Mode.LIGHTEN);
			androidEdge.setAlpha(1);
		}
	}
	
	@Override
	public int getLayoutRes(int id) { 
		switch (id) {
		case BaseResources.layout.ptr_header_vertical:
			return R.layout.ptr_header;
		}
		return super.getLayoutRes(id); 
	}
	
	@Override
	public View findViewById(View view, int layoutId, int id) { 
		switch (layoutId) {
		case BaseResources.layout.ptr_header_vertical: {
			switch (id) {
			case BaseResources.view.ptr_header_inner:
				return view.findViewById(R.id.ptr_header_inner);
			case BaseResources.view.ptr_header_image:
				return view.findViewById(R.id.ptr_header_image);
			case BaseResources.view.ptr_header_progress:
				return view.findViewById(R.id.ptr_header_progress);
			case BaseResources.view.ptr_header_sub_text:
				return view.findViewById(R.id.ptr_header_sub_text);
			case BaseResources.view.ptr_header_text:
				return view.findViewById(R.id.ptr_header_text);
			}
			return null;
		}}
		return super.findViewById(view, layoutId, id); 
	}
	
	@Override
	public int getDrawableRes(int id) { 
		switch (id) { 
		case BaseResources.drawable.icon_menu_undo:
			return R.drawable.ic_menu_revert_holo_dark;
		case BaseResources.drawable.icon_control_play:
			return R.drawable.ic_control_play;
		case BaseResources.drawable.overscroll_edge:
			return R.drawable.overscroll_edge;
		case BaseResources.drawable.overscroll_glow:
			return R.drawable.overscroll_glow;
		case BaseResources.drawable.ptr_rotate_spinner:
			return MyRefreshHelper.getDefault().getLoadingDrawableRes();
		case BaseResources.drawable.photo_action_background:
			return R.drawable.photo_action_selector;
		case BaseResources.drawable.photo_action_selected_background:
			return R.drawable.photo_action_selected;
		case BaseResources.drawable.photo_controls_background:
			return R.drawable.photo_controls_selector;
		case drawable.card_image_background: 
			return R.drawable.bg_image_background; 
		case drawable.card_header_background: 
			return R.drawable.bg_header_background;
		case drawable.card_avatar_selector:
			return R.drawable.ic_avatar;
		case drawable.card_avatar_round_selector:
			return R.drawable.avatar_round_selector;
		case drawable.card_list_selector:
			return R.drawable.list_selector_holo_light;
		case drawable.accountinfo_avatar_round_selector:
		case drawable.accountmenu_avatar_round_selector:
		case drawable.sectioninfo_avatar_round_selector:
			return R.drawable.avatar_round_selector;
		case drawable.accountinfo_background:
		case drawable.accountmenu_background:
			return R.drawable.background_02;
		//case drawable.accountinfo_actionbar_backfround:
		//	return R.drawable.bg_account_actionbar;
		case drawable.accountinfo_overlay:
			return R.drawable.ic_camera_grey;
		case drawable.sectioninfo_overlay:
			return R.drawable.ic_camera_grey;
		case drawable.profile_tab_active_item_selector:
			return R.drawable.profile_tab_active_item_selector;
		case drawable.profile_tab_inactive_item_selector:
			return R.drawable.profile_tab_inactive_item_selector;
		//case drawable.profile_card_background:
		//	return R.drawable.card_purpleborder;
		case drawable.notify_avatar_background:
			return R.drawable.ic_avatar;
		//case drawable.notify_empty_state_image:
		//	return R.drawable.emptystate_notifications;
		case drawable.setting_avatar_background:
			return R.drawable.ic_avatar;
		//case drawable.icon_setting_general:
		//	return R.drawable.ic_setting_general;
		case drawable.icon_setting_checkbox_off:
			return R.drawable.ic_setting_checkbox_off;
		case drawable.icon_setting_checkbox_on:
			return R.drawable.ic_setting_checkbox_on;
		case drawable.icon_setting_radiobox_off:
			return R.drawable.ic_setting_radiobox_off;
		case drawable.icon_setting_radiobox_on:
			return R.drawable.ic_setting_radiobox_on;
		case drawable.icon_setting_checkin:
			return R.drawable.ic_setting_checkin;
		case drawable.icon_setting_arrow:
			return R.drawable.btn_setting_arrow;
		case drawable.bg_setting_action_selector:
			return R.drawable.bg_setting_action_selector;
		case drawable.bg_setting_first_selector:
			return R.drawable.bg_setting_first_selector;
		case drawable.bg_setting_last_selector:
			return R.drawable.bg_setting_last_selector;
		case drawable.bg_setting_middle_selector:
			return R.drawable.bg_setting_middle_selector;
		case drawable.bg_setting_single_selector:
			return R.drawable.bg_setting_single_selector;
		case drawable.bg_setting_first_normal:
			return R.drawable.bg_setting_single_normal;
		case drawable.bg_setting_last_normal:
			return R.drawable.bg_setting_single_normal;
		case drawable.bg_setting_middle_normal:
			return R.drawable.bg_setting_single_normal;
		case drawable.bg_setting_single_normal:
			return R.drawable.bg_setting_single_normal;
		case drawable.bg_setting_button_selector:
			return R.drawable.bg_setting_button_selector;
		//case drawable.dashboard_emptystate_image:
		//	return R.drawable.emptystate_dashboard;
		//case drawable.dashboard_background_image:
		//	return R.drawable.background_dashboard;
		case drawable.setting_account_action_icon_selected:
			return R.drawable.ic_action_accept;
		case drawable.setting_account_action_icon_remove:
			return R.drawable.ic_action_cancel_red;
		case drawable.host_action_icon_remove:
			return R.drawable.ic_action_cancel_red;
		case drawable.setting_action_background:
			return R.drawable.bg_setting_action_selector;
		case drawable.bg_task_action_selector:
			return R.drawable.bg_setting_action_selector;
		case drawable.bg_host_action_selector:
			return R.drawable.bg_setting_action_selector;
		case drawable.bg_dialog_singlechoice_item:
			return R.drawable.list_selector_holo_light;
		case drawable.section_item_background:
			return R.drawable.section_list_selector;
		case drawable.section_item_background_selected:
			return R.drawable.section_list_selected;
		case drawable.section_action_background:
			return R.drawable.section_action_selector;
		case drawable.section_list_category_background:
			return R.drawable.list_selector_holo_light;
		case drawable.section_list_shortcut_background:
			return R.drawable.list_selector_holo_light;
		//case drawable.section_empty_folder_image:
		//	return R.drawable.bg_empty_folder;
		//case drawable.icon_library_default:
		//	return R.drawable.ic_nav_library;
		//case drawable.icon_library_image:
		//	return R.drawable.ic_nav_picture;
		//case drawable.icon_library_audio:
		//	return R.drawable.ic_nav_music;
		//case drawable.icon_library_video:
		//	return R.drawable.ic_nav_film;
		case drawable.btn_dialog_checkmark:
			return R.drawable.btn_radio_holo_light;
		case drawable.bg_item_selected:
			return R.drawable.item_selected_holo_light;
		case drawable.icon_homeasup_back_indicator:
			return R.drawable.ic_ab_back_holo_light;
		case drawable.icon_homeasup_menu_indicator:
			return R.drawable.ic_ab_menu_holo_light;
		case drawable.icon_dialog_back_indicator:
			return R.drawable.ic_ab_back_holo_dark;
		case drawable.icon_task_cancel:
			return R.drawable.ic_action_cancel_red;
		case drawable.icon_task_done:
			return R.drawable.ic_action_accept;
		}
		return super.getDrawableRes(id); 
	}
	
	@Override
	public int getColorRes(int id) { 
		switch (id) { 
		case color.progress_front_color: 
			return R.color.progress_front; 
		case color.progress_back_color: 
			return R.color.progress_back;
		//case color.accountinfo_actionbar_title_color:
		//	return R.color.white;
		case color.accountinfo_header_name_color:
			return R.color.accountinfo_header_name_color;
		case color.overviewspace_percent_color:
			return R.color.overviewspace_percent_color;
		//case color.dashboard_empty_title_color:
		//	return R.color.dashboard_empty_title_color;
		//case color.dashboard_background_color:
		//	return R.color.dashboard_background_color;
		case color.setting_summary_link_color:
			return R.color.dialog_message_link_color;
		case color.setting_title_color:
			return R.color.setting_title_enabled_color;
		}
		return super.getColorRes(id); 
	}
	
	public int getColorStateListRes(int id) { 
		switch (id) { 
		case color.accountmenu_header_title_color:
			return R.color.menu_title_color;
		case color.accountmenu_header_subtitle_color:
			return R.color.menu_subtitle_color;
		case color.setting_title_color:
			return R.color.setting_title_color;
		case color.dashboard_item_title_color:
			return R.color.dashboard_title_color;
		}
		return super.getColorStateListRes(id); 
	}
	
	@Override
	public int getStringRes(int id) { 
		switch (id) {
		case BaseResources.string.ptr_pull_label:
			return R.string.label_ptr_pull;
		case BaseResources.string.ptr_refreshing_label:
			return R.string.label_ptr_refreshing;
		case BaseResources.string.ptr_release_label:
			return R.string.label_ptr_release;
		case string.about_title:
			return R.string.about_title;
		case string.about_message:
			return R.string.about_message;
		case string.login_signingin_message:
			return R.string.login_signingin_message;
		case string.login_signingout_message:
			return R.string.login_signingout_message;
		//case string.dashboard_empty_title:
		//	return R.string.dashboard_empty_title;
		}
		return super.getStringRes(id); 
	}
	
	@Override
	public int getStyleRes(int id) { 
		switch (id) {
		//case style.dialog_alert_light:
		//	return R.style.AppDialogAlert_Light;
		//case style.dialog_alert_dark:
		//	return R.style.AppDialogAlert_Dark;
		}
		return super.getStyleRes(id); 
	}
	
	@Override
	public CharSequence getStringText(int id) { 
		switch (id) {
		case string.about_message:
			return getAboutMessage();
		}
		return super.getStringText(id); 
	}
	
	public CharSequence getAboutMessage() { 
		String text = String.format(
				ResourceHelper.getResources().getString(R.string.about_message), 
				ResourceHelper.getResources().getString(R.string.app_versionname)); 
		
		return Html.fromHtml(text);
	}
	
	@Override
	public Drawable getSectionNavIcon(Object data) {
		if (data == null) return null;
		
		if (data instanceof ILibraryData) {
			ILibraryData item = (ILibraryData)data;
			String mimetype = item.getType();
			
			int iconRes = R.drawable.ic_nav_library;
			if (mimetype != null) {
				if (mimetype.indexOf("image") >= 0)
					iconRes = R.drawable.ic_nav_picture;
				else if (mimetype.indexOf("audio") >= 0)
					iconRes = R.drawable.ic_nav_music;
				else if (mimetype.indexOf("video") >= 0)
					iconRes = R.drawable.ic_nav_film;
			}
			
			return getResources().getDrawable(iconRes);
		}
		
		String mimetype = null;
		boolean isfolder = false;
		
		if (data instanceof ISectionSearch) {
			ISectionSearch item = (ISectionSearch)data;
			mimetype = item.getType();
			isfolder = true;
			
		} else if (data instanceof ISectionData) {
			ISectionData item = (ISectionData)data;
			mimetype = item.getType();
			isfolder = item.isFolder();
			
		} else if (data instanceof LocalFolderItem) {
			return getResources().getDrawable(R.drawable.ic_nav_folder);
			
		} else if (data instanceof HostListClusterItem) {
			return getResources().getDrawable(R.drawable.ic_nav_host);
		}
		
		int iconRes = isfolder ? R.drawable.ic_nav_folder : R.drawable.ic_nav_file;
		if (mimetype != null) {
			if (isfolder) {
				if (mimetype.indexOf("/x-recycle") >= 0)
					iconRes = R.drawable.ic_nav_folder_recycle;
				else if (mimetype.indexOf("/x-share") >= 0)
					iconRes = R.drawable.ic_nav_folder_share;
				else if (mimetype.indexOf("/x-upload") >= 0)
					iconRes = R.drawable.ic_nav_folder_upload;
				else if (mimetype.indexOf("/x-search") >= 0)
					iconRes = R.drawable.ic_nav_search;
			} else {
				if (mimetype.startsWith("text/"))
					iconRes = R.drawable.ic_nav_file_text;
			}
		}
		
		return getResources().getDrawable(iconRes);
	}
	
	@Override
	public Drawable getSectionDialogIcon(Object data) {
		if (data == null) return null;
		
		if (data instanceof ILibraryData) {
			ILibraryData item = (ILibraryData)data;
			String mimetype = item.getType();
			
			int iconRes = R.drawable.ic_nav_library_white;
			if (mimetype != null) {
				if (mimetype.indexOf("image") >= 0)
					iconRes = R.drawable.ic_nav_picture_white;
				else if (mimetype.indexOf("audio") >= 0)
					iconRes = R.drawable.ic_nav_music_white;
				else if (mimetype.indexOf("video") >= 0)
					iconRes = R.drawable.ic_nav_film_white;
			}
			
			return getResources().getDrawable(iconRes);
		}
		
		String mimetype = null;
		boolean isfolder = false;
		
		if (data instanceof ISectionSearch) {
			ISectionSearch item = (ISectionSearch)data;
			mimetype = item.getType();
			isfolder = true;
			
		} else if (data instanceof ISectionData) {
			ISectionData item = (ISectionData)data;
			mimetype = item.getType();
			isfolder = item.isFolder();
			
		} else if (data instanceof LocalFolderItem) {
			return getResources().getDrawable(R.drawable.ic_nav_folder_white);
			
		} else if (data instanceof HostListClusterItem) {
			return getResources().getDrawable(R.drawable.ic_nav_host_white);
		}
		
		int iconRes = isfolder ? R.drawable.ic_nav_folder_white : R.drawable.ic_nav_file_white;
		if (mimetype != null) {
			if (isfolder) {
				if (mimetype.indexOf("/x-recycle") >= 0)
					iconRes = R.drawable.ic_nav_folder_recycle_white;
				else if (mimetype.indexOf("/x-share") >= 0)
					iconRes = R.drawable.ic_nav_folder_share_white;
				else if (mimetype.indexOf("/x-upload") >= 0)
					iconRes = R.drawable.ic_nav_folder_upload_white;
				else if (mimetype.indexOf("/x-search") >= 0)
					iconRes = R.drawable.ic_nav_search_white;
			} else {
				if (mimetype.startsWith("text/"))
					iconRes = R.drawable.ic_nav_file_text_white;
			}
		}
		
		return getResources().getDrawable(iconRes);
	}
	
	@Override
	public Drawable getSectionBigIcon(Object data) { 
		if (data == null) return null;
		
		if (data instanceof ILibraryData) {
			ILibraryData item = (ILibraryData)data;
			String mimetype = item.getType();
			
			int iconRes = R.drawable.ic_big_library;
			if (mimetype != null) {
				if (mimetype.indexOf("image") >= 0)
					iconRes = R.drawable.ic_big_picture;
				else if (mimetype.indexOf("audio") >= 0)
					iconRes = R.drawable.ic_big_music;
				else if (mimetype.indexOf("video") >= 0)
					iconRes = R.drawable.ic_big_film;
			}
			
			return getResources().getDrawable(iconRes);
		}
		
		String mimetype = null;
		boolean isfolder = false;
		
		if (data instanceof ISectionSearch) {
			ISectionSearch item = (ISectionSearch)data;
			mimetype = item.getType();
			isfolder = true;
			
		} else if (data instanceof ISectionData) {
			ISectionData item = (ISectionData)data;
			mimetype = item.getType();
			isfolder = item.isFolder();
		}
		
		int iconRes = isfolder ? R.drawable.ic_big_folder : R.drawable.ic_big_file;
		if (mimetype != null) {
			if (isfolder) {
				if (mimetype.indexOf("/x-recycle") >= 0)
					iconRes = R.drawable.ic_big_folder_recycle;
				else if (mimetype.indexOf("/x-share") >= 0)
					iconRes = R.drawable.ic_big_folder_share;
				else if (mimetype.indexOf("/x-upload") >= 0)
					iconRes = R.drawable.ic_big_folder_upload;
			} else {
				if (mimetype.startsWith("text/"))
					iconRes = R.drawable.ic_big_file_text;
				else if (mimetype.startsWith("image/"))
					iconRes = R.drawable.ic_big_picture;
				else if (mimetype.startsWith("audio/"))
					iconRes = R.drawable.ic_big_music;
				else if (mimetype.startsWith("video/"))
					iconRes = R.drawable.ic_big_film;
			}
		}
		
		return getResources().getDrawable(iconRes);
	}
	
	@Override
	public String getShareInformation(Fieldable data) { 
		String info = ResourceHelper.getResources().getString(R.string.share_information); 
		return InformationOperation.formatShareText(data, info);
	}
	
	//@Override
	//public Intent createSettingIntent(String screenKey) { 
	//	return SettingActivity.createIntent(screenKey); 
	//}
	
	@Override
	public boolean handleActionError(Activity activity, 
			ActionError error, Object data) {
		if (activity != null && error != null) {
			String message = error.getMessage();
			if (message != null) {
				message = message.toLowerCase();
				if (message.indexOf("unauthorized") >= 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("handleActionError: unauthorized, activity=" + activity 
								+ " error=" + error);
					}
					
					MyApp.getInstance().getAccountApp().resetAccounts();
					startLoginActivity(activity, null, null);
					return true;
				} else if (message.indexOf("connecttimeout") >= 0 || 
						message.indexOf("sockettimeout") >= 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("handleActionError: connect timeout, activity=" + activity 
								+ " error=" + error);
					}
					
					MyApp.getInstance().getAccountApp().resetAccounts();
					startLoginActivity(activity, null, null);
					return true;
				} else if (message.indexOf("connectexception") >= 0) {
					
				}
			}
		}
		return super.handleActionError(activity, error, data);
	}
	
	@Override
	public void startLoginActivity(Activity from, AccountApp.LoginAction action, 
			String accountEmail) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("startLoginActivity: from=" + from + " action=" + action 
					+ " account=" + accountEmail);
		}
		
		if (from != null) {
			try {
				from.finishAffinity();
			} catch (Throwable e) {
				if (LOG.isWarnEnabled()) 
					LOG.warn("startLoginActivity: error: " + e, e);
			}
		}
		
		String accountAction = null;
		if (action == AccountApp.LoginAction.ADD_ACCOUNT)
			accountAction = RegisterActivity.ACTION_ADD_ACCOUNT;
		else if (action == AccountApp.LoginAction.SELECT_ACCOUNT)
			accountAction = RegisterActivity.ACTION_SELECT_ACCOUNT;
		else if (action == AccountApp.LoginAction.SWITCH_ACCOUNT)
			accountAction = RegisterActivity.ACTION_SWITCH_ACCOUNT;
		
		RegisterActivity.actionLogin(accountAction, accountEmail);
	}
	
	@Override
	public InformationBinderFactory createInformationBinderFactory() {
		return new InformationBinderFactory() {
				public ViewType getViewType() { return sViewType; }
				public IMenuOperation createDefaultListOperation() { return null; }
				public IMenuOperation createPhotoListOperation() { return null; }
				
				@Override
				public boolean onInformationClick(Activity from, InformationOne one) { 
					String location = InformationOperation.setInformationClick(one);
					if (location != null && location.length() > 0) {
						//ReaderActivity.actionActivity(from, location, one.getTitle());
						//return true;
					}
					return false;
				}
				
				@Override
				public boolean onInformationImageClick(Activity from, InformationOne one) { 
					PhotoShowActivity.actionShow(from, PhotoHelper.newPhotoList(
							one.getAllImageList(), one.getFirstImage(true)));
					return true;
				}
			};
	}
	
	static void startNetworkSetting(Context from) {
		final Intent intent;
		if (android.os.Build.VERSION.SDK_INT > 10) {
		    intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
		} else {
		    intent = new Intent();
		    ComponentName component = new ComponentName("com.android.settings","com.android.settings.WirelessSettings");
		    intent.setComponent(component);
		    intent.setAction("android.intent.action.VIEW");
		}
		from.startActivity(intent);
	}
	
	static Intent createIntentForMetricsNotification(Context context) {
		//return new Intent(Settings.ACTION_SETTINGS);
		return new Intent(context, RegisterActivity.class);
	}
	
	static void setDownloadSources() { 
		SourceHelper.addSource("anybox.org", R.drawable.ic_nav_anybox);
		SourceHelper.addSource("javenstudio.org", R.drawable.ic_nav_anybox);
	}
	
	public static final ViewType sViewType = new ViewType();
	public static final SortType sSortType = new SortType();
	public static final FilterType sFilterType = new FilterType();
	
}
