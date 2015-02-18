package org.javenstudio.provider.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public class SelectDialogHelper {
	private static final Logger LOG = Logger.getLogger(SelectDialogHelper.class);

	public static interface ISelectItem {
		public CharSequence getTitle();
		public CharSequence getSubTitle();
	}
	
	public static class SelectCategory implements ISelectItem {
		private CharSequence mTitle = null;
		private CharSequence mSubTitle = null;
		
		public SelectCategory(CharSequence title) {
			mTitle = title;
		}
		
		public SelectCategory(int titleRes) {
			mTitle = ResourceHelper.getResources().getString(titleRes);
		}
		
		public CharSequence getTitle() { return mTitle; }
		public void setTitle(CharSequence title) { mTitle = title; }
		
		public CharSequence getSubTitle() { return mSubTitle; }
		public void setSubTitle(CharSequence title) { mSubTitle = title; }
	}
	
	public static class SelectItem implements ISelectItem {
		private CharSequence mTitle = null;
		private CharSequence mSubTitle = null;
		
		public SelectItem() {}
		
		public SelectItem(CharSequence title) {
			mTitle = title;
		}
		
		public SelectItem(int titleRes) {
			mTitle = ResourceHelper.getResources().getString(titleRes);
		}
		
		public CharSequence getTitle() { return mTitle; }
		public void setTitle(CharSequence title) { mTitle = title; }
		
		public CharSequence getSubTitle() { return mSubTitle; }
		public void setSubTitle(CharSequence title) { mSubTitle = title; }
		
		public Drawable getIcon() { return null; }
		public Drawable getItemDrawable() { return null; }
		
		public boolean isSelected() { return false; }
		public void onItemClick(Activity activity) {}
	}
	
	public boolean showSelectDialog(final Activity activity, 
			final ISelectItem[] items, int titleRes) {
		return showSelectDialog(activity, items, (CharSequence)null, titleRes);
	}
	
	public boolean showSelectDialog(final Activity activity, 
			final ISelectItem[] items, CharSequence title) {
		return showSelectDialog(activity, items, title, 0);
	}
	
	private boolean showSelectDialog(final Activity activity, 
			final ISelectItem[] items, CharSequence title, int titleRes) {
		if (items == null || activity == null || activity.isDestroyed()) 
			return false;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("showSelectDialog: activity=" + activity 
					+ " itemCount=" + (items != null ? items.length : 0));
		}
		
		if (title == null && titleRes != 0) 
			title = ResourceHelper.getResources().getString(titleRes);
		
		final AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		builder.setTitle(title);
		builder.setAdapter(createAdapter(activity, items), null);
		
		builder.setPositiveButton(R.string.dialog_cancel_button, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		
		builder.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					onDialogShow(builder, dialog);
				}
			});
		
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					onDialogDismiss(builder, dialog);
					mDialog = null;
					mBuilder = null;
				}
			});
		
		AlertDialog dialog = builder.show(activity);
		mDialog = dialog;
		mBuilder = builder;
		
		return dialog != null; 
	}
	
	private AlertDialogBuilder mBuilder = null;
	private AlertDialog mDialog = null;
	
	public AlertDialogBuilder getBuilder() { return mBuilder; }
	public AlertDialog getDialog() { return mDialog; }
	
	public void dismissDialog() { 
		AlertDialog dialog = mDialog;
		if (dialog != null) dialog.dismiss();
	}
	
	protected void onDialogDismiss(AlertDialogBuilder builder, 
			DialogInterface dialog) {}
	
	protected void onDialogShow(AlertDialogBuilder builder, 
			DialogInterface dialog) {}
	
	public ListAdapter createAdapter(final Activity context, ISelectItem[] items) {
		if (context == null || items == null) return null;
		
		ArrayAdapter<ISelectItem> adapter = new ArrayAdapter<ISelectItem>(context, 0, items) { 
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					ISelectItem item = getItem(position);
					if (item != null) {
						if (item instanceof SelectItem)
							return getItemView(context, (SelectItem)item, convertView, parent);
						else if (item instanceof SelectCategory)
							return getCategoryView(context, (SelectCategory)item, convertView, parent);
					}
					return null;
				}
				@Override
				public boolean areAllItemsEnabled() {
			        return false;
			    }
				@Override
			    public boolean isEnabled(int position) {
			        return false;
			    }
			};
		
		return adapter;
	}
	
	protected View getCategoryView(final Activity context, 
			final SelectCategory item, View convertView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		final View view = inflater.inflate(R.layout.select_list_category, null);
		
		final TextView titleView = (TextView)view.findViewById(R.id.select_list_item_title);
		if (titleView != null) {
			titleView.setText(item.getTitle());
			titleView.setVisibility(View.VISIBLE);
		}
		
		return view;
	}
	
	protected View getItemView(final Activity context, 
			final SelectItem item, View convertView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		final View view = inflater.inflate(R.layout.select_list_item, null);
		
		final TextView titleView = (TextView)view.findViewById(R.id.select_list_item_title);
		if (titleView != null) {
			titleView.setText(item.getTitle());
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.select_list_item_subtitle);
		if (subtitleView != null) {
			subtitleView.setText(item.getSubTitle());
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.select_list_item_poster_icon);
		if (iconView != null) {
			Drawable icon = item.getIcon();
			if (icon != null) iconView.setImageDrawable(icon);
			iconView.setVisibility(View.VISIBLE);
		}
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.select_list_item_poster_image);
		if (imageView != null) {
			Drawable fd = item.getItemDrawable();
			if (fd != null) {
				DataBinder.onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				imageView.setVisibility(View.VISIBLE);
				DataBinder.onImageDrawableBinded(fd, true);
			} else {
				imageView.setImageDrawable(null);
				imageView.setVisibility(View.GONE);
			}
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = getItemViewBackgroundRes(item.isSelected());
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						item.onItemClick(context);
					}
				});
		}
		
		return view;
	}
	
	protected int getItemViewBackgroundRes(boolean selected) {
		return AppResources.getInstance().getDrawableRes(
				selected ? AppResources.drawable.section_item_background_selected : 
					AppResources.drawable.section_item_background);
	}
	
}
