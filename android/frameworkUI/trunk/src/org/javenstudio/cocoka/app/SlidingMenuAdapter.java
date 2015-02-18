package org.javenstudio.cocoka.app;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SlidingMenuAdapter extends ArrayAdapter<ActionItem> {
	
	public SlidingMenuAdapter(Activity context, ActionItem[] items) {
		super(context, 0, items);
	}

	public Activity getActivity() { return (Activity)getContext(); }
	
	@Override
    public boolean isEnabled(int position) {
		ActionItem item = getItem(position);
        return item != null ? item.getSubItems() == null : true;
    }
	
	@Override
	public final View getView(int position, View convertView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(getContext());
		if (convertView == null) 
			convertView = inflater.inflate(R.layout.slidingmenu_row, null);
		
		final ActionItem item = getItem(position);
		if (item == null) return convertView;
		
		ImageView icon = (ImageView) convertView.findViewById(R.id.slidingmenu_row_icon);
		initItemIcon(item, icon);
		
		TextView title = (TextView) convertView.findViewById(R.id.slidingmenu_row_title);
		initItemText(item, title);

		initSubLayout(inflater, item, convertView, icon);
		
		View mainView = convertView.findViewById(R.id.slidingmenu_row_main);
		initItemBackground(item, mainView);
		
		return convertView;
	}
	
	protected void initItemIcon(final ActionItem item, final ImageView icon) { 
		if (item == null || icon == null) return;
		
		if (item.getIcon() != null) 
			icon.setImageDrawable(item.getIcon());
		else if (item.getIconRes() != 0) 
			icon.setImageResource(item.getIconRes());
	}
	
	protected void initItemText(final ActionItem item, final TextView title) { 
		if (item == null || title == null) return;
		
		int colorRes = item.getTitleColorRes();
		int statelistRes = item.getTitleColorStateListRes();
		int sizeRes = item.getTitleSizeRes();
		
		if (statelistRes != 0) {
			ColorStateList stateList = getContext().getResources().getColorStateList(statelistRes);
			if (stateList != null) title.setTextColor(stateList);
		} else if (colorRes != 0) {
			int color = getContext().getResources().getColor(colorRes);
			title.setTextColor(color);
		}
		
		if (sizeRes != 0) {
			title.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
					getContext().getResources().getDimensionPixelSize(sizeRes));
		}
		
		title.setText(item.getTitle());
	}
	
	protected void initItemBackground(final ActionItem item, final View view) { 
		if (item == null || view == null) return;
		
		Drawable background = item.getBackgroundDrawable();
		int backgroundRes = item.getBackgroundRes();
		
		if (background != null) view.setBackground(background);
		else if (backgroundRes != 0) view.setBackgroundResource(backgroundRes);
		//else view.setBackgroundResource(R.drawable.abs__list_selector_holo_light);
	}
	
	private void initSubLayout(final LayoutInflater inflater, final ActionItem item, 
			final View view, final ImageView icon) { 
		if (inflater == null || item == null || view == null || icon == null) 
			return;
		
		final SlidingMenuLayout subContainer = (SlidingMenuLayout) view.findViewById(R.id.slidingmenu_row_subcontainer);
		final ViewGroup subLayout = (ViewGroup) view.findViewById(R.id.slidingmenu_row_sub);
		final View mainView = view.findViewById(R.id.slidingmenu_row_main);
		
		final ActionItem[] subItems = item.getSubItems();
		if (subItems != null && mainView != null) { 
			initSubViews(subItems, subLayout);
			
			final Runnable collapser = new Runnable() {
					@Override
					public void run() {
						if (subContainer.getStatus() == SlidingMenuLayout.Status.EXPANDED) {
							//initItemIcon(item, icon);
							subContainer.collapse(false);
						}
					}
				};
			
			final Runnable expender = new Runnable() {
					@Override
					public void run() {
						if (subContainer.getStatus() == SlidingMenuLayout.Status.COLLAPSED) {
							//icon.setImageResource(R.drawable.add_light);
							boolean changed = item.onSubItemExpand(getActivity());
							if (changed) { 
								initSubViews(item.getSubItems(), subLayout); 
								//subContainer.invalidate();
								//subContainer.requestLayout();
							}
							subContainer.expand(changed);
						}
					}
				};
			
			item.setCollapser(collapser);
			item.setExpender(expender);
			
			mainView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final SlidingMenuLayout.Status status = subContainer.getStatus();
						if (status == SlidingMenuLayout.Status.EXPANDED) { 
							collapser.run();
						} else if (status == SlidingMenuLayout.Status.COLLAPSED) { 
							expender.run();
						}
					}
				});
		}
	}
	
	private void initSubViews(final ActionItem[] subItems, ViewGroup subLayout) { 
		if (subItems == null || subLayout == null) 
			return;
		
		final LayoutInflater inflater = LayoutInflater.from(getContext());
		subLayout.removeAllViews();
		
		for (int i=0; subItems != null && i < subItems.length; i++) { 
			final ActionItem subItem = subItems[i];
			if (subItem == null) continue;
			
			View subView = inflater.inflate(R.layout.slidingmenu_row_item, null);
			subView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						ActionItem.OnClickListener listener = subItem.getOnClickListener();
						if (listener != null) 
							listener.onActionClick();
					}
				});
			
			ImageView subIcon = (ImageView) subView.findViewById(R.id.slidingmenu_row_item_icon);
			if (subItem.getIcon() != null) 
				subIcon.setImageDrawable(subItem.getIcon());
			else if (subItem.getIconRes() != 0)
				subIcon.setImageResource(subItem.getIconRes());
			
			TextView subTitle = (TextView) subView.findViewById(R.id.slidingmenu_row_item_title);
			subTitle.setText(subItem.getName());
			
			subLayout.addView(subView);
		}
		
		//subLayout.setLayoutAnimation(newSubLayoutAnimations());
	}
	
	//protected LayoutAnimationController newSubLayoutAnimations() { 
		//Animation ani = new AlphaAnimation(0.0f, 1.0f);
		//ani.setDuration(400);
		
		//LayoutAnimationController controller = new LayoutAnimationController(ani);
		//controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
		//controller.setDelay(0.0f);
		
		//return null;
	//}

}
