package org.javenstudio.android.setting;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.SimpleActivity;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.cocoka.widget.setting.ActionSetting;
import org.javenstudio.cocoka.widget.setting.CheckBoxSetting;
import org.javenstudio.cocoka.widget.setting.EditTextSetting;
import org.javenstudio.cocoka.widget.setting.ListItemSetting;
import org.javenstudio.cocoka.widget.setting.ListSetting;
import org.javenstudio.cocoka.widget.setting.OnSettingIntentClickListener;
import org.javenstudio.cocoka.widget.setting.Setting;
import org.javenstudio.cocoka.widget.setting.SettingCategory;
import org.javenstudio.cocoka.widget.setting.SettingGroup;
import org.javenstudio.cocoka.widget.setting.SettingManager;
import org.javenstudio.cocoka.widget.setting.SettingScreen;
import org.javenstudio.common.util.Logger;

public abstract class SettingActivityImpl extends SimpleActivity 
		implements SettingScreen.LayoutBinder, OnSettingIntentClickListener {
	private static final Logger LOG = Logger.getLogger(SettingActivityImpl.class);
	
	public static final String SETTINGS_TAG = "cocoka:settings";
	public static final String EXTRA_SCREENKEY = "org.javenstudio.screenkey";
	
	public static final int FLAG_NORMAL = 0; 
	public static final int FLAG_CHANGED = 1; 
	public static final int FLAG_REQUIRED = 2; 
	
	private Bundle mSavedInstanceState = null;
	private SettingManager mSettingManager = null;
	
	@Override
	public void hideProgressView() { 
		super.hideProgressView();
	}
	
	@Override
	protected boolean isLockOrientationDisabled(int orientation) { 
		lockScreenOrientation();
		return true; 
	}
	
	@Override
	protected boolean isUnlockOrientationDisabled() { 
		lockScreenOrientation();
		return true; 
	}
	
	public void lockScreenOrientation() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	@Override
	public void setContentFragment() { 
		if (LOG.isDebugEnabled()) LOG.debug("setContentFragment");
		setContentFragment(new SettingFragment());
	}
	
    public static class SettingFragment extends Fragment {
    	@Override
    	public View onCreateView(LayoutInflater inflater, ViewGroup container,
    			Bundle savedInstanceState) {
    		final Activity activity = getActivity();
    		if (activity instanceof SettingActivityImpl) { 
    			final SettingActivityImpl settingActivity = (SettingActivityImpl)activity;
	    		final View view = settingActivity.onCreateContentView(inflater, 
	    				container, savedInstanceState);
	    		if (view != null) return view;
    		}
    		return new LinearLayout(getActivity().getApplicationContext());
    	}
    }
	
	@Override
	public boolean onActionHome() { 
		if (getActivityHelper().onActionHome()) 
			return true;
		
		if (super.onActionHome()) 
			return true;
		
		onBackPressed(); 
		return true;
	}
	
	@Override
	public boolean onActionRefresh() { 
		setContentFragment();
		return true; 
	}
	
	@Override
	protected boolean onFlingToRight() { 
		onBackPressed(); 
		return true; 
	}
	
	@Override
    protected void doOnCreate(Bundle savedInstanceState) {
		getController().initialize(getCallback()); 
        super.doOnCreate(savedInstanceState);
        lockScreenOrientation();
        
        getSettingManager();
        initSettingActionBar();
	}
	
	protected void initSettingActionBar() {
        String screenKey = getIntentSettingScreenKey();
		if (screenKey != null && screenKey.length() > 0) { 
			SettingScreen screen = getCurrentSettingScreen();
			if (screen != null) {
				setTitle(screen.getTitle());
				
				Drawable icon = screen.getHomeIcon();
		        int iconRes = screen.getHomeIconRes();
		        if (icon != null) setActionBarIcon(icon);
		        else if (iconRes != 0) setActionBarIcon(iconRes);
			}
		}
    }
	
	@Override
	protected void onResume() {
		if (LOG.isDebugEnabled()) LOG.debug("onResume");
        super.onResume();
        bindSettings();
	}
	
    @Override
    protected void onStop() {
        super.onStop();
        mSettingManager.dispatchActivityStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSettingManager.dispatchActivityDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        final SettingScreen settingScreen = getCurrentSettingScreen();
        if (settingScreen != null) {
            Bundle container = new Bundle();
            settingScreen.saveHierarchyState(container);
            outState.putBundle(SETTINGS_TAG, container);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        Bundle container = state.getBundle(SETTINGS_TAG);
        if (container != null) {
            final SettingScreen settingScreen = getCurrentSettingScreen();
            if (settingScreen != null) {
                settingScreen.restoreHierarchyState(container);
                mSavedInstanceState = state;
                return;
            }
        }

        // Only call this if we didn't save the instance state for later.
        // If we did save it, it will be restored when we bind the adapter.
        super.onRestoreInstanceState(state);
    }
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mSettingManager.dispatchActivityResult(requestCode, resultCode, data);
    }
	
	@Override
    public void onContentChanged() {
		if (LOG.isDebugEnabled()) LOG.debug("onContentChanged");
        super.onContentChanged();
        bindSettings();
    }
	
	@Override
	public boolean onSettingIntentClick(SettingScreen settingScreen,
			Setting setting, Intent intent) {
		if (settingScreen != null && intent != null) { 
			startActivity(intent); 
			overridePendingTransition(R.anim.activity_right_enter, R.anim.activity_left_exit); 
			
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void overridePendingTransition() { 
		String screenKey = getIntentSettingScreenKey();
		if (screenKey != null && screenKey.length() > 0) { 
			overridePendingTransition(R.anim.setting_left_enter, R.anim.activity_right_exit); 
			return;
		}
		super.overridePendingTransition();
	}
	
	protected String getIntentSettingScreenKey() {
		return getIntent().getStringExtra(EXTRA_SCREENKEY);
	}
	
	protected String getSettingsTag() { return SETTINGS_TAG; }
	
	protected SettingManager createSettingManager() {
		return new SettingManager(getApplicationContext());
	}
	
	public final synchronized SettingManager getSettingManager() { 
		if (mSettingManager == null) { 
			SettingManager settingManager = createSettingManager();
			if (LOG.isDebugEnabled())
				LOG.debug("getSettingManager: created manager: " + settingManager);
			
			mSettingManager = settingManager; 
		}
		return mSettingManager; 
	}
	
	private void requireSettingManager() {
        if (mSettingManager == null) 
            throw new RuntimeException("This should be called after super.onCreate.");
    }
	
	public void addSettingFromResource(int settingResId) {
		requireSettingManager();
        
        setRootSettingScreen(mSettingManager.inflateFromResource(settingResId,
                getCurrentSettingScreen()));
    }
	
	public abstract SettingScreen getCurrentSettingScreen(); // {
	//	requireSettingManager();
    //    return mSettingManager.getSettingScreen();
    //}
	
	public void setRootSettingScreen(SettingScreen settingScreen) {
		requireSettingManager();
		
		if (mSettingManager.setSettings(settingScreen)) 
            bindSettings();
	}
	
	public final SettingScreen getRootSettingScreen() { 
		requireSettingManager();
        return mSettingManager.getSettingScreen();
	}
	
    private final void bindSettings() {
    	if (LOG.isDebugEnabled()) LOG.debug("bindSettings");
    	
        final SettingScreen settingScreen = getCurrentSettingScreen();
        if (settingScreen != null) {
        	settingScreen.setLayoutBinder(this);
        	settingScreen.init(this);
        	
        	setContentFragment();
        	
            if (mSavedInstanceState != null) {
                super.onRestoreInstanceState(mSavedInstanceState);
                mSavedInstanceState = null;
            }
        } else { 
        	setContentFragment();
        }
    }
	
    private final View onCreateContentView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
    	final SettingScreen settingScreen = getCurrentSettingScreen();
        if (settingScreen != null) {
	    	final View view = getSettingScreenView(inflater, settingScreen);
	    	
	    	SettingScreen.OnBindListener listener = settingScreen.getOnBindListener(); 
	    	if (listener != null)
	    		listener.onBindScreenViews(settingScreen, view); 
	    	
	    	return view;
        }
        
        return null;
    }
    
    protected View getSettingScreenView(LayoutInflater inflater, SettingScreen screen) { 
    	if (inflater == null || screen == null) return null;
    	if (LOG.isDebugEnabled())
    		LOG.debug("getSettingScreenView: screen=" + screen);
    	
    	final View view = inflater.inflate(R.layout.setting_list, null);
    	final ListView listView = (ListView)view.findViewById(R.id.setting_list_listview);
    	
    	listView.setOnItemClickListener(screen);
    	listView.setAdapter(screen.getAdapter());
    	RefreshListView.disableOverscrollGlowEdge(listView);
    	
    	return view;
    }
    
    @Override
	public Dialog createSettingScreenDialog(SettingScreen screen) { 
		//Dialog dialog = new Dialog(this, R.style.NoContentOverlay);
		//dialog.getWindow().setWindowAnimations(R.style.PopupRightAnimation);
    	return null;
    }
	
    @Override
	public int getSettingItemResource(Setting setting) { 
		if (setting == null) return 0; 
		
		Setting.ViewBinder binder = setting.getViewBinder(); 
		if (binder != null) { 
			int resource = binder.getViewResource(); 
			if (resource != 0) 
				return resource; 
		}
		
		if (setting instanceof SettingCategory) 
			return R.layout.setting_category; 
		
		if (setting instanceof EditTextSetting) 
			return R.layout.setting_child_edittext; 
		
		if (setting instanceof ActionSetting) 
			return R.layout.setting_child_action; 
		
		if (setting instanceof PluginInfo)
			return R.layout.setting_child_plugininfo;
		
		if (setting instanceof AccountItemSetting)
			return R.layout.setting_accountitem;
		
		return R.layout.setting_child; 
    }
    
    @Override
	public void bindSettingItemView(Setting setting, View view) { 
		if (setting == null || view == null) 
			return; 
		
		if (setting instanceof ListSetting) { 
			bindListSettingView((ListSetting)setting, view); 
			
		} else if (setting instanceof ListItemSetting) { 
			bindListItemSettingView((ListItemSetting)setting, view); 
			
		} else if (setting instanceof SettingCategory) { 
			bindSettingCategoryView((SettingCategory)setting, view); 
			
		} else if (setting instanceof CheckBoxSetting) { 
			bindCheckBoxSettingView((CheckBoxSetting)setting, view); 
			
		} else if (setting instanceof EditTextSetting) { 
			bindEditTextSettingView((EditTextSetting)setting, view); 
			
		} else if (setting instanceof ActionSetting) { 
			bindActionSettingView((ActionSetting)setting, view); 
			
		} else if (setting instanceof PluginInfo) {
			bindPluginInfoSettingView((PluginInfo)setting, view);
			
		} else if (setting instanceof AccountItemSetting) {
			bindAccountItemSettingView((AccountItemSetting)setting, view);
			
		} else 
			bindSettingItemViewDefault(setting, view, true); 
    }
    
	protected void bindSettingItemViews(Setting setting, View view, boolean selector) { 
		if (setting == null || view == null) 
			return;
		
		ImageView imageView = (ImageView)view.findViewById(R.id.setting_child_image); 
		TextView titleView = (TextView)view.findViewById(R.id.setting_child_title); 
		TextView subtitleView = (TextView)view.findViewById(R.id.setting_child_subtitle); 
		TextView summaryView = (TextView)view.findViewById(R.id.setting_child_summary); 
		TextView rightText = (TextView)view.findViewById(R.id.setting_child_righttext); 
		ImageView rightImage = (ImageView)view.findViewById(R.id.setting_child_rightimage); 
		View rightLayout = view.findViewById(R.id.setting_child_rightlayout); 
		
		if (imageView != null) { 
			Drawable icon = setting.getIcon(); 
			if (icon != null) { 
				imageView.setImageDrawable(icon); 
				imageView.setVisibility(View.VISIBLE); 
			} else 
				imageView.setVisibility(View.GONE); 
		}
		
		if (titleView != null) { 
			if (selector) {
				int colorsRes = AppResources.getInstance().getColorStateListRes(AppResources.color.setting_title_color);
				if (colorsRes != 0) titleView.setTextColor(getResources().getColorStateList(colorsRes));
			} else {
				int colorRes = AppResources.getInstance().getColorRes(AppResources.color.setting_title_color);
				if (colorRes != 0) titleView.setTextColor(getResources().getColor(colorRes));
			}
			
			titleView.setText(setting.getTitle()); 
			titleView.setEnabled(setting.isEnabled());
			
			switch (setting.getFlag()) { 
			case FLAG_CHANGED: 
				titleView.setTextColor(Color.BLUE);
				break;
			case FLAG_REQUIRED: 
				titleView.setTextColor(Color.RED);
				break;
			}
		}
		
		if (subtitleView != null) { 
			subtitleView.setText(setting.getSubTitle()); 
			subtitleView.setEnabled(setting.isEnabled());
		}
		
		if (summaryView != null) { 
			int linkcolorRes = AppResources.getInstance().getColorRes(AppResources.color.setting_summary_link_color);
			if (linkcolorRes != 0) summaryView.setLinkTextColor(getResources().getColor(linkcolorRes));
			summaryView.setText(setting.getSummary()); 
			summaryView.setEnabled(setting.isEnabled());
		}
		
		if (rightLayout != null) { 
			CharSequence message = setting.getMessage(); 
			if (message != null && message.length() > 0) { 
				if (rightText != null) { 
					rightText.setVisibility(View.VISIBLE); 
					rightText.setText(message); 
				}
				if (rightImage != null) 
					rightImage.setVisibility(View.GONE); 
				
				rightLayout.setBackgroundResource(AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_message_new));
				rightLayout.setVisibility(View.VISIBLE); 
				rightLayout.setEnabled(setting.isEnabled());
				
			} else { 
				if (rightText != null) 
					rightText.setVisibility(View.GONE); 
				if (rightImage != null) { 
					if (setting.getIntent() != null || setting.getOnSettingClickListener() != null) {
						rightImage.setImageResource(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_arrow));
						rightText.setVisibility(View.VISIBLE); 
					} else { 
						rightImage.setVisibility(View.GONE); 
					}
				}
				
				if (setting.isEnabled() || setting.isSelectable()) {
					rightLayout.setVisibility(View.VISIBLE); 
					rightLayout.setEnabled(setting.isEnabled());
				} else 
					rightLayout.setVisibility(View.INVISIBLE); 
			}
		}
	}
	
	protected void bindSettingItemBackgroundSelector(Setting setting, View view) { 
		if (setting == null || view == null) 
			return;
		
		int backgroundResource = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_single_selector); 
		
		SettingGroup group = setting.getParent(); 
		if (group != null) { 
			int count = group.getSettingDisplayCount(); 
			int index = group.getSettingDisplayIndex(setting); 
			
			if (count > 1) { 
				if (index == 0) 
					backgroundResource = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_first_selector); 
				else if (index == count - 1) 
					backgroundResource = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_last_selector); 
				else 
					backgroundResource = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_middle_selector); 
			}
		}
		
		view.setBackgroundResource(backgroundResource); 
	}
	
	protected void bindSettingItemBackgroundNormal(Setting setting, View view) { 
		if (setting == null || view == null) 
			return;
		
		int backgroundResource = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_single_normal); 
		
		SettingGroup group = setting.getParent(); 
		if (group != null) { 
			int count = group.getSettingDisplayCount(); 
			int index = group.getSettingDisplayIndex(setting); 
			
			if (count > 1) { 
				if (index == 0) 
					backgroundResource = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_first_normal); 
				else if (index == count - 1) 
					backgroundResource = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_last_normal); 
				else 
					backgroundResource = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_middle_normal); 
			}
		}
		
		view.setBackgroundResource(backgroundResource); 
	}
	
	protected void bindSettingItemViewDefault(Setting setting, View view, boolean selector) { 
		if (setting == null || view == null) 
			return;
		
		Setting.ViewBinder binder = setting.getViewBinder(); 
		if (binder == null || !binder.bindSettingView(setting, view))
			bindSettingItemViews(setting, view, selector); 
		
		if (binder == null || !binder.bindSettingBackground(setting, view)) {
			if (selector) bindSettingItemBackgroundSelector(setting, view); 
			else bindSettingItemBackgroundNormal(setting, view); 
		}
	}
	
	protected void bindActionSettingView(final ActionSetting setting, View view) { 
		if (setting == null || view == null) 
			return;
		
		Setting.ViewBinder binder = setting.getViewBinder(); 
		if (binder == null || !binder.bindSettingView(setting, view)) { 
			TextView titleView = (TextView)view.findViewById(R.id.setting_child_title); 
			if (titleView != null) { 
				titleView.setText(setting.getTitle()); 
				
				titleView.setBackgroundResource(AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_action_selector));
			}
		}
		
		if (binder == null || !binder.bindSettingBackground(setting, view)) { 
			view.setBackgroundResource(0);
		}
	}
	
	protected void bindEditTextSettingView(final EditTextSetting setting, View view) { 
		bindSettingItemViewDefault(setting, view, true); 
		
		if (setting == null || view == null) 
			return;
		
		final EditText editView = (EditText)view.findViewById(R.id.setting_child_edittext); 
		if (editView != null) { 
			int inputType = setting.getInputType(); 
			if (inputType != 0)
				editView.setInputType(inputType); 
			
			setEditTextSettingViewValue(setting, editView);
			editView.setEnabled(setting.isEnabled());
			
			setting.setOnSettingViewBindListener(new Setting.OnSettingViewBindListener() {
					@Override
					public void onBindSettingView(Setting s) {
						setEditTextSettingViewValue(setting, editView);
					}
				});
			
			EditTextSetting.OnTextChangeListener listener = setting.getOnTextChangeListener(); 
			if (listener != null) { 
				editView.addTextChangedListener(listener); 
				
			} else { 
				editView.addTextChangedListener(new TextWatcher() {
						@Override
						public void afterTextChanged(Editable s) {}
		
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
							setting.changeValue(s);
						}
					});
			}
			
			editView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						setting.callFocusChangeListener(hasFocus);
					}
				});
		}
	}
	
	protected void setEditTextSettingViewValue(EditTextSetting setting, EditText editView) { 
		EditTextSetting.EditTextBinder binder = setting.getEditTextBinder(); 
		if (binder == null || !binder.bindEditText(setting, editView)) { 
			editView.setHint(setting.getSummary()); 
			editView.setText(setting.getText()); 
		}
	}
	
	protected void bindCheckBoxSettingView(final CheckBoxSetting setting, View view) { 
		bindSettingItemViewDefault(setting, view, false); 
		
		if (setting == null || view == null) 
			return;
		
		View rightButton = view.findViewById(R.id.setting_child_rightbutton); 
		if (rightButton != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_button_selector);
			if (backgroundRes != 0) rightButton.setBackgroundResource(backgroundRes);
		}
		
		ImageView rightImage = (ImageView)view.findViewById(R.id.setting_child_rightimage); 
		if (rightImage != null) { 
			final int resId; 
			if (setting.isRadio()) { 
				resId = setting.isChecked() ? 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_radiobox_on) : 
							AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_radiobox_off);
			} else { 
				resId = setting.isChecked() ? 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_checkbox_on) : 
							AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_checkbox_off);
			}
			if (resId != 0) rightImage.setImageResource(resId);
			rightImage.setVisibility(View.VISIBLE);
		}
		
		if (view instanceof SettingListItem) { 
			SettingListItem listItem = (SettingListItem)view; 
			
			listItem.setOnButtonClickListener(new SettingListItem.OnButtonClickListener() {
					@Override
					public void onRightButtonClicked(View view) {
						boolean newValue = !setting.isChecked(); 
						setting.changeValue(newValue); 
					}
				});
		}
	}
	
	protected void bindSettingCategoryView(SettingCategory category, View view) { 
		if (category == null || view == null) 
			return;
		
		Setting.ViewBinder binder = category.getViewBinder(); 
		if (binder == null || !binder.bindSettingView(category, view)) { 
			TextView textView = (TextView)view.findViewById(R.id.setting_category_title); 
			if (textView != null) { 
				boolean settedValue = false; 
				CharSequence title = category.getTitle(); 
				Drawable icon = category.getIcon(); 
				
				if (title != null) { 
					textView.setText(title); 
					settedValue = true; 
				}
				
				if (icon != null) { 
					textView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null); 
					settedValue = true; 
				}
				
				textView.setVisibility(settedValue ? View.VISIBLE : View.GONE);
			}
		}
		
		if (binder == null || !binder.bindSettingBackground(category, view)) { 
			// do nothing
		}
	}
	
	protected void bindListSettingView(final ListSetting setting, View view) { 
		bindSettingItemViewDefault(setting, view, true); 
	}
	
	protected void bindListItemSettingView(final ListItemSetting setting, View view) { 
		bindSettingItemViewDefault(setting, view, false); 
		
		if (setting == null || view == null) 
			return;
		
		View rightButton = view.findViewById(R.id.setting_child_rightbutton); 
		if (rightButton != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_setting_button_selector);
			if (backgroundRes != 0) rightButton.setBackgroundResource(backgroundRes);
		}
		
		ImageView rightImage = (ImageView)view.findViewById(R.id.setting_child_rightimage); 
		if (rightImage != null) { 
			if (setting.isChecked()) { 
				rightImage.setImageResource(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_checkin));
				rightImage.setVisibility(View.VISIBLE);
			} else { 
				rightImage.setImageResource(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_setting_checkin));
				rightImage.setVisibility(View.INVISIBLE);
			}
		}
	}
    
	protected void bindPluginInfoSettingView(PluginInfo setting, View view) { 
		if (setting == null || view == null) 
			return;
		
		Setting.ViewBinder binder = setting.getViewBinder(); 
		if (binder == null || !binder.bindSettingView(setting, view))
			bindSettingItemViews(setting, view, true); 
		
		if (binder == null || !binder.bindSettingBackground(setting, view))
			bindSettingItemBackgroundSelector(setting, view); 
	}
	
	protected void bindAccountItemSettingView(AccountItemSetting setting, View view) { 
		if (setting == null || view == null) 
			return;
		
		Setting.ViewBinder binder = setting.getViewBinder(); 
		if (binder == null || !binder.bindSettingView(setting, view))
			bindSettingItemViews(setting, view, true); 
		
		if (binder == null || !binder.bindSettingBackground(setting, view))
			bindSettingItemBackgroundSelector(setting, view); 
	}
	
}
