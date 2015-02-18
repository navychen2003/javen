package org.javenstudio.cocoka.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;

public abstract class BaseResources {

	private static BaseResources sInstance = null; 
	private static String sInstanceClassName = null; //AppResources.class.getName(); 
	
	public static synchronized BaseResources getInstance() { 
		if (sInstance == null) { 
			sInstance = (BaseResources)BaseResources.newInstance(
					ResourceHelper.getContext(), sInstanceClassName); 
		}
		return sInstance; 
	}
	
	public static synchronized void setImplementClassName(String className) { 
		if (sInstanceClassName != null) 
			throw new RuntimeException("Instance class name already set");
		
		if (className != null && className.length() > 0) 
			sInstanceClassName = className; 
	}
	
	public static BaseResources newInstance(Context context, String className) { 
		// Pull in the actual implementation of the TalkConfiguration at run-time
        try {
            Class<?> clazz = Class.forName(className);
            BaseResources instance = (BaseResources)clazz.newInstance();
            if (instance != null) { 
            	instance.initialize(context); 
            	return instance; 
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(
            		className + " could not be loaded", ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        }
        
        throw new RuntimeException(className + " could not be instantiated");
	}
	
	private Context mContext = null;
	
	protected void initialize(Context context) { 
		mContext = context;
	}
	
	public final Context getContext() { 
		if (mContext == null) 
			throw new NullPointerException("resource not initialize");
		return mContext;
	}
	
	public final Resources getResources() {
		return getContext().getResources();
	}
	
	public abstract AlertDialog.Builder createDialogBuilder(Activity activity);
	
	public int getDrawableRes(int id) { return 0; }
	public int getColorRes(int id) { return 0; }
	public int getStringRes(int id) { return 0; }
	public int getLayoutRes(int id) { return 0; }
	
	public CharSequence getStringText(int id) { return null; }
	public View findViewById(View view, int layoutId, int id) { return null; }
	
	public static final class drawable {
		public static final int panel_undo_background = 101;
		public static final int icon_menu_undo = 102;
		public static final int icon_menu_share = 103;
		public static final int icon_menu_download = 104;
		public static final int icon_menu_delete = 105;
		public static final int icon_control_play = 106;
		public static final int overscroll_edge = 150;
		public static final int overscroll_glow = 151;
		//public static final int event_background = 152;
		public static final int ptr_flip_spinner = 153;
		public static final int ptr_rotate_spinner = 154;
		public static final int photo_action_background = 155;
		public static final int photo_action_selected_background = 156;
		public static final int photo_controls_background = 157;
	}
	
	public static final class string {
		public static final int ptr_pull_label = 101;
		public static final int ptr_refreshing_label = 102;
		public static final int ptr_release_label = 103;
		public static final int ptr_from_bottom_pull_label = 104;
		public static final int ptr_from_bottom_refreshing_label = 105;
		public static final int ptr_from_bottom_release_label = 106;
	}
	
	public static final class layout {
		public static final int ptr_header_horizontal = 101;
		public static final int ptr_header_vertical = 102;
	}
	
	public static final class view {
		public static final int ptr_header_inner = 101;
		public static final int ptr_header_text = 102;
		public static final int ptr_header_progress = 103;
		public static final int ptr_header_sub_text = 104;
		public static final int ptr_header_image = 105;
	}
	
}
