package org.javenstudio.android.information;

import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;

public class InformationSourceBinder extends InformationBinder {

	public InformationSourceBinder() {}
	
	@Override
	protected int getColumnSize(IActivity activity) { 
		//final int colsize = activity.getResources().getInteger(R.integer.list_column_size);
		//return colsize > 1 ? colsize : 1;
		return 1;
	}
	
	@Override
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	public View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.information_list, container, false);
	}
	
	@Override
	public View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.information_list_listview);
	}
	
	@Override
	protected void setInformationTextView(IActivity activity, InformationOne one) { 
		final View view = one.getBindView();
		if (view == null) return;
		
		String title = one.getTitle();
		String text = one.getSummary();
		String content = one.getContent();
		String author = one.getAuthor();
		String date = one.getDate();
		
		if (date == null) date = "";
		if (author != null && author.length() > 0) 
			date = author + " " + date;
		
		if (content != null && content.length() > 0) {
			int contentLen = one.getContentLength();
			if (contentLen > 0)
				text = content;
		}
		
		if (title != null && title.length() > 30) { 
			if (text == null || text.length() == 0) 
				text = title;
			
			title = title.substring(0, 30) + 
					activity.getResources().getString(R.string.ellipsize_end);
		}
		
		CharSequence summary = null;
		
		//Object fieldAttr = one.getField(Information.ATTR_SUMMARYMODE);
		//if (fieldAttr != null && fieldAttr instanceof String) { 
		//	String summaryMode = (String)fieldAttr;
		//	if (summaryMode.equalsIgnoreCase(Information.SUMMARYMODE_CONTENT)) { 
		//		String content = one.getContent();
		//		if (content != null && content.length() > 0) 
		//			text = content;
		//	}
		//}
		
		if (summary == null) 
			summary = InformationHelper.formatContentSpanned(text);
		
		TextView titleView = (TextView)view.findViewById(R.id.information_item_title);
		if (titleView != null) { 
			if (title != null && title.length() > 0) {
				titleView.setText(InformationHelper.formatTitleSpanned(title));
				titleView.setVisibility(View.VISIBLE);
			} else
				titleView.setVisibility(View.GONE);
		}
		
		TextView dateView = (TextView)view.findViewById(R.id.information_item_date);
		if (dateView != null) 
			dateView.setText(date);
		
		TextView textView = (TextView)view.findViewById(R.id.information_item_text);
		if (textView != null) { 
			textView.setText(summary);
			textView.setAutoLinkMask(Linkify.ALL);
			textView.setLinksClickable(true);
			textView.setClickable(false);
		}
	}
	
}
