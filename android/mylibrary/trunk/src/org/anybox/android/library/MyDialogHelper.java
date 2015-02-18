package org.anybox.android.library;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AlertDialogHelper;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.common.util.Logger;

public class MyDialogHelper extends AlertDialogHelper {
	private static final Logger LOG = Logger.getLogger(MyDialogHelper.class);

	@Override
	public void onAlertDialogPreCreate(AlertDialogBuilder builder) {
		if (builder == null) return;
		super.onAlertDialogPreCreate(builder);
		
		View view = getCustomTitle(builder.getContext(), null, 
				builder.getTitle(), builder.getIcon(), builder.getIconRes(), 
				builder.getRefreshListener());
		
		builder.setCustomTitle(view);
	}
	
	@Override
	public void setDialogTitle(AlertDialog dialog, CharSequence title) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogTitle: dialog=" + dialog 
					+ " title=" + title);
		}
		
		TextView titleView = findTitleView(dialog);
		if (titleView != null) {
			titleView.setText(title);
			titleView.setVisibility(View.VISIBLE);
			
			return;
		}
		
		super.setDialogTitle(dialog, title);
	}
	
	@Override
	public void setDialogTitle(AlertDialog dialog, int titleRes) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogTitle: dialog=" + dialog 
					+ " titleRes=" + titleRes);
		}
		
		TextView titleView = findTitleView(dialog);
		if (titleView != null) {
			titleView.setText(titleRes);
			titleView.setVisibility(View.VISIBLE);
			
			return;
		}
		
		super.setDialogTitle(dialog, titleRes);
	}
	
	@Override
	public void setDialogIcon(AlertDialog dialog, Drawable icon) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogIcon: dialog=" + dialog 
					+ " icon=" + icon);
		}
		
		ImageView iconView = findIconView(dialog);
		if (iconView != null) {
			if (icon !=  null) {
				iconView.setImageDrawable(icon);
				iconView.setVisibility(View.VISIBLE);
			} else {
				iconView.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	public void setDialogIcon(AlertDialog dialog, int iconRes) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogIcon: dialog=" + dialog 
					+ " iconRes=" + iconRes);
		}
		
		ImageView iconView = findIconView(dialog);
		if (iconView != null) {
			if (iconRes != 0) {
				iconView.setImageResource(iconRes);
				iconView.setVisibility(View.VISIBLE);
			} else {
				iconView.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	public void setDialogIndicator(AlertDialog dialog, Drawable icon) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogIndicator: dialog=" + dialog 
					+ " icon=" + icon);
		}
		
		ImageView iconView = findIndicatorView(dialog);
		if (iconView != null) {
			if (icon !=  null) {
				iconView.setImageDrawable(icon);
				iconView.setVisibility(View.VISIBLE);
			} else {
				iconView.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	public void setDialogIndicator(AlertDialog dialog, int iconRes) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogIndicator: dialog=" + dialog 
					+ " iconRes=" + iconRes);
		}
		
		ImageView iconView = findIndicatorView(dialog);
		if (iconView != null) {
			if (iconRes != 0) {
				iconView.setImageResource(iconRes);
				iconView.setVisibility(View.VISIBLE);
			} else {
				iconView.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	public void setDialogIconListener(AlertDialog dialog, 
			View.OnClickListener listener) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogIconListener: dialog=" + dialog 
					+ " listener=" + listener);
		}
		
		View view = findIconContainer(dialog);
		if (view != null) {
			view.setOnClickListener(listener);
		}
	}
	
	@Override
	public void setDialogTitleListener(AlertDialog dialog, 
			View.OnClickListener listener) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogTitleListener: dialog=" + dialog 
					+ " listener=" + listener);
		}
		
		View view = findTitleContainer(dialog);
		if (view != null) {
			view.setOnClickListener(listener);
		}
	}
	
	@Override
	public void setDialogRefreshListener(AlertDialog dialog, 
			View.OnClickListener listener, boolean showRefresh) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogRefreshListener: dialog=" + dialog 
					+ " listener=" + listener + " showRefresh=" + showRefresh);
		}
		
		ImageView refreshView = findRefreshView(dialog);
		if (refreshView != null) {
			refreshView.setOnClickListener(listener);
			refreshView.setVisibility(showRefresh ? View.VISIBLE : View.GONE);
		}
	}
	
	@Override
	public void showDialogProgressBar(AlertDialog dialog, 
			boolean showProgress, boolean showRefresh) {
		if (dialog == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("onShowDialogProgressBar: dialog=" + dialog 
					+ " showProgress=" + showProgress + " showRefresh=" + showRefresh);
		}
		
		View progressView = findProgressView(dialog);
		if (progressView != null) {
			progressView.setVisibility(showProgress ? View.VISIBLE : View.GONE);
		}
		
		ImageView refreshView = findRefreshView(dialog);
		if (refreshView != null) {
			refreshView.setVisibility(showRefresh ? View.VISIBLE : View.GONE);
		}
	}
	
	public TextView findTitleView(AlertDialog dialog) {
		if (dialog == null) return null;
		return (TextView)dialog.findViewById(R.id.dialog_title);
	}
	
	public ImageView findIconView(AlertDialog dialog) {
		if (dialog == null) return null;
		return (ImageView)dialog.findViewById(R.id.dialog_icon);
	}
	
	public ImageView findIndicatorView(AlertDialog dialog) {
		if (dialog == null) return null;
		return (ImageView)dialog.findViewById(R.id.dialog_indicator);
	}
	
	public ImageView findRefreshView(AlertDialog dialog) {
		if (dialog == null) return null;
		return (ImageView)dialog.findViewById(R.id.dialog_refresh);
	}
	
	public View findProgressView(AlertDialog dialog) {
		if (dialog == null) return null;
		return dialog.findViewById(R.id.dialog_progressbar);
	}
	
	public View findIconContainer(AlertDialog dialog) {
		if (dialog == null) return null;
		return dialog.findViewById(R.id.dialog_icon_container);
	}
	
	public View findTitleContainer(AlertDialog dialog) {
		if (dialog == null) return null;
		return dialog.findViewById(R.id.dialog_title_container);
	}
	
	public View findActionContainer(AlertDialog dialog) {
		if (dialog == null) return null;
		return dialog.findViewById(R.id.dialog_action_container);
	}
	
	private static View getCustomTitle(Context context, View view, 
			CharSequence title, Drawable icon, int iconRes, 
			View.OnClickListener refreshListener) {
		if (context == null) return null;
		if (LOG.isDebugEnabled()) 
			LOG.debug("getCustomTitle: view=" + view + " title=" + title);
		
		if (view == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
			view = inflater.inflate(R.layout.dialog_custom_title, null);
		}
		
		TextView titleView = (TextView)view.findViewById(R.id.dialog_title);
		if (titleView != null) {
			titleView.setText(title);
			titleView.setVisibility(View.VISIBLE);
		}
		
		ImageView iconView = (ImageView)view.findViewById(R.id.dialog_icon);
		if (iconView != null) {
			if (icon != null) {
				iconView.setImageDrawable(icon);
				iconView.setVisibility(View.VISIBLE);
			} else if (iconRes != 0) {
				iconView.setImageResource(iconRes);
				iconView.setVisibility(View.VISIBLE);
			} else {
				iconView.setVisibility(View.GONE);
			}
		}
		
		ImageView indicatorView = (ImageView)view.findViewById(R.id.dialog_indicator);
		if (indicatorView != null) {
			indicatorView.setVisibility(View.GONE);
		}
		
		View progressView = view.findViewById(R.id.dialog_progressbar);
		if (progressView != null) {
			progressView.setVisibility(View.GONE);
		}
		
		ImageView refreshView = (ImageView)view.findViewById(R.id.dialog_refresh);
		if (refreshView != null) {
			if (refreshListener != null) {
				refreshView.setOnClickListener(refreshListener);
				refreshView.setVisibility(View.VISIBLE);
			} else {
				refreshView.setVisibility(View.GONE);
			}
		}
		
		return view;
	}
	
	@Override
	public void onAlertDialogPreShow(AlertDialog dialog) {
		if (dialog == null) return;
		super.onAlertDialogPreShow(dialog);
		
		dialog.getWindow().getAttributes().windowAnimations = R.style.AppAnimation_Dialog;
	}
	
	@Override
	public void onAlertDialogShow(AlertDialog dialog) {
		if (dialog == null) return;
		super.onAlertDialogShow(dialog);
		
		final Resources res = dialog.getContext().getResources();
		
		//int topPanelId = res.getIdentifier("topPanel", "id", "android");
		//View topPanel = dialog.findViewById(topPanelId);
		//if (topPanel != null) topPanel.setBackgroundColor(R.color.main_actionbar_background_light);
		
		int titleDividerId = res.getIdentifier("titleDivider", "id", "android");
		View titleDivider = dialog.findViewById(titleDividerId);
		if (titleDivider != null) titleDivider.setVisibility(View.GONE);
		
		int titleDividerTopId = res.getIdentifier("titleDividerTop", "id", "android");
		View titleDividerTop = dialog.findViewById(titleDividerTopId);
		if (titleDividerTop != null) titleDividerTop.setVisibility(View.GONE);
		
		int messageId = res.getIdentifier("message", "id", "android");
		View messageView = dialog.findViewById(messageId);
		if (messageView != null && messageView instanceof TextView) {
			TextView messageText = (TextView)messageView;
			messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.dialog_message_size));
			messageText.setLinkTextColor(res.getColor(R.color.dialog_message_link_color));
		}
		
		int scrollId = res.getIdentifier("scrollView", "id", "android");
		View scrollView = dialog.findViewById(scrollId);
		if (scrollView != null && scrollView instanceof ScrollView) {
			ScrollView sv = (ScrollView)scrollView;
			sv.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
		}
		
		ListView listView = dialog.getListView();
		if (listView != null) {
			listView.setSelector(R.drawable.list_selector_holo_light);
			RefreshListView.disableOverscrollGlowEdge(listView);
		}
		
		int buttonId1 = res.getIdentifier("button1", "id", "android");
		int buttonId2 = res.getIdentifier("button2", "id", "android");
		int buttonId3 = res.getIdentifier("button3", "id", "android");
		
		View button1 = dialog.findViewById(buttonId1);
		View button2 = dialog.findViewById(buttonId2);
		View button3 = dialog.findViewById(buttonId3);
		
		if (button1 != null) button1.setBackgroundResource(R.drawable.list_selector_holo_light);
		if (button2 != null) button2.setBackgroundResource(R.drawable.list_selector_holo_light);
		if (button3 != null) button3.setBackgroundResource(R.drawable.list_selector_holo_light);
	}
	
}
