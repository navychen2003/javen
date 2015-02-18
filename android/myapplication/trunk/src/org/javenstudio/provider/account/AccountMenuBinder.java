package org.javenstudio.provider.account;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.cocoka.graphics.ChartDrawable;
import org.javenstudio.cocoka.graphics.DelegatedDrawable;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.activity.AccountMenuActivity;
import org.javenstudio.provider.activity.AccountMenuFragment;

public abstract class AccountMenuBinder extends DataBinder {
	private static final Logger LOG = Logger.getLogger(AccountMenuBinder.class);

	public abstract AccountInfoProvider getProvider();
	
	private int mVisibility = View.GONE;
	public int getVisibility() { return mVisibility; }
	
	public void onMenuScrolled(AccountMenuFragment fragment, 
			View view, float percentOpen) {
	}
	
	public void onMenuVisibilityChanged(AccountMenuFragment fragment, 
			View view, int visibility) {
		if (fragment == null || view == null) return;
		mVisibility = visibility;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onMenuVisibilityChanged: fragment=" + fragment + " view=" + view 
					+ " visibility=" + visibility);
		}
		
		if (visibility == View.VISIBLE) {
			bindHeaderView(fragment, view, null);
			bindFooterView(fragment, view, null);
			bindBehindView(fragment, view, null);
			bindAboveView(fragment, view, null);
		}
	}
	
	public View createHeaderView(AccountMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.account_menuheader, null);
	}

	public View createFooterView(AccountMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.account_menufooter, null);
	}
	
	public View createBehindView(AccountMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return null;
	}
	
	public View createAboveView(AccountMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return null;
	}
	
	public void onViewCreated(AccountMenuFragment fragment, 
			View view, Bundle savedInstanceState) {
	}
	
	public void onActivityCreated(AccountMenuFragment fragment, 
			Bundle savedInstanceState) {
		if (fragment == null) return;
		bindHeaderView(fragment, fragment.getView(), savedInstanceState);
		bindFooterView(fragment, fragment.getView(), savedInstanceState);
		bindBehindView(fragment, fragment.getView(), savedInstanceState);
		bindAboveView(fragment, fragment.getView(), savedInstanceState);
	}
	
	public int getHeaderTopBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.accountmenu_header_top_background);
	}
	
	public int getHeaderBottomBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.accountmenu_header_bottom_background);
	}
	
	public void bindHeaderView(AccountMenuFragment fragment, View view, 
			Bundle savedInstanceState) {
		if (fragment == null || view == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("bindHeaderView: fragment=" + fragment + " view=" + view);
		
		final AccountMenuActivity menuactivity = (AccountMenuActivity)fragment.getActivity();
		
		final ImageView avatarView = (ImageView)view.findViewById(R.id.accountmenu_header_avatar_image);
		if (avatarView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.accountmenu_avatar_round_selector);
			if (backgroundRes != 0) avatarView.setBackgroundResource(backgroundRes);
			
			int size = AppResources.getInstance().getResources().getDimensionPixelSize(R.dimen.accountmenu_avatar_size);
			Drawable d = getProvider().getAccountItem().getAvatarRoundDrawable(size, 0);
			if (d != null) { 
				DataBinder.onImageDrawablePreBind(d, avatarView);
				avatarView.setImageDrawable(d);
				DataBinder.onImageDrawableBinded(d, false);
			}
			
			avatarView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						menuactivity.showAccountProfile();
					}
				});
		}
		
		TextView titleView = (TextView)view.findViewById(R.id.accountmenu_header_title);
		if (titleView != null) {
			int colorsRes = AppResources.getInstance().getColorStateListRes(AppResources.color.accountmenu_header_title_color);
			if (colorsRes != 0) titleView.setTextColor(AppResources.getInstance().getResources().getColorStateList(colorsRes));
			CharSequence title = getProvider().getAccountItem().getUserTitle();
			if (title == null || title.length() == 0) 
				title = AppResources.getInstance().getStringText(AppResources.string.accountmenu_title);
			if (title != null) titleView.setText(title);
		}
		
		TextView subtitleView = (TextView)view.findViewById(R.id.accountmenu_header_subtitle);
		if (subtitleView != null) {
			int colorsRes = AppResources.getInstance().getColorStateListRes(AppResources.color.accountmenu_header_subtitle_color);
			if (colorsRes != 0) subtitleView.setTextColor(AppResources.getInstance().getResources().getColorStateList(colorsRes));
			CharSequence title = getProvider().getAccountItem().getAccountFullname();
			if (title == null || title.length() == 0) 
				title = AppResources.getInstance().getStringText(AppResources.string.accountmenu_subtitle);
			if (subtitleView != null) subtitleView.setText(title);
		}
		
		final View topView = view.findViewById(R.id.accountmenu_header_header);
		if (topView != null) {
			int backgroundRes = getHeaderTopBackgroundRes();
			if (backgroundRes != 0) topView.setBackgroundResource(backgroundRes);
		}
		
		final View bottomView = view.findViewById(R.id.accountmenu_header_body);
		if (bottomView != null) {
			int backgroundRes = getHeaderBottomBackgroundRes();
			if (backgroundRes != 0) bottomView.setBackgroundResource(backgroundRes);
		}
		
		final View layoutView = view.findViewById(R.id.accountmenu_header);
		if (layoutView != null) {
			int imageWidth = menuactivity.getSlidingMenu().getMenuWidth();
			int imageHeight = (int)(AppResources.getInstance().getResources().getDimension(R.dimen.accountmenu_header_height) + 
					AppResources.getInstance().getResources().getDimension(R.dimen.accountmenu_body_height));
			
			Drawable d = getProvider().getAccountItem().getBackgroundDrawable(imageWidth, imageHeight);
			if (d == null) { 
				int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.accountmenu_background);
				if (backgroundRes != 0) {
					Drawable bg = menuactivity.getResources().getDrawable(backgroundRes);
					if (bg != null) d = new DelegatedDrawable(bg, imageWidth, imageHeight);
				}
			}
			if (d != null) { 
				DataBinder.onImageDrawablePreBind(d, layoutView);
				layoutView.setBackground(d);
				DataBinder.onImageDrawableBinded(d, false);
			}
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						menuactivity.showAccountProfile();
					}
				});
		}
	}

	public void bindFooterView(AccountMenuFragment fragment, View view, 
			Bundle savedInstanceState) {
		if (fragment == null || view == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("bindFooterView: fragment=" + fragment + " view=" + view);
		
		final AccountMenuActivity menuactivity = (AccountMenuActivity)fragment.getActivity();
		final AccountUser user = getProvider().getAccountItem().getAccountUser();
		
		TextView titleView = (TextView)view.findViewById(R.id.accountmenu_footer_title);
		if (titleView != null) {
			String text = menuactivity.getResources().getString(R.string.remaining_space_title);
			String space = Utilities.formatSize(user.getTotalRemainingSpace());
			CharSequence title = String.format(text, space);
			if (title == null || title.length() == 0) 
				title = AppResources.getInstance().getStringText(AppResources.string.accountmenu_footer_title);
			if (title != null) titleView.setText(title);
			
			int colorRes = AppResources.getInstance().getColorRes(AppResources.color.accountmenu_footer_title_color);
			if (colorRes == 0) colorRes = R.color.accountmenu_footer_title_color;
			int color = menuactivity.getResources().getColor(colorRes);
			titleView.setTextColor(color);
		}
		
		ImageView iconView = (ImageView)view.findViewById(R.id.accountmenu_footer_icon);
		if (iconView != null) {
			int colorRes = AppResources.getInstance().getColorRes(AppResources.color.accountmenu_footer_icon_color);
			if (colorRes == 0) colorRes = R.color.accountmenu_footer_icon_color;
			int color = menuactivity.getResources().getColor(colorRes);
			
			ChartDrawable chart = new ChartDrawable(ChartDrawable.CHART_PIE);
			chart.getBelowPaint().setColor(color);
			chart.getBelowPaint().setStrokeWidth(3.0f);
			chart.getAbovePaint().setColor(color);
			chart.getAbovePaint().setStrokeWidth(3.0f);
			chart.setStartAngle(-90.0f);
			chart.setPercent(user.getTotalUsedPercent());
			
			iconView.setImageDrawable(chart);
		}
		
		final View layoutView = view.findViewById(R.id.accountmenu_footer);
		if (layoutView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.card_list_selector);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						menuactivity.showAccountSpaces();
					}
				});
		}
	}
	
	public void bindBehindView(AccountMenuFragment fragment, View view, 
			Bundle savedInstanceState) {
	}
	
	public void bindAboveView(AccountMenuFragment fragment, View view, 
			Bundle savedInstanceState) {
	}
	
}
