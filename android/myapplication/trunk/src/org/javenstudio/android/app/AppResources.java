package org.javenstudio.android.app;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.cocoka.app.BaseResources;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.information.InformationBinderFactory;

public abstract class AppResources extends BaseResources {
	//private static final Logger LOG = Logger.getLogger(AppResources.class);

	public static interface Fieldable { 
		public String getField(String name);
	}
	
	public static class Fields implements Fieldable { 
		private final Map<String, String> mFields = new HashMap<String, String>();

		@Override
		public synchronized String getField(String name) {
			return mFields.get(name);
		}
		
		public synchronized void addField(String name, String value) { 
			if (name == null || value == null) return;
			mFields.put(name, value);
		}
	}
	
	public static final AppResources getInstance() { 
		return (AppResources)BaseResources.getInstance(); 
	}
	
	public static final class drawable {
		public static final int icon_menu_search = 1005;
		public static final int icon_menu_camera = 1007;
		public static final int icon_menu_photo = 1008;
		public static final int icon_menu_video = 1009;
		public static final int icon_menu_viewlarge = 1010;
		public static final int icon_menu_viewlist = 1011;
		public static final int icon_menu_viewsmall = 1012;
		public static final int icon_menu_share = 1013;
		public static final int icon_menu_copy = 1014;
		public static final int icon_menu_notification = 1031;
		public static final int icon_menu_notification_enabled = 1032;
		public static final int icon_menu_sort_name_asc = 1036;
		public static final int icon_menu_sort_name_desc = 1037;
		public static final int icon_menu_sort_modified_asc = 1038;
		public static final int icon_menu_sort_modified_desc = 1039;
		public static final int icon_menu_sort_size_asc = 1040;
		public static final int icon_menu_sort_size_desc = 1041;
		public static final int icon_menu_sort_type_asc = 1042;
		public static final int icon_menu_sort_type_desc = 1043;
		public static final int icon_menu_filter_all = 1044;
		public static final int icon_menu_filter_image = 1045;
		public static final int icon_menu_filter_audio = 1046;
		public static final int icon_menu_filter_video = 1047;
		public static final int icon_menu_operation_move = 1048;
		public static final int icon_menu_operation_delete = 1049;
		public static final int icon_menu_operation_rename = 1050;
		public static final int icon_menu_operation_modify = 1051;
		public static final int icon_menu_operation_share = 1052;
		public static final int icon_menu_operation_open = 1053;
		public static final int icon_menu_operation_details = 1054;
		public static final int icon_menu_operation_select = 1055;
		public static final int icon_menu_operation_download = 1056;
		
		public static final int icon_selected_on_large = 1015;
		public static final int icon_selected_off_large = 1016;
		public static final int icon_selected_on_small = 1017;
		public static final int icon_selected_off_small = 1018;
		public static final int icon_setting_radiobox_on = 1022;
		public static final int icon_setting_radiobox_off = 1023;
		public static final int icon_setting_checkbox_on = 1024;
		public static final int icon_setting_checkbox_off = 1025;
		public static final int icon_setting_checkin = 1026;
		public static final int icon_setting_arrow = 1027;
		public static final int icon_setting_general = 1028;
		public static final int icon_setting_account = 1029;
		public static final int icon_setting_about = 1030;
		public static final int icon_setting_task = 1031;
		public static final int icon_setting_plugin = 1032;
		
		public static final int card_image_background = 1901;
		public static final int card_header_background = 1902;
		public static final int card_avatar_selector = 1903;
		public static final int card_avatar_round_selector = 1904;
		public static final int card_list_selector = 1905;
		
		//public static final int icon_check = 1950;
		//public static final int icon_cross = 1951;
		public static final int icon_error = 1952;
		public static final int icon_error_warning = 1953;
		public static final int icon_help = 1954;
		public static final int icon_dialog_warning = 1955;
		public static final int icon_dialog_back_indicator = 1956;
		public static final int icon_homeasup_menu_indicator = 1957;
		public static final int icon_homeasup_back_indicator = 1958;
		public static final int icon_task_cancel = 1959;
		public static final int icon_task_done = 1960;
		
		public static final int bg_message_new = 1079;
		public static final int bg_item_selected = 1080;
		public static final int bg_setting_single_selector = 1081;
		public static final int bg_setting_first_selector = 1082;
		public static final int bg_setting_last_selector = 1083;
		public static final int bg_setting_middle_selector = 1084;
		public static final int bg_setting_single_normal = 1085;
		public static final int bg_setting_first_normal = 1086;
		public static final int bg_setting_last_normal = 1087;
		public static final int bg_setting_middle_normal = 1088;
		public static final int bg_setting_action_selector = 1089;
		public static final int bg_setting_button_selector = 1090;
		public static final int bg_dialog_singlechoice_item = 1091;
		public static final int bg_task_action_selector = 1092;
		public static final int bg_host_action_selector = 1093;
		
		public static final int btn_dialog_checkmark = 1095;
		
		public static final int accountinfo_avatar_round_selector = 1100;
		public static final int accountmenu_avatar_round_selector = 1101;
		public static final int sectioninfo_avatar_round_selector = 1102;
		public static final int accountinfo_background = 1103;
		public static final int accountmenu_background = 1104;
		public static final int accountmenu_header_top_background = 1105;
		public static final int accountmenu_header_bottom_background = 1106;
		//public static final int accountinfo_actionbar_backfround = 1107;
		public static final int accountinfo_overlay = 1108;
		public static final int accountinfo_header_background = 1109;
		public static final int accountinfo_header_actions_background = 1110;
		public static final int accountinfo_above_actions_background = 1111;
		
		public static final int profile_tab_active_item_selector = 1200;
		public static final int profile_tab_inactive_item_selector = 1201;
		public static final int accountinfo_card_background = 1202;
		public static final int sectioninfo_card_background = 1203;
		public static final int notify_avatar_background = 1204;
		public static final int notify_header_background = 1205;
		//public static final int notify_empty_state_image = 1206;
		
		public static final int sectioninfo_image_background = 1302;
		//public static final int dashboard_emptystate_image = 1303;
		//public static final int dashboard_background_image = 1304;
		public static final int setting_avatar_background = 1305;
		public static final int setting_action_background = 1306;
		public static final int setting_account_action_icon_selected = 1307;
		public static final int setting_account_action_icon_remove = 1308;
		public static final int host_action_icon_remove = 1309;
		
		public static final int section_item_background = 1400;
		public static final int section_item_background_selected = 1401;
		public static final int section_poster_background = 1402;
		public static final int section_action_background = 1403;
		public static final int section_list_header_background = 1404;
		public static final int section_list_footer_background = 1405;
		public static final int section_list_category_background = 1406;
		public static final int section_list_shortcut_background = 1407;
		//public static final int section_list_item_background = 1408;
		//public static final int section_empty_folder_image = 1409;
		
		public static final int dashboard_item_header_background = 1500;
		public static final int dashboard_item_body_background = 1501;
		public static final int sectioninfo_header_main_background = 1502;
		public static final int sectioninfo_header_poster_background = 1503;
		public static final int sectioninfo_header_actions_background = 1504;
		public static final int sectioninfo_above_actions_background = 1505;
		public static final int sectioninfo_overlay = 1506;
	}
	
	public static final class color {
		public static final int progress_front_color = 1001;
		public static final int progress_back_color = 1002;
		public static final int card_title_color = 1003;
		//public static final int upload_percent_color = 1004;
		//public static final int download_percent_color = 1005;
		
		public static final int accountinfo_action_textcolor = 1100;
		//public static final int accountinfo_actionbar_title_color = 1101;
		public static final int accountinfo_header_name_color = 1102;
		public static final int accountmenu_item_title_color = 1103;
		public static final int accountmenu_header_title_color = 1104;
		public static final int accountmenu_header_subtitle_color = 1105;
		public static final int accountmenu_footer_title_color = 1106;
		public static final int accountmenu_footer_icon_color = 1107;
		
		public static final int overviewspace_percent_color = 1201;
		public static final int dashboard_item_title_color = 1202;
		public static final int dashboard_user_title_color = 1203;
		public static final int dashboard_image_text_color = 1204;
		//public static final int dashboard_empty_title_color = 1205;
		//public static final int dashboard_empty_subtitle_color = 1206;
		//public static final int dashboard_background_color = 1207;
		public static final int setting_summary_link_color = 1208;
		public static final int setting_title_color = 1209;
		
		public static final int sectioninfo_header_name_color = 1300;
		public static final int sectioninfo_action_textcolor = 1301;
		public static final int sectioninfo_header_image_text_color = 1302;
	}
	
	public static final class string {
		public static final int about_title = 1001;
		public static final int about_message = 1002;
		public static final int dialog_warning_title = 1003;
		public static final int login_signingin_message = 1004;
		public static final int login_signingout_message = 1005;
		
		public static final int accountinfo_title = 1103;
		public static final int accountmenu_title = 1104;
		public static final int accountmenu_subtitle = 1105;
		public static final int accountmenu_footer_title = 1106;
		public static final int accountmenu_secondfooter_title = 1107;
		public static final int accountmenu_secondfooter_loading_title = 1108;
		public static final int accountmenu_logout_confirm_title = 1109;
		public static final int accountmenu_logout_confirm_message = 1110;
		public static final int switchaccount_confirm_title = 1111;
		public static final int switchaccount_confirm_message = 1112;
		public static final int deleteaccount_confirm_title = 1113;
		public static final int deleteaccount_confirm_message = 1114;
		public static final int addaccount_confirm_title = 1115;
		public static final int addaccount_confirm_message = 1116;
		
		public static final int notify_announcement_title = 1200;
		public static final int notify_invite_title = 1201;
		public static final int notify_message_title = 1202;
		public static final int notify_systemalert_title = 1203;
		//public static final int notify_empty_message = 1204;
		public static final int plurals_notify_moremessage_title = 1205;
		public static final int plurals_notify_moreinvite_title = 1206;
		
		public static final int invite_be_friend_message = 1300;
		public static final int invite_not_accepted_message = 1301;
		public static final int dashboard_download_image_label = 1302;
		//public static final int dashboard_empty_title = 1302;
		//public static final int dashboard_empty_message = 1303;
		public static final int sectioninfo_download_image_label = 1304;
		
		public static final int section_file_information_label = 1400;
		public static final int section_folder_information_label = 1401;
		public static final int section_folder_count_information_label = 1402;
		public static final int section_folder_empty_information_label = 1403;
		public static final int section_modified_information_label = 1404;
		public static final int section_search_name_title = 1405;
		public static final int section_search_count_information_label = 1406;
		public static final int section_search_notfound_information_label = 1407;
		public static final int section_sortby_dialog_title = 1408;
		public static final int section_filterby_dialog_title = 1409;
		public static final int section_operation_dialog_title = 1410;
		public static final int section_select_upload_title = 1411;
		public static final int section_select_store_folder_title = 1412;
	}
	
	public static final class style {
		//public static final int dialog_alert_light = 1001;
		//public static final int dialog_alert_dark = 1002;
	}
	
	public static final class dimen {
		public static final int accountmenu_item_title_size = 1001;
	}
	
	public static final class anim {
		public static final int dashboard_item_show_animation = 1001;
		public static final int section_grid_item_show_animation = 1002;
		public static final int section_list_footer_show_animation = 1003;
		public static final int section_list_footer_hide_animation = 1004;
		public static final int section_empty_folder_show_animation = 1005;
	}
	
	public int getDrawableRes(int id) { 
		switch (id) {
		//case BaseResources.drawable.event_background:
		//	return R.drawable.bg_events_on_air;
		case BaseResources.drawable.icon_menu_delete:
			return R.drawable.ic_menu_delete_holo_light;
		case BaseResources.drawable.icon_menu_download:
			return R.drawable.ic_menu_download_holo_light;
		case BaseResources.drawable.icon_menu_share:
			return R.drawable.ic_menu_share_holo_light;
		case drawable.accountinfo_overlay:
			return R.drawable.ic_camera_grey;
		case drawable.sectioninfo_overlay:
			return R.drawable.ic_camera_grey;
		case drawable.icon_dialog_warning:
			return R.drawable.ic_error_white;
		case drawable.icon_error:
			return R.drawable.ic_error_white;
		case drawable.icon_error_warning:
			return R.drawable.ic_error_gold;
		case drawable.icon_help:
			return R.drawable.ic_help_white;
		case drawable.icon_menu_search:
			return R.drawable.ic_menu_search_holo_light;
		case drawable.accountmenu_header_bottom_background:
			return R.drawable.bg_account_menubottom;
		}
		return 0; 
	}
	
	public int getColorRes(int id) { return 0; }
	public int getStyleRes(int id) { return 0; }
	public int getDimenRes(int id) { return 0; }
	public int getAnimRes(int id) { return 0; }
	
	public int getColorStateListRes(int id) { 
		switch (id) {
		case color.accountinfo_action_textcolor:
		case color.sectioninfo_action_textcolor:
			return R.color.accountinfo_action_textcolor;
		}
		return 0; 
	}
	
	public int getStringRes(int id) { 
		switch (id) {
		case string.dialog_warning_title:
			return R.string.dialog_warning_title;
		}
		return 0; 
	}
	
	public ColorStateList getColorStateList(int id) { 
		int res = getColorStateListRes(id);
		if (res != 0) return getContext().getResources().getColorStateList(res);
		return null;
	}
	
	public CharSequence getQuantityStringText(int id, int quantity) { 
		int res = getStringRes(id);
		if (res != 0) return getContext().getResources().getQuantityString(res, quantity);
		return null; 
	}
	
	public String getString(int id) { 
		int res = getStringRes(id);
		if (res != 0) return getContext().getString(res);
		return null; 
	}
	
	public CharSequence getStringText(int id) { 
		int res = getStringRes(id);
		if (res != 0) return getContext().getText(res);
		return null; 
	}
	
	public int getLargeSelectedDrawableRes(boolean selected) { 
		return selected ? getDrawableRes(drawable.icon_selected_on_large) : 
			getDrawableRes(drawable.icon_selected_off_large);
	}
	
	public int getSmallSelectedDrawableRes(boolean selected) { 
		return selected ? getDrawableRes(drawable.icon_selected_on_small) : 
			getDrawableRes(drawable.icon_selected_off_small);
	}
	
	public final String getShareInformation() { 
		return getShareInformation((AppResources.Fieldable)null); 
	}
	
	public String getShareInformation(AppResources.Fieldable data) { 
		return null; 
	}
	
	public void playVideo(Activity activity, Uri uri, String title) { 
		if (activity == null || uri == null) 
			return;
		
		if (title == null) title = "";
		
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/*")
                    .putExtra(Intent.EXTRA_TITLE, title);
            activity.startActivity(intent);
            //        .putExtra(MovieActivity.KEY_TREAT_UP_AS_BACK, true);
            //activity.startActivityForResult(intent, REQUEST_PLAY_VIDEO);
        } catch (Throwable e) {
            //Toast.makeText(activity, activity.getString(R.string.video_err),
            //        Toast.LENGTH_SHORT).show();
        }
	}
	
	public abstract InformationBinderFactory createInformationBinderFactory();
	//public abstract Intent createSettingIntent(String screenKey);
	
	public boolean handleActionError(Activity activity, ActionError error, Object data) {
		return false;
	}
	
	public void startLoginActivity(Activity from, AccountApp.LoginAction action, 
			String accountEmail) {}
	
	@Override
	public final AlertDialogBuilder createDialogBuilder(Activity activity) {
		return new AlertDialogBuilder(activity, getDialogHelper());
	}
	
	public abstract AlertDialogHelper getDialogHelper();
	
	public Drawable getSectionNavIcon(Object data) { return null; }
	public Drawable getSectionBigIcon(Object data) { return null; }
	public Drawable getSectionDialogIcon(Object data) { return null; }
	
	public String formatReadableBytes(long length) {
		return Utilities.formatSize(length);
	}
	
	public String formatDetailsBytes(long length) {
		String text = getResources().getString(R.string.filesize_bytes);
		text = String.format(text, ""+length);
		return Utilities.formatSize(length) + "(" + text + ")";
	}
	
	public String formatReadableTime(long time) {
		return Utilities.formatDate(time);
	}
	
	public String formatRefreshTime(long requestTime) {
		if (requestTime > 0) {
			String text = getResources().getString(R.string.updated_timeago_title);
			String time = formatTimeAgo(System.currentTimeMillis() - requestTime);
			String label = String.format(text, time);
			return label;
		}
		return null;
	}
	
	public String formatDuration(long millis) {
		if (millis < 0) millis = 0;
		
		long seconds = millis / 1000;
	    long minutes = seconds / 60;
	    long hours = minutes / 60;
	    
	    StringBuilder sbuf = new StringBuilder();
	    if (hours > 0) {
	    	if (hours < 10) sbuf.append('0');
	    	sbuf.append(hours);
	    	sbuf.append(':');
	    }
	    
	    seconds = seconds - minutes * 60;
	    minutes = minutes - hours * 60;
	    
	    if (minutes < 10) sbuf.append('0');
	    sbuf.append(minutes);
	    sbuf.append(':');
	    
	    if (seconds < 10) sbuf.append('0');
	    sbuf.append(seconds);
	    
	    return sbuf.toString();
	}
	
	public String formatTimeAgo(long distanceMillis) {
		String prefix = getContext().getString(R.string.timeago_prefixago);
		String suffix = getContext().getString(R.string.timeago_suffixago);
		
		if (distanceMillis < 0) {
			prefix = getContext().getString(R.string.timeago_prefixfromnow);
			suffix = getContext().getString(R.string.timeago_suffixfromnow);
			distanceMillis = Math.abs(distanceMillis);
		}
		
		if (prefix == null) prefix = "";
		if (suffix == null) suffix = "";
		
		long seconds = distanceMillis / 1000;
	    long minutes = seconds / 60;
	    long hours = minutes / 60;
	    long days = hours / 24;
	    long years = days / 365;
		
	    final String text;
	    if (seconds < 45) {
	    	text = substituteTimeAgo(R.string.timeago_seconds, Math.round(seconds));
	    } else if (seconds < 90) {
	    	text = substituteTimeAgo(R.string.timeago_minute, 1);
	    } else if (minutes < 45) {
	    	text = substituteTimeAgoQuantity(R.plurals.plurals_timeago_minutes, Math.round(minutes));
	    } else if (minutes < 90) {
	    	text = substituteTimeAgo(R.string.timeago_hour, 1);
	    } else if (hours < 24) {
	    	text = substituteTimeAgoQuantity(R.plurals.plurals_timeago_hours, Math.round(hours));
	    } else if (hours < 48) {
	    	text = substituteTimeAgo(R.string.timeago_day, 1);
	    } else if (days < 7) {
	    	text = substituteTimeAgoQuantity(R.plurals.plurals_timeago_days, Math.round(days));
	    } else if (days < 8) {
	    	text = substituteTimeAgo(R.string.timeago_week, 1);
	    } else if (days < 14) {
	    	text = substituteTimeAgoQuantity(R.plurals.plurals_timeago_days, Math.round(days));
	    } else if (days < 22) {
	    	text = substituteTimeAgoQuantity(R.plurals.plurals_timeago_weeks, Math.round(days/7));
	    } else if (days < 60) {
	    	text = substituteTimeAgo(R.string.timeago_month, 1);
	    } else if (days < 365) {
	    	text = substituteTimeAgoQuantity(R.plurals.plurals_timeago_months, Math.round(days/30));
	    } else if (years < 2) {
	    	text = substituteTimeAgo(R.string.timeago_year, 1);
	    } else {
	    	text = substituteTimeAgoQuantity(R.plurals.plurals_timeago_years, Math.round(years));
	    }
	    
		return prefix + text + suffix;
	}
	
	protected String substituteTimeAgo(final int resId, int number) {
		String text = getContext().getString(resId);
		String value = Long.toString(number);
		if (number >= 0 && number < sNumberRes.length)
			value = getContext().getString(sNumberRes[(int)number]);
		return String.format(text, value);
	}
	
	protected String substituteTimeAgoQuantity(final int resId, final int number) {
		String text = getContext().getResources().getQuantityString(resId, number);
		String value = Long.toString(number);
		if (number >= 0 && number < sNumberRes.length)
			value = getContext().getString(sNumberRes[(int)number]);
		return String.format(text, value);
	}
	
	private static final int[] sNumberRes = new int[] {
			R.string.timeago_number_0,
			R.string.timeago_number_1,
			R.string.timeago_number_2,
			R.string.timeago_number_3,
			R.string.timeago_number_4,
			R.string.timeago_number_5,
			R.string.timeago_number_6,
			R.string.timeago_number_7,
			R.string.timeago_number_8,
			R.string.timeago_number_9,
			R.string.timeago_number_10
		};
	
}
