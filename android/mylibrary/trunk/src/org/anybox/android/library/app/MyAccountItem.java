package org.anybox.android.library.app;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.anybox.android.library.R;
import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.cocoka.widget.AdvancedAdapter;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.list.AccountListAdapter;
import org.javenstudio.provider.account.list.AccountListDataSet;
import org.javenstudio.provider.account.list.AccountListDataSets;
import org.javenstudio.provider.account.list.AccountListFactory;
import org.javenstudio.provider.account.list.AccountListItem;
import org.javenstudio.provider.account.list.AccountListItemBase;

public abstract class MyAccountItem extends AccountListItemBase {
	private static final Logger LOG = Logger.getLogger(MyAccountItem.class);

	public static AccountListAdapter getListAdapter(final IActivity activity, 
			final AccountApp app) {
		if (activity == null || app == null) return null;
		if (LOG.isDebugEnabled()) 
			LOG.debug("getListAdapter: activity=" + activity + " app=" + app);
		
		final AccountListDataSets dataSets = new AccountListDataSets();
		
		final AccountListFactory factory = new AccountListFactory() {
				private final AccountHelper.OnRemoveListener mListener = 
					new AccountHelper.OnRemoveListener() {
						@Override
						public void onAccountRemoved(AccountData account, boolean success) {
							updateAccountListDataSets(app, dataSets);
						}
					};
				
				@Override
				public AccountListItem createAccountListItem(AccountApp app, AccountData account) {
					return new MyAccountItem(app, account) {
							@Override
							protected AccountHelper.OnRemoveListener getRemoveListener() {
								return mListener;
							}
						};
				}
			};
		
		factory.updateAccountListDataSets(app, dataSets);
		
		final AccountListAdapter adapter = new AccountListAdapter(activity.getActivity(), dataSets, 
				R.layout.register_accountitem, 0);
		
		adapter.setViewBinder(new AdvancedAdapter.ViewBinder() {
				@Override
				public void onViewBinded(AdvancedAdapter.DataSet dataSet, View view, int position) {
					AccountListDataSet ads = (AccountListDataSet)dataSet;
					AccountListItem item = ads != null ? ads.getAccountListItem() : null;
					if (item != null) item.bindViews(activity, view);
				}
			});
		
		return adapter;
	}
	
	private MyAccountItem(AccountApp app, AccountData account) {
		super(app, account);
	}

	@Override
	public int getViewRes() {
		return R.layout.register_accountitem;
	}

	@Override
	public void bindViews(final IActivity activity, View view) {
		if (activity == null || view == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("bindViews: item=" + this + " activity=" + activity 
					+ " view=" + view);
		}
		
		final ImageView avatarView = (ImageView)view.findViewById(R.id.login_accountitem_user_avatar);
		if (avatarView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.accountmenu_avatar_round_selector);
			if (backgroundRes != 0) avatarView.setBackgroundResource(backgroundRes);
			
			int size = AppResources.getInstance().getContext().getResources().getDimensionPixelSize(R.dimen.login_accountitem_avatar_size);
			Drawable d = getAvatarDrawable(size, 0);
			if (d != null) { 
				onImageDrawablePreBind(d, avatarView);
				avatarView.setImageDrawable(d);
				onImageDrawableBinded(d, false);
			}
		}
		
		final ImageView actionView = (ImageView)view.findViewById(R.id.login_accountitem_action_image);
		if (actionView != null) {
			int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.setting_account_action_icon_remove);
			if (iconRes != 0) actionView.setImageResource(iconRes);
			
			actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onAccountRemove(activity);
					}
				});
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.login_accountitem_title);
		if (titleView != null) {
			titleView.setText(getAccount().getFullName());
		}
		
		final TextView textView = (TextView)view.findViewById(R.id.login_accountitem_text);
		if (textView != null) {
			String text = AppResources.getInstance().getContext().getString(R.string.login_timeago_message);
			String timeago = AppResources.getInstance().formatTimeAgo(
					System.currentTimeMillis() - getAccount().getUpdateTime());
			textView.setText(String.format(text, timeago));
		}
		
		view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onAccountClick(activity);
				}
			});
	}
	
}
