package org.javenstudio.cocoka.app;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.common.util.Logger;

public class ActionAdapter extends ArrayAdapter<ActionItem> {
	private static final Logger LOG = Logger.getLogger(ActionAdapter.class);
	
	public ActionAdapter(Context context, ActionItem[] items) {
		super(context, 0, items);
	}
	
	@Override
	public final View getView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, false);
	}
	
	@Override
    public final View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, true);
	}
	
	@Override
	public boolean isEnabled(int position) {
		final ActionItem item = getItem(position);
		if (item != null) return isItemEnabled(item);
        return false;
    }
	
	@Override
	public long getItemId(int position) {
		final ActionItem item = getItem(position);
		if (item != null) return item.getIdentity();
        return -1;
	}
	
	private View getView(int position, View convertView, boolean dropdown) {
		final LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = convertView;
		
		final ActionItem item = getItem(position);
		if (item != null) {
			view = getItemView(item, inflater, convertView, dropdown);
			if (view == null) {
				view = inflater.inflate(dropdown ? R.layout.actionbar_list_item_dropdown : 
					R.layout.actionbar_list_item, null);
				bindItemView(getContext(), item, view, dropdown);
			}
			
			item.onViewBinded(view, dropdown);
		}
		
		return view;
	}
	
	protected View getItemView(ActionItem item, LayoutInflater inflater, 
			View convertView, boolean dropdown) {
		return item.getView(inflater, convertView, dropdown);
	}
	
	protected boolean isItemEnabled(ActionItem item) {
		return item.isEnabled();
	}
	
	static class ItemView { 
		private final WeakReference<View> mViewRef;
		private final boolean mDropdown;
		
		public ItemView(View view, boolean dropdown) { 
			mViewRef = view != null ? new WeakReference<View>(view) : null;
			mDropdown = dropdown;
		}
		
		public boolean isDropdown() { return mDropdown; }
		public View getView() { return mViewRef != null ? mViewRef.get() : null; }
	}
	
	static class ItemViews { 
		private final List<ItemView> mViews = new ArrayList<ItemView>();
		
		public synchronized View getDropDownView() {
			for (int i=0; i < mViews.size(); ) { 
				ItemView itemView = mViews.get(i);
				View v = itemView != null ? itemView.getView() : null;
				if (itemView != null && v != null) { 
					if (itemView.isDropdown()) return v;
				}
			}
			return null;
		}
		
		public synchronized void addItemView(View view, boolean dropdown) { 
			boolean found = false;
			
			for (int i=0; i < mViews.size(); ) { 
				ItemView itemView = mViews.get(i);
				View v = itemView != null ? itemView.getView() : null;
				if (itemView != null && v != null) { 
					if (v == view) found = true;
					i ++; continue;
				}
				mViews.remove(i);
			}
			
			if (!found && view != null) 
				mViews.add(new ItemView(view, dropdown));
		}
		
		public synchronized void rebindViews(Context context, ActionItem item) { 
			for (int i=0; i < mViews.size(); ) { 
				ItemView itemView = mViews.get(i);
				View v = itemView != null ? itemView.getView() : null;
				if (itemView != null && v != null) { 
					bindItemView(context, item, v, itemView.isDropdown());
					i ++; continue;
				}
				mViews.remove(i);
			}
		}
	}
	
	static void bindItemView(Context context, ActionItem item, 
			View view, boolean dropdown) { 
		if (context == null || item == null || view == null) 
			return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("bindItemView: item=" + item + " view=" + view 
					+ " dropdown=" + dropdown);
		}
		
		final Resources res = context.getResources();

		TextView title = (TextView)view.findViewById(R.id.actionbar_list_item_title);
		TextView subtitle = (TextView)view.findViewById(R.id.actionbar_list_item_subtitle);
		ImageView icon = (ImageView)view.findViewById(R.id.actionbar_list_item_icon);
		
		if (subtitle != null) 
			subtitle.setVisibility(View.GONE);
		if (icon != null)
			icon.setVisibility(View.GONE);
		
		boolean hasSubTitle = false;
		boolean showIcon = item.showIcon(dropdown);
		
		if (dropdown) {
			if (title != null)
				title.setText(item.getDropdownText());
			
			if (showIcon) {
				final Drawable d = item.getDropdownIcon();
				final int iconRes = item.getIconRes();
				
				if (d != null) { 
					if (icon != null) {
						icon.setImageDrawable(d); 
						icon.setVisibility(View.VISIBLE);
					}
				} else if (iconRes != 0) { 
					if (icon != null) {
						icon.setImageResource(iconRes);
						icon.setVisibility(View.VISIBLE);
					}
				}
			}
		} else { 
			if (title != null)
				title.setText(item.getTitle());
			
			CharSequence subtext = item.getSubTitle();
			if (subtext != null && subtext.length() > 0) {
				if (subtitle != null) {
					subtitle.setText(subtext);
					subtitle.setVisibility(View.VISIBLE);
				}
				hasSubTitle = true;
			}
			
			if (showIcon) {
				final Drawable d = item.getIcon();
				final int iconRes = item.getIconRes();
				
				if (d != null) { 
					if (icon != null) {
						icon.setImageDrawable(d); 
						icon.setVisibility(View.VISIBLE);
					}
				} else if (iconRes != 0) { 
					if (icon != null) {
						icon.setImageResource(iconRes);
						icon.setVisibility(View.VISIBLE);
					}
				}
			}
		}
		
		if (title != null) { 
			title.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(hasSubTitle ? 
					R.dimen.action_bar_title_text_size : (dropdown ?
					R.dimen.action_bar_title_text_size_dropdown_nosub : 
					R.dimen.action_bar_title_text_size_nosub)));
			
			item.onTitleBinded(title, subtitle, dropdown);
		}
	}
	
	static void bindItemView2(Context context, ActionItem item, 
			View view, boolean dropdown) { 
		if (context == null || item == null || view == null) 
			return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("bindItemView2: item=" + item + " view=" + view 
					+ " dropdown=" + dropdown);
		}
		
		final Resources res = context.getResources();

		TextView title = (TextView)view.findViewById(R.id.actionbar_list_item_title);
		TextView subtitle = (TextView)view.findViewById(R.id.actionbar_list_item_subtitle);
		ImageView icon = (ImageView)view.findViewById(R.id.actionbar_list_item_icon);
		
		if (subtitle != null) 
			subtitle.setVisibility(View.GONE);
		if (icon != null)
			icon.setVisibility(View.GONE);
		
		boolean hasSubTitle = false;
		
		if (dropdown) {
			if (title != null)
				title.setText(item.getDropdownText());
			
			final Drawable d = item.getIcon();
			final int iconRes = item.getIconRes();
			
			if (d != null) { 
				if (icon != null) {
					icon.setImageDrawable(d); 
					icon.setVisibility(View.VISIBLE);
				}
			} else if (iconRes != 0) { 
				if (icon != null) {
					icon.setImageResource(iconRes);
					icon.setVisibility(View.VISIBLE);
				}
			}
			
		} else { 
			if (title != null)
				title.setText(item.getTitle());
			
			CharSequence subtext = item.getSubTitle();
			if (subtext != null && subtext.length() > 0) {
				if (subtitle != null) {
					subtitle.setText(subtext);
					subtitle.setVisibility(View.VISIBLE);
				}
				hasSubTitle = true;
			}
		}
		
		if (title != null) { 
			title.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(hasSubTitle ? 
					R.dimen.action_bar_title_text_size : (dropdown ?
					R.dimen.action_bar_title_text_size_dropdown_nosub : 
					R.dimen.action_bar_title_text_size_nosub)));
		}
	}
	
}
