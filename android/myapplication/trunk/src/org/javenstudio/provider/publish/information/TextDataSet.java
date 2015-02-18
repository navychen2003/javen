package org.javenstudio.provider.publish.information;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class TextDataSet extends AbstractDataSet<TextItem> {

	public TextDataSet(TextDataSets dataSets, TextItem data) {
		super(dataSets, data); 
		
		if (data != null) 
			data.setDataSet(this);
	}
	
	@Override 
	public boolean isEnabled() { 
		return getTextItem().getProvider().getOnItemClickListener() != null;
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public TextItem getTextItem() { 
		return (TextItem)getObject(); 
	}
	
}
