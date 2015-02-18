package org.javenstudio.provider.account.notify;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;

public class AnnouncementNotifyItem extends NotifyItem {

	private final IAnnouncementNotifyData mData;
	
	public AnnouncementNotifyItem(NotifyProvider provider, 
			IAnnouncementNotifyData data) {
		super(provider);
		mData = data;
	}

	public IAnnouncementNotifyData getData() { return mData; }
	
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
			CharSequence title = AppResources.getInstance().getStringText(AppResources.string.notify_announcement_title);
			if (title == null || title.length() == 0)
				title = AppResources.getInstance().getResources().getString(R.string.notify_announcement_title);
			IAnnouncementNotifyData data = getData();
			if (data != null) 
				title = "" + title + " (" + data.getTotalCount() + ")";
			titleView.setText(title);
		}
		
		final ViewGroup bodyView = (ViewGroup)view.findViewById(R.id.notify_item_body);
		if (bodyView != null) {
			IAnnouncementNotifyData data = getData();
			if (data != null) {
				//int totalCount = data.getTotalCount();
				//int showCount = 0;
				
				IAnnouncementNotifyData.IAnnouncementItem[] items = data.getItems();
				if (items != null) {
					for (IAnnouncementNotifyData.IAnnouncementItem item : items) {
						if (item != null) {
							addView(item, bodyView);
							//showCount ++;
						}
					}
				}
			}
		}
	}

	protected void addView(IAnnouncementNotifyData.IAnnouncementItem data, ViewGroup container) {
		if (data == null || container == null) return;
		
		LayoutInflater inflater = LayoutInflater.from(AppResources.getInstance().getContext());
		View view = inflater.inflate(R.layout.notify_announcement_item, null);
		
		final ImageView posterView = (ImageView)view.findViewById(R.id.notify_announcement_poster_image);
		if (posterView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.notify_avatar_background);
			if (backgroundRes != 0) posterView.setBackgroundResource(backgroundRes);
			
			int size = AppResources.getInstance().getResources().getDimensionPixelSize(R.dimen.notify_item_avatar_size);
			Drawable d = data.getPosterDrawable(size, 0);
			if (d != null) { 
				onImageDrawablePreBind(d, posterView);
				posterView.setImageDrawable(d);
				onImageDrawableBinded(d, false);
			}
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.notify_announcement_title);
		if (titleView != null) {
			String txt = data.getAnnouncementTitle();
			titleView.setText(txt);
		}
		
		final TextView textView = (TextView)view.findViewById(R.id.notify_announcement_text);
		if (textView != null) {
			String txt = formatBody(data.getAnnouncementBody());
			textView.setText(txt);
		}
		
		//final TextView dateView = (TextView)view.findViewById(R.id.notify_announcement_date);
		//if (dateView != null) {
		//	String txt = AppResources.getInstance().formatTimeAgo(
		//			System.currentTimeMillis() - data.getPublishTime());
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
