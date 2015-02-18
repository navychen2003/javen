package org.javenstudio.android.information;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class InformationDataSet extends AbstractDataSet<Information> {

	public InformationDataSet(InformationDataSets dataSets, Information data) {
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
	
	public Information getInformation() { 
		return (Information)getObject(); 
	}

}
