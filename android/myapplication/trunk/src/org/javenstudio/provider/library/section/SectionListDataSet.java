package org.javenstudio.provider.library.section;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class SectionListDataSet extends AbstractDataSet<SectionListItem> {

	public SectionListDataSet(SectionListDataSets dataSets, SectionListItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getSectionListItem().getProvider().getOnItemClickListener() != null; 
	}

	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public SectionListItem getSectionListItem() { 
		return (SectionListItem)getObject(); 
	}
	
}
