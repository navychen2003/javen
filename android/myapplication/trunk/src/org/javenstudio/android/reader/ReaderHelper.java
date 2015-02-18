package org.javenstudio.android.reader;

import org.javenstudio.cocoka.android.ModuleManager;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.cocoka.widget.model.NavigationItem;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.information.InformationBinderFactory;
import org.javenstudio.android.information.InformationRegistry;
import org.javenstudio.android.information.subscribe.SubscribeHelper;
import org.javenstudio.android.information.subscribe.SubscribeNavGroup;
import org.javenstudio.android.information.subscribe.SubscribeNavItem;

public final class ReaderHelper {

	public static class OpmlSubscribeNavItemFacoty implements SubscribeNavItem.Factory { 
		@Override
		public SubscribeNavItem create(NavigationInfo info, boolean selected) { 
			return InformationBinderFactory.getInstance().createSubscribeNavItem(info, selected); 
		}
	}
	
	public static void registerOpml(String displayName, final String location) { 
		final SubscribeNavGroup news = new SubscribeNavGroup(new NavigationInfo(displayName)); 
		news.setOnSelectedListener(new NavigationItem.OnSelectedListener() {
				@Override
				public void onSelected(NavigationItem item, boolean selected) {
					if (selected && news.getChildCount() == 0) { 
						SubscribeHelper.scheduleLoadSubscribeItems(news, 
								new OpmlSubscribeNavItemFacoty(), 
								location); 
					}
				}
			});
		InformationRegistry.addNavigationItem(news); 
	}
	
	public static String normalizeInformationLocation(String location) { 
		ModuleManager.GlobalMethod method = ResourceHelper.getModuleManager()
				.getGlobalMethod("normalizeInformationLocation");
		
		if (method != null && method.existMethod()) 
			return (String)method.invoke(location);
		
		return location;
	}
	
	public static String[] getInformationLocationList(String location) { 
		ModuleManager.GlobalMethod method = ResourceHelper.getModuleManager()
				.getGlobalMethod("getInformationLocationList");
		
		if (method != null && method.existMethod()) 
			return (String[])method.invoke(location);
		
		return null;
	}
	
	public static int getInformationItemType(String location) { 
		ModuleManager.GlobalMethod method = ResourceHelper.getModuleManager()
				.getGlobalMethod("getInformationItemType");
		
		if (method != null && method.existMethod()) { 
			Number number = (Number)method.invoke(location);
			if (number != null) 
				return number.intValue();
		}
		
		return 0;
	}
	
	public static String normalizeImageLocation(String location) { 
		ModuleManager.GlobalMethod method = ResourceHelper.getModuleManager()
				.getGlobalMethod("normalizeImageLocation");
		
		if (method != null && method.existMethod()) 
			return (String)method.invoke(location);
		
		return location;
	}
	
	public static final int ACTION_IGNORE_IMAGE = 1;
	
	public static void addInformationSourceAction(String location, int action) { 
		switch (action) { 
		case ACTION_IGNORE_IMAGE: 
			HttpImageItem.addIgnorePattern(location);
			break;
		default: 
			break;
		}
	}
	
}
