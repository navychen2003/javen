package org.javenstudio.provider.library.section;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IRefreshView;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBinderBase;
import org.javenstudio.provider.library.ICategoryData;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.IVisibleData;

public abstract class SectionListBinderBase extends ProviderBinderBase {
	private static final Logger LOG = Logger.getLogger(SectionListBinderBase.class);

	private final SectionListFactory mFactory;
	private final SectionListProvider mProvider;
	private final SectionListDataSets mDataSets;
	
	private GestureDetector mGestureDetector = null;
	private View mHeaderView = null;
	private View mFooterView = null;
	private View mCenterView = null;
	private int mFirstVisibleItem = -1;
	
	public SectionListBinderBase(SectionListProvider provider, 
			SectionListFactory factory) {
		if (provider == null || factory == null) throw new NullPointerException();
		mProvider = provider;
		mFactory = factory;
		mDataSets = factory.createSectionListDataSets(provider);
	}
	
	public final SectionListFactory getFactory() { return mFactory; }
	public final SectionListProvider getProvider() { return mProvider; }
	public final SectionListDataSets getDataSets() { return mDataSets; }
	
	public abstract IActivity getBindedActivity();
	public abstract View getBindedListView();
	public boolean showAboveViews() { return true; }
	
	public void onActionModeViewBinded(SelectMode mode, View view) {
		if (mode == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("onActionModeViewBinded: mode=" + mode + " view=" + view);
		
		hideFooterView(mode.getActionHelper().getIActivity(), false, true);
	}
	
	@Override
	protected final int getFirstVisibleItem(ListAdapter adapter) { 
		return -1;
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, ListAdapter adapter, 
			AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) { 
		super.onFirstVisibleChanged(activity, adapter, view, 
				firstVisibleItem, visibleItemCount, totalItemCount);
		mFirstVisibleItem = firstVisibleItem;
	}
	
	public void setVisibleSelection(IVisibleData data) {
		final View view = getBindedListView();
		if (view == null) return;
		
		ListAdapter adapter = getListViewAdapter(view);
		if (adapter != null && adapter instanceof SectionListAdapter) {
			SectionListAdapter gridAdapter = (SectionListAdapter)adapter;
			int position = gridAdapter.findSectionSelection(data);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("setVisibleSelection: view=" + view + " data=" + data 
						+ " position=" + position);
			}
			
			if (position >= 0 && position < adapter.getCount()) {
				setListViewSelection(view, position);
				setHeaderView(data, position);
			}
		}
	}
	
	public void bindBackgroundView(IActivity activity) {}
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
		if (activity == null || inflater == null) return;
		mGestureDetector = null;
		mHeaderView = null;
		mFooterView = null;
		mCenterView = null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("bindBehindAbove: activity=" + activity);
		
		final View aboveView = activity.getContentAboveView();
		if (aboveView != null && aboveView instanceof ViewGroup) {
			ViewGroup aboveGroup = (ViewGroup)aboveView;
			
			if (showAboveViews()) {
				View view = getAboveView(activity, inflater);
				if (view != null) {
					aboveGroup.removeAllViews();
					aboveGroup.addView(view, new ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, 
							ViewGroup.LayoutParams.MATCH_PARENT));
				}
				
				mGestureDetector = onCreateGestureDetector(activity);
				aboveView.setVisibility(View.VISIBLE);
			} else {
				aboveView.setVisibility(View.GONE);
			}
		}
	}
	
	public int getHeaderViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.section_list_header_background);
	}
	
	public int getFooterViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.section_list_footer_background);
	}
	
	public int getHeaderCategoryViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.section_list_category_background);
	}
	
	public int getFooterShortcutViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.section_list_shortcut_background);
	}
	
	public int getItemViewBackgroundRes(boolean selected) {
		return AppResources.getInstance().getDrawableRes(
				selected ? AppResources.drawable.section_item_background_selected : 
					AppResources.drawable.section_item_background);
	}
	
	public int getItemPosterViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.section_poster_background);
	}
	
	protected View getAboveView(final IActivity activity, LayoutInflater inflater) {
		if (activity == null || inflater == null) return null;
		final View view = inflater.inflate(R.layout.section_list_above, null);
		
		ViewGroup centerView = (ViewGroup)view.findViewById(R.id.section_list_above_center);
		if (centerView != null) centerView.setVisibility(View.GONE);
		mCenterView = centerView;
		
		ViewGroup headerView = (ViewGroup)view.findViewById(R.id.section_list_above_header);
		if (headerView != null) {
			View categoryView = (View)view.findViewById(R.id.section_above_category);
			int categorybgRes = getHeaderCategoryViewBackgroundRes();
			if (categorybgRes != 0 && categoryView != null) 
				categoryView.setBackgroundResource(categorybgRes);
			
			int backgroundRes = getHeaderViewBackgroundRes();
			if (backgroundRes != 0) headerView.setBackgroundResource(backgroundRes);
			
			headerView.setVisibility(View.GONE);
			headerView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onHeaderViewClick(v);
					}
				});
		}
		mHeaderView = headerView;
		
		ViewGroup footerView = (ViewGroup)view.findViewById(R.id.section_list_above_footer);
		if (footerView != null) {
			ViewGroup shortcutView = (ViewGroup)view.findViewById(R.id.section_above_shortcut);
			//int shortcutbgRes = getFooterShortcutViewBackgroundRes();
			//if (shortcutbgRes != 0 && shortcutView != null) 
			//	shortcutView.setBackgroundResource(shortcutbgRes);
			
			if (shortcutView != null) {
				View uploadButton = getShortcutUploadButtonView(activity, inflater, 
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								onUploadButtonClick(activity, v);
							}
						});
				
				View createButton = getShortcutCreateButtonView(activity, inflater, 
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								onCreateButtonClick(activity, v);
							}
						});
				
				View scanButton = getShortcutScanButtonView(activity, inflater, 
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								onScanButtonClick(activity, v);
							}
						});
			
				shortcutView.addView(uploadButton, 
						new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
				shortcutView.addView(createButton, 
						new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
				shortcutView.addView(scanButton, 
						new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
			}
			
			int backgroundRes = getFooterViewBackgroundRes();
			if (backgroundRes != 0) footerView.setBackgroundResource(backgroundRes);
			
			if (isFooterViewEnabled(activity)) 
				showFooterView(activity, footerView, false, true);
			else 
				hideFooterView(activity, footerView, true, true);
		}
		mFooterView = footerView;
		
		return view;
	}
	
	protected void onUploadButtonClick(IActivity activity, View view) {
		getProvider().onActionButtonClick(activity, SectionListProvider.ACTION_UPLOAD);
	}
	
	protected void onCreateButtonClick(IActivity activity, View view) {
		getProvider().onActionButtonClick(activity, SectionListProvider.ACTION_CREATE);
	}
	
	protected void onScanButtonClick(IActivity activity, View view) {
		getProvider().onActionButtonClick(activity, SectionListProvider.ACTION_SCAN);
	}
	
	protected void onHeaderViewClick(View headerView) {
		Object data = headerView != null ? headerView.getTag() : null;
		if (LOG.isDebugEnabled())
			LOG.debug("onHeaderViewClick: view=" + headerView + " data=" + data);
		
		if (data != null && data instanceof ICategoryData) {
			ICategoryData category = (ICategoryData)data;
			setVisibleSelection(category);
		}
	}
	
	protected void setHeaderView(IVisibleData data, int firstVisibleItem) { 
		View headerView = mHeaderView;
		if (headerView != null) {
			if (firstVisibleItem > 0 && data != null) {
				ICategoryData category = null;
				if (data instanceof ISectionFolder) {
					category = data.getParent().getFolderCategory();;
				} else if (data instanceof ISectionData) {
					category = data.getParent().getFileCategory();
				} else if (data instanceof ICategoryData) {
					category = (ICategoryData)data;
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("setHeaderView: firstVisibleItem=" + firstVisibleItem 
							+ " data=" + data + " category=" + category);
				}
				
				if (category != null) {
					TextView categoryTitle = (TextView)headerView.findViewById(R.id.section_above_category_title);
					if (categoryTitle != null) categoryTitle.setText(category.getName());
					headerView.setTag(category);
					headerView.setVisibility(View.VISIBLE);
					return;
				}
			}
			
			headerView.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onPullToRefresh(IRefreshView refreshView) {
		if (LOG.isDebugEnabled()) LOG.debug("onPullToRefresh: refreshView=" + refreshView);
		super.onPullToRefresh(refreshView);
		
		View headerView = mHeaderView;
		if (headerView != null) {
			headerView.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onTouchEvent(IRefreshView refreshView, MotionEvent event) {
		GestureDetector detector = mGestureDetector; 
		if (detector != null) detector.onTouchEvent(event);
	}
	
	protected GestureDetector onCreateGestureDetector(final IActivity activity) {
		if (activity == null) return null;
		return new GestureDetector(activity.getActivity(), 
				new GestureDetector.OnGestureListener() {
				public boolean onDown(MotionEvent e) { return false; }
				public void onShowPress(MotionEvent e) {}
				public boolean onSingleTapUp(MotionEvent e) { return false; }
				public void onLongPress(MotionEvent e) {}
				public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) { return false; }
				public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { 
					return onTouchScroll(activity, distanceX, distanceY); 
				}
			});
	}
	
	protected boolean onTouchScroll(IActivity activity, float distanceX, float distanceY) {
		if (distanceY > 10) {
			//if (LOG.isDebugEnabled()) LOG.debug("onTouchScroll: distanceY=" + distanceY + " up");
			hideFooterView(activity);
			
		} else if (distanceY < -10) {
			//if (LOG.isDebugEnabled()) LOG.debug("onTouchScroll: distanceY=" + distanceY + " down");
			showFooterView(activity);
		}
		
		return false;
	}
	
	protected boolean isFooterViewEnabled(IActivity activity) { 
		if (activity != null) return activity.getActionHelper().isActionMode() == false;
		return false; 
	}
	
	protected void showFooterView(IActivity activity) {
		if (isFooterViewEnabled(activity)) 
			showFooterView(activity, true, false);
	}
	
	protected void showFooterView(IActivity activity, 
			boolean animShow, boolean force) {
		final View view = mFooterView;
		showFooterView(activity, view, animShow, force);
	}
	
	protected void showFooterView(IActivity activity, View view, 
			boolean animShow, boolean force) {
		if (view != null && activity != null) {
			if (view.getVisibility() != View.VISIBLE || force) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("showFooterView: activity=" + activity + " view=" + view
							+ " animShow=" + animShow + " force=" + force);
				}
				
				if (animShow) {
					int animRes = AppResources.getInstance().getAnimRes(AppResources.anim.section_list_footer_show_animation);
					if (animRes == 0) animRes = R.anim.ds_slide_in_up;
					Animation ani = AnimationUtils.loadAnimation(activity.getActivity(), animRes);
					view.setAnimation(ani);
				}
				view.setVisibility(View.VISIBLE);
			}
		}
	}
	
	protected void hideFooterView(IActivity activity) {
		if (isFooterViewEnabled(activity)) 
			hideFooterView(activity, true, false);
	}
	
	protected void hideFooterView(IActivity activity, 
			boolean animShow, boolean force) {
		final View view = mFooterView;
		hideFooterView(activity, view, animShow, force);
	}
	
	protected void hideFooterView(IActivity activity, View view, 
			boolean animShow, boolean force) {
		if (view != null && activity != null) {
			int firstVisibleItem = mFirstVisibleItem;
			if ((view.getVisibility() == View.VISIBLE && firstVisibleItem > 0) || force) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("hideFooterView: activity=" + activity + " view=" + view 
							+ " animShow=" + animShow + " force=" + force);
				}
				
				if (animShow) {
					int animRes = AppResources.getInstance().getAnimRes(AppResources.anim.section_list_footer_hide_animation);
					if (animRes == 0) animRes = R.anim.ds_slide_out_down;
					Animation ani = AnimationUtils.loadAnimation(activity.getActivity(), animRes);
					view.setAnimation(ani);
				}
				view.setVisibility(View.INVISIBLE);
			}
		}
	}
	
	public void showAboveCenterView(boolean show) {
		final View view = mCenterView;
		if (view != null) {
			if (show) {
				if (view instanceof FrameLayout) {
					FrameLayout centerView = (FrameLayout)view;
					centerView.removeAllViews();
					View v = getCenterView();
					if (v != null) { 
						centerView.addView(v, new FrameLayout.LayoutParams(
								FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
								Gravity.CENTER));
					}
				}
				int animRes = AppResources.getInstance().getAnimRes(AppResources.anim.section_empty_folder_show_animation);
				//if (animRes == 0) animRes = R.anim.empty_folder_in;
				if (animRes != 0) {
					Animation ani = AnimationUtils.loadAnimation(ResourceHelper.getContext(), animRes);
					view.setAnimation(ani);
				}
				view.setVisibility(View.VISIBLE);
			} else {
				view.setVisibility(View.GONE);
			}
		}
	}
	
	protected View getCenterView() { 
		//final LayoutInflater inflater = LayoutInflater.from(ResourceHelper.getContext());
		//final View view = inflater.inflate(R.layout.section_empty_folder, null);
		
		//ImageView imageView = (ImageView)view.findViewById(R.id.section_empty_image);
		//if (imageView != null) {
		//	int emptyRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.section_empty_folder_image);
		//	if (emptyRes != 0) imageView.setImageResource(emptyRes);
		//}
		
		return null; //view;
	}
	
	protected View getShortcutUploadButtonView(IActivity activity, 
			LayoutInflater inflater, View.OnClickListener listener) {
		return getShortcutButtonView(activity, inflater, 
				R.string.label_action_upload, R.drawable.ic_shortcut_upload,
				listener);
	}
	
	protected View getShortcutCreateButtonView(IActivity activity, 
			LayoutInflater inflater, View.OnClickListener listener) {
		return getShortcutButtonView(activity, inflater, 
				R.string.label_action_create, R.drawable.ic_shortcut_create,
				listener);
	}
	
	protected View getShortcutScanButtonView(IActivity activity, 
			LayoutInflater inflater, View.OnClickListener listener) {
		return getShortcutButtonView(activity, inflater, 
				R.string.label_action_scan, R.drawable.ic_shortcut_camera,
				listener);
	}
	
	protected View getShortcutButtonView(IActivity activity, LayoutInflater inflater, 
			int titleRes, int iconRes, View.OnClickListener listener) {
		if (activity == null || inflater == null) return null;
		final View view = inflater.inflate(R.layout.section_shortcut_button, null);
		
		ImageView iconView = (ImageView)view.findViewById(R.id.section_shortcut_button_icon);
		if (iconView != null) iconView.setImageResource(iconRes);
		
		TextView titleView = (TextView)view.findViewById(R.id.section_shortcut_button_title);
		if (titleView != null) titleView.setText(titleRes);
		
		int backgroundRes = getFooterShortcutViewBackgroundRes();
		if (backgroundRes != 0) view.setBackgroundResource(backgroundRes);
		
		view.setOnClickListener(listener);
		
		return view;
	}
	
}
