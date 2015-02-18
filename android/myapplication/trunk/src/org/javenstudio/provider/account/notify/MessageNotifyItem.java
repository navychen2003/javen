package org.javenstudio.provider.account.notify;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;

public class MessageNotifyItem extends NotifyItem {

	private final IMessageNotifyData mData;
	
	public MessageNotifyItem(NotifyProvider provider, 
			IMessageNotifyData data) {
		super(provider);
		mData = data;
	}

	public IMessageNotifyData getData() { return mData; }
	
	@Override
	public int getViewRes() {
		return R.layout.notify_item;
	}

	@Override
	public void bindView(View view) {
		if (view == null) return;
		
		final View headerView = view.findViewById(R.id.notify_item_header);
		if (headerView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.notify_header_background);
			if (backgroundRes != 0) headerView.setBackgroundResource(backgroundRes);
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.notify_item_title);
		if (titleView != null) {
			CharSequence title = AppResources.getInstance().getStringText(AppResources.string.notify_message_title);
			if (title == null || title.length() == 0)
				title = AppResources.getInstance().getResources().getString(R.string.notify_message_title);
			IMessageNotifyData data = getData();
			if (data != null) 
				title = "" + title + " (" + data.getTotalCount() + ")";
			titleView.setText(title);
		}
		
		final ViewGroup bodyView = (ViewGroup)view.findViewById(R.id.notify_item_body);
		if (bodyView != null) {
			IMessageNotifyData data = getData();
			if (data != null) {
				int totalCount = data.getTotalCount();
				int showCount = 0;
				
				IMessageNotifyData.IMessageItem[] items = data.getItems();
				if (items != null) {
					for (IMessageNotifyData.IMessageItem item : items) {
						if (item != null) {
							addView(item, bodyView);
							showCount ++;
						}
					}
				}
				
				addMoreView(bodyView, totalCount - showCount);
			}
		}
	}

	protected void addMoreView(ViewGroup container, int count) {
		if (container == null || count <= 0)
			return;
		
		LayoutInflater inflater = LayoutInflater.from(AppResources.getInstance().getContext());
		View view = inflater.inflate(R.layout.notify_more_item, null);
		
		final TextView titleView = (TextView)view.findViewById(R.id.notify_more_title);
		if (titleView != null) {
			int titleRes = AppResources.getInstance().getStringRes(AppResources.string.plurals_notify_moremessage_title);
			if (titleRes == 0) titleRes = R.plurals.plurals_notify_moremessage_title;
			String text = AppResources.getInstance().getResources().getQuantityString(titleRes, count);
			String title = String.format(text, ""+count);
			titleView.setText(title);
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.card_list_selector);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
					}
				});
		}
		
		container.addView(view, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}
	
	protected void addView(IMessageNotifyData.IMessageItem data, ViewGroup container) {
		if (data == null || container == null) return;
		
		LayoutInflater inflater = LayoutInflater.from(AppResources.getInstance().getContext());
		View view = inflater.inflate(R.layout.notify_message_item, null);
		
		final ImageView avatarView = (ImageView)view.findViewById(R.id.notify_message_user_avatar);
		if (avatarView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.notify_avatar_background);
			if (backgroundRes != 0) avatarView.setBackgroundResource(backgroundRes);
			
			int size = AppResources.getInstance().getResources().getDimensionPixelSize(R.dimen.notify_item_avatar_size);
			Drawable d = data.getAvatarDrawable(size, 0);
			if (d != null) { 
				onImageDrawablePreBind(d, avatarView);
				avatarView.setImageDrawable(d);
				onImageDrawableBinded(d, false);
			}
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.notify_message_title);
		if (titleView != null) {
			String txt = data.getMessageTitle();
			titleView.setText(txt);
		}
		
		final TextView textView = (TextView)view.findViewById(R.id.notify_message_text);
		if (textView != null) {
			String txt = formatBody(data.getMessageBody());
			textView.setText(txt);
		}
		
		//final TextView dateView = (TextView)view.findViewById(R.id.notify_message_date);
		//if (dateView != null) {
		//	String txt = AppResources.getInstance().formatTimeAgo(
		//			System.currentTimeMillis() - data.getMessageTime());
		//	dateView.setText(txt);
		//}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.card_list_selector);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
					}
				});
		}
		
		container.addView(view, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}
	
}
