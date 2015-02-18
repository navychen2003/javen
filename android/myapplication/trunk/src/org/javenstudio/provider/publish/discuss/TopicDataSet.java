package org.javenstudio.provider.publish.discuss;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class TopicDataSet extends AbstractDataSet<TopicItem> {

	public TopicDataSet(TopicDataSets dataSets, TopicItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getTopicItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public TopicItem getTopicItem() { 
		return (TopicItem)getObject(); 
	}
	
}
