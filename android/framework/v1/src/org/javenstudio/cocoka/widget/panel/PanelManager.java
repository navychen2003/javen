package org.javenstudio.cocoka.widget.panel;

import java.util.ArrayList;
import java.util.List;

import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.ImageButton;
import org.javenstudio.cocoka.widget.ResultCallback;
import org.javenstudio.cocoka.widget.ToolBar;

public abstract class PanelManager {

	public static interface OnPanelCreateListener {
		public void onCreated(PanelManager manager, PanelConfig config, View view); 
	}
	
	public static interface OnPanelViewAddListener {
		public void onAdded(PanelManager manager, PanelConfig config, View view); 
	}
	
	public static interface OnPanelViewRemoveListener {
		public void onRemoved(PanelManager manager, PanelConfig config, View view); 
	}
	
	public static interface OnPanelShownListener {
		public void onShown(PanelManager manager, PanelConfig config, View view); 
	}
	
	public static interface OnPanelHiddenListener {
		public void onHidden(PanelManager manager, PanelConfig config, View view); 
	}
	
	public static interface OnPanelDestroyListener {
		public void onDestroy(PanelManager manager, PanelConfig config, View view); 
	}
	
	public static interface OnBackPressedListener {
		public void onBackPressed(PanelManager manager, PanelConfig config); 
	}
	
	public static interface PanelAction extends ResultCallback {
		public boolean dispatchIconClick(PanelConfig conf); 
		public void onIconClicked(PanelConfig conf); 
	}
	
	public static class PanelConfig {
		public int mIconResource = 0; 
		public int mTitleResource = 0; 
		public int mViewResource = 0; 
		public PanelAction mAction = null; 
		public OnPanelCreateListener mCreateListener = null; 
		public OnPanelViewAddListener mAddListener = null; 
		public OnPanelViewRemoveListener mRemoveListener = null; 
		public OnPanelShownListener mShownListener = null; 
		public OnPanelHiddenListener mHiddenListener = null; 
		public OnPanelDestroyListener mDestroyListener = null; 
		public OnBackPressedListener mBackPressedListener = null; 
		public View.OnClickListener mClickListener = null; 
		public boolean mCheckable = true; 
		public boolean mDefault = false; 
		public boolean mShowing = false; 
		public ImageButton mButton = null; 
	}
	
	public static class BaseController {
		public int getIconResource(int defaultResource) {
			return defaultResource; 
		}
		
		public int getViewResource(int defaultResource) {
			return defaultResource; 
		}
		
		public int getTitleResource(int defaultResource) {
			return defaultResource; 
		}
	}
	
	protected PanelController mController; 
	protected List<PanelConfig> mPanelConfigs = new ArrayList<PanelConfig>(); 
	
	public PanelManager(PanelController controller) {
		mController = controller; 
		registerPanels(); 
	}
	
	public PanelController getController() {
		return mController; 
	}
	
	protected abstract void registerPanels(); 
	protected abstract int getButtonResource(int count, int position); 
	protected abstract int getButtonBackgroundResource(int count, int position); 
	
	public void onBackPressed() {
		for (PanelConfig conf : mPanelConfigs) {
			if (conf.mBackPressedListener != null) 
				conf.mBackPressedListener.onBackPressed(this, conf); 
		}
	}
	
	public boolean registerPanel(PanelConfig config) {
		if (config == null || config.mIconResource == 0) 
			return false; 
		
		for (PanelConfig conf : mPanelConfigs) {
			if (conf.mIconResource == config.mIconResource) 
				return false; 
		}
		
		mPanelConfigs.add(config); 
		
		return true; 
	}
	
	@SuppressWarnings({"unused"})
	public void initPanels(final PanelLayout panel, final ToolBar toolbar) {
		if (toolbar == null || panel == null) 
			return; 
		
		PanelConfig defaultPanel = null; 
		
		int count = mPanelConfigs.size() + 1; 
		int position = 0; 
		
		for (final PanelConfig conf : mPanelConfigs) {
			final ImageButton button = toolbar.addImageButton(
					getButtonResource(count, position), conf.mIconResource);
			
			button.setBackgroundDrawable(ResourceHelper.getResourceContext().getDrawable(
					getButtonBackgroundResource(count, position))); 
			button.setCheckable(conf.mCheckable); 
			conf.mButton = button; 
			
			if (conf.mClickListener == null) {
				conf.mClickListener = new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (conf.mAction != null && conf.mAction.dispatchIconClick(conf)) 
							return; 
						if (conf.mCheckable) 
							toolbar.setChildChecked(button); 
						if (conf.mViewResource != 0) {
							View view = panel.setPanelView(conf); 
							for (final PanelConfig cf : mPanelConfigs) {
								cf.mShowing = false; 
							}
							conf.mShowing = true; 
						}
						if (conf.mAction != null) 
							conf.mAction.onIconClicked(conf); 
					}
				}; 
			}
			
			button.setOnClickListener(conf.mClickListener);
			
			if (conf.mDefault) 
				defaultPanel = conf; 
			
			position ++; 
		}
		
		// last empty panel button
		if (position == count - 1) {
			ImageButton btn = toolbar.addImageButton(getButtonResource(count, position));
			btn.setBackgroundDrawable(ResourceHelper.getResourceContext().getDrawable(
					getButtonBackgroundResource(count, position))); 
		}
		
		if (defaultPanel != null) {
			final PanelConfig conf = defaultPanel; 
			final ImageButton button = defaultPanel.mButton; 
			
			if (conf.mClickListener != null) 
				conf.mClickListener.onClick(button); 
		}
	}
	
}
