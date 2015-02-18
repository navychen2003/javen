package org.javenstudio.provider.publish.discuss;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class ReplyDataSet extends AbstractDataSet<ReplyItem> {

	public ReplyDataSet(ReplyDataSets dataSets, ReplyItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getReplyItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public ReplyItem getReplyItem() { 
		return (ReplyItem)getObject(); 
	}
	
}
