package org.javenstudio.android.information;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;

public class InformationListBinder extends InformationBinder {

	public InformationListBinder() {}
	
	@Override
	protected int getColumnSize(IActivity activity) { 
		final int colsize = activity.getResources().getInteger(R.integer.list_column_size);
		return colsize > 1 ? colsize : 1;
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
	protected int getInformationItemViewRes(IActivity activity) { 
		return R.layout.information_item; 
	}
	
	@Override
	protected int getItemViewHeight(IActivity activity, ViewGroup container, View view) { 
		if (getColumnSize(activity) > 1) 
			return (int)activity.getResources().getDimension(getItemViewHeightDimenRes(activity));
		
		return getDefaultItemViewHeight(activity, container, view);
	}
	
	protected int getItemViewHeightDimenRes(IActivity activity) { 
		return R.dimen.list_item_height; 
	}
	
	@Override
	protected void setInformationTextView(IActivity activity, InformationOne one) { 
		final View view = one.getBindView();
		if (view == null) return;
		
		String title = one.getTitle();
		String text = one.getSummary();
		String author = one.getAuthor();
		String date = one.getDate();
		
		if (date == null) date = "";
		if (author != null && author.length() > 0) 
			date = author + " " + date;
		
		if (title != null && title.length() > 30) { 
			if (text == null || text.length() == 0) 
				text = title;
			
			title = title.substring(0, 30) + 
					activity.getResources().getString(R.string.ellipsize_end);
		}
		
		CharSequence summary = null;
		//boolean hideContent = isHideContent(activity, one);
		
		//Object fieldAttr = one.getField(Information.ATTR_SUMMARYMODE);
		//if (fieldAttr != null && fieldAttr instanceof String) { 
		//	String summaryMode = (String)fieldAttr;
		//	if (summaryMode.equalsIgnoreCase(Information.SUMMARYMODE_SECTION)) {
		//		//summary = InformationHelper.formatContentSpanned(text);
		//		
		//	} else if (summaryMode.equalsIgnoreCase(Information.SUMMARYMODE_CONTENT)) { 
		//		String content = one.getContent();
		//		if (content != null && content.length() > 0) 
		//			text = content;
		//		
		//		//summary = InformationHelper.formatContentSpanned(text);
		//		
		//	} else if (summaryMode.equalsIgnoreCase(Information.SUMMARYMODE_NOCONTENT)) { 
		//		text = null;
		//		title = one.getTitle();
		//		hideContent = true;
		//	}
		//}
		
		if (summary == null && text != null) 
			summary = InformationHelper.formatSummarySpanned(text);
		
		TextView titleView = (TextView)view.findViewById(R.id.information_item_title);
		if (titleView != null) 
			titleView.setText(InformationHelper.formatTitleSpanned(title));
		
		TextView dateView = (TextView)view.findViewById(R.id.information_item_date);
		if (dateView != null)
			dateView.setText(date);
		
		TextView textView = (TextView)view.findViewById(R.id.information_item_text);
		if (textView != null) { 
			textView.setText(summary);
			//textView.setAutoLinkMask(Linkify.ALL);
			//textView.setClickable(false);
		}
		
		onInformationTextBinded(activity, one, titleView, textView);
	}
	
	protected void onInformationTextBinded(IActivity activity, InformationOne one, 
			TextView titleView, TextView textView) { 
		if (titleView != null) { 
			titleView.setMinLines(1);
			titleView.setMaxLines(3);
		}
		
		if (textView != null) 
			textView.setMaxLines(5);
	}
	
}
