package org.javenstudio.provider.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.account.AccountWork;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.notify.NotifyBinder;
import org.javenstudio.provider.account.notify.NotifyProvider;
import org.javenstudio.provider.account.notify.OnNotifyChangeListener;
import org.javenstudio.provider.activity.AccountMenuActivity;
import org.javenstudio.provider.activity.AccountSecondaryMenuFragment;

public abstract class AccountSecondaryMenuBinder extends DataBinder {
	private static final Logger LOG = Logger.getLogger(AccountSecondaryMenuBinder.class);

	public abstract AccountInfoProvider getProvider();
	
	private int mVisibility = View.GONE;
	public int getVisibility() { return mVisibility; }
	
	private boolean mOpened = false;
	
	public void onSecondaryMenuScrolled(AccountSecondaryMenuFragment fragment, 
			View view, float percentOpen) {
		if (percentOpen > 0.95f) {
			boolean opened = mOpened == false;
			mOpened = true;
			
			if (opened) {
				AccountUser user = getProvider().getAccountApp().getAccount();
				if (user != null) {
					NotifyProvider provider = user.getNotifyProvider();
					if (provider != null) 
						provider.onMenuOpened(fragment);
				}
			}
		} else {
			mOpened = false;
		}
	}
	
	public void onSecondaryMenuVisibilityChanged(AccountSecondaryMenuFragment fragment, 
			View view, int visibility) {
		if (fragment == null || view == null) return;
		mVisibility = visibility;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onSecondaryMenuVisibilityChanged: fragment=" + fragment + " view=" + view 
					+ " visibility=" + visibility);
		}
		
		if (visibility == View.VISIBLE) {
			setSecondaryMenuListAdapter(fragment);
			bindHeaderView(fragment, view, null);
			bindFooterView(fragment, view, null);
			bindBehindView(fragment, view, null);
			bindAboveView(fragment, view, null);
		}
	}
	
	public void setSecondaryMenuListAdapter(AccountSecondaryMenuFragment fragment) {
		if (fragment == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("setSecondaryMenuListAdapter: fragment=" + fragment);
		
		ListAdapter adapter = null;
		AccountUser user = getProvider().getAccountApp().getAccount();
		if (user != null) {
			NotifyProvider provider = user.getNotifyProvider();
			if (provider != null) {
				NotifyBinder binder = provider.getBinder();
				if (binder != null) {
					adapter = binder.createListAdapter((IActivity)fragment.getActivity());
				}
			}
		}
		
		fragment.setListAdapter(adapter);
	}
	
	public void onSecondaryMenuListItemClick(AccountSecondaryMenuFragment fragment, int position) {
		if (fragment == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("onSecondaryMenuListItemClick: fragment=" + fragment 
					+ " position=" + position);
		}
	}
	
	public View createHeaderView(AccountSecondaryMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return null; //inflater.inflate(R.layout.account_secondheader, null);
	}

	public View createFooterView(AccountSecondaryMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.account_secondfooter, null);
	}
	
	public View createBehindView(AccountSecondaryMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return null;
	}
	
	public View createAboveView(AccountSecondaryMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return null; //inflater.inflate(R.layout.account_secondabove, null);
	}
	
	public void onViewCreated(AccountSecondaryMenuFragment fragment, 
			View view, Bundle savedInstanceState) {
	}
	
	public void onActivityCreated(AccountSecondaryMenuFragment fragment, 
			Bundle savedInstanceState) {
		if (fragment == null) return;
		bindHeaderView(fragment, fragment.getView(), savedInstanceState);
		bindFooterView(fragment, fragment.getView(), savedInstanceState);
		bindBehindView(fragment, fragment.getView(), savedInstanceState);
		bindAboveView(fragment, fragment.getView(), savedInstanceState);
	}
	
	public void bindHeaderView(AccountSecondaryMenuFragment fragment, final View view, 
			Bundle savedInstanceState) {
		if (fragment == null || view == null) return;
	}

	public void bindFooterView(AccountSecondaryMenuFragment fragment, final View view, 
			final Bundle savedInstanceState) {
		if (fragment == null || view == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("bindFooterView: fragment=" + fragment + " view=" + view);
		
		final AccountUser user = getProvider().getAccountItem().getAccountUser();
		final AccountWork work = getProvider().getAccountItem().getAccountApp().getWork();
		
		updateFooterView(fragment, view);
		
		final View layoutView = view.findViewById(R.id.accountmenu_secondfooter);
		if (layoutView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.card_list_selector);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						work.scheduleWork(user, AccountWork.WorkType.ACCOUNTINFO, 0);
					}
				});
		}
		
		work.removeListener(mListener);
		WorkListener listener = new WorkListener(fragment, view);
		mListener = listener;
		work.addListener(getProvider().getAccountUser(), listener);
		getProvider().getAccountUser().setOnNotifyChangeListener(listener);
	}
	
	protected void updateFooterView(AccountSecondaryMenuFragment fragment, final View view) {
		if (fragment == null || view == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("updateFooterView: fragment=" + fragment + " view=" + view);
		
		final AccountMenuActivity menuactivity = (AccountMenuActivity)fragment.getActivity();
		final AccountUser user = getProvider().getAccountItem().getAccountUser();
		final AccountWork work = getProvider().getAccountItem().getAccountApp().getWork();
		
		TextView titleView = (TextView)view.findViewById(R.id.accountmenu_secondfooter_title);
		if (titleView != null) {
			CharSequence title = null;
			if (work.isWorkRunning(user, AccountWork.WorkType.ACCOUNTINFO)) {
				title = AppResources.getInstance().getStringText(AppResources.string.accountmenu_secondfooter_loading_title);
				if (title == null || title.length() == 0) {
					title = menuactivity.getResources().getString(R.string.loading_title);
				}
			} else {
				title = AppResources.getInstance().getStringText(AppResources.string.accountmenu_secondfooter_title);
				if (title == null || title.length() == 0) {
					String text = menuactivity.getResources().getString(R.string.updated_timeago_title);
					String time = AppResources.getInstance().formatTimeAgo(System.currentTimeMillis() - user.getUpdateTime());
					title = String.format(text, time);
				}
			}
			if (title != null) titleView.setText(title);
			
			int colorRes = AppResources.getInstance().getColorRes(AppResources.color.accountmenu_footer_title_color);
			if (colorRes == 0) colorRes = R.color.accountmenu_footer_title_color;
			int color = menuactivity.getResources().getColor(colorRes);
			titleView.setTextColor(color);
		}
		
		View iconView = view.findViewById(R.id.accountmenu_secondfooter_icon);
		View progressView = view.findViewById(R.id.accountmenu_secondfooter_progressbar);
		if (work.isWorkRunning(user, AccountWork.WorkType.ACCOUNTINFO)) {
			if (iconView != null) iconView.setVisibility(View.GONE);
			if (progressView != null) progressView.setVisibility(View.VISIBLE);
		} else {
			if (iconView != null) iconView.setVisibility(View.VISIBLE);
			if (progressView != null) progressView.setVisibility(View.GONE);
		}
	}
	
	public void bindBehindView(AccountSecondaryMenuFragment fragment, View view, 
			Bundle savedInstanceState) {
		if (fragment == null || view == null) return;
	}
	
	public void bindAboveView(AccountSecondaryMenuFragment fragment, View view, 
			Bundle savedInstanceState) {
		if (fragment == null || view == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("bindAboveView: fragment=" + fragment + " view=" + view);
		
		updateAboveView(fragment, view);
	}
	
	protected void updateAboveView(AccountSecondaryMenuFragment fragment, View view) {
		if (fragment == null || view == null) return;
	}
	
	private WorkListener mListener = null;
	
	private class WorkListener implements AccountWork.OnStateChangeListener, 
			OnNotifyChangeListener {
		private final AccountSecondaryMenuFragment mFragment;
		private final View mView;
		
		public WorkListener(AccountSecondaryMenuFragment fragment, View view) {
			mFragment = fragment;
			mView = view;
		}
		
		@Override
		public void onStateChanged(AccountUser user, AccountWork.WorkType type, 
				AccountWork.WorkState state) {
			if (user == null || type == null || state == null)
				return;
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("onStateChanged: account=" + user 
						+ " type=" + type + " state=" + state);
			}
			
			AccountUser account = getProvider().getAccountItem().getAccountUser();
			if (user != account) return;
			
			updateFooterViews();
		}
		
		private void updateFooterViews() {
			final AccountSecondaryMenuFragment fragment = mFragment;
			final View view = mView;
			if (fragment == null || view == null)
				return;
			
			if (getVisibility() == View.VISIBLE && !fragment.getActivity().isDestroyed()) {
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() {
							AccountSecondaryMenuBinder.this.updateFooterView(fragment, view);
						}
					});
			}
		}

		@Override
		public void onNotifyChanged(AccountUser user, int count) {
			if (user == null) return;
			if (LOG.isDebugEnabled())
				LOG.debug("onNotifyChanged: account=" + user + " count=" + count);
			
			AccountUser account = getProvider().getAccountItem().getAccountUser();
			if (user != account) return;
			
			updateAboveViews();
		}
		
		private void updateAboveViews() {
			final AccountSecondaryMenuFragment fragment = mFragment;
			final View view = mView;
			if (fragment == null || view == null)
				return;
			
			if (getVisibility() == View.VISIBLE && !fragment.getActivity().isDestroyed()) {
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() {
							AccountSecondaryMenuBinder.this.updateAboveView(fragment, view);
						}
					});
			}
		}
	}
	
}
