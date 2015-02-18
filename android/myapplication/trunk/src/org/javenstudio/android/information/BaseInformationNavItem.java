package org.javenstudio.android.information;

import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.cocoka.widget.model.NavigationInfo;

public abstract class BaseInformationNavItem extends InformationNavItem {

	private final NavBinder mBinder;
	private NavModel mModel = null; 
	
	public BaseInformationNavItem(NavBinder res, NavigationInfo info, 
			boolean selected) { 
		super(info, selected); 
		mBinder = res;
	}
	
	protected NavModel getModel() { 
		return mModel;
	}
	
	@Override
	public final IMenuOperation getMenuOperation() { 
		return mBinder.getMenuOperation(); 
	}
	
	@Override
	public final InformationBinder getBinder() { 
		return mBinder.getBinder(this);
	}
	
	@Override 
	public final void onFetched(NavModel model, String location, String content, boolean first) { 
		if (model == null || content == null) return; 
		
		mModel = model; 
		onAddInformationBegin(model, location, first); 
		parseInformation(location, content, first);
		onAddInformationEnd(model, location, first); 
	}
	
	protected abstract void parseInformation(String location, String content, boolean first);
	
}
