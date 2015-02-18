package org.javenstudio.provider.publish.comment;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class CommentDataSet extends AbstractDataSet<CommentItem> {

	public CommentDataSet(CommentDataSets dataSets, CommentItem data) {
		super(dataSets, data); 
		
		if (data != null) 
			data.setDataSet(this);
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public CommentItem getCommentItem() { 
		return (CommentItem)getObject(); 
	}
	
}
