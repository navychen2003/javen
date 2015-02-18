package org.javenstudio.android.reader;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import android.content.Context;

import org.javenstudio.cocoka.android.MainMethods;
import org.javenstudio.cocoka.widget.model.NavigationGroup;
import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.cocoka.widget.model.NavigationItem;
import org.javenstudio.android.information.InformationBinderFactory;
import org.javenstudio.android.information.InformationRegistry;
import org.javenstudio.android.information.InformationSource;
import org.javenstudio.android.information.comment.CommentListBase;

public final class ReaderMethods {

	public static void registerMethods() { 
		final Class<?> clazz = ReaderMethods.class;
		
		MainMethods.registerMethod(clazz, "newNavigationGroup", 
				String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "addSubscribeRssItem", 
				Object.class, String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "addSubscribeLargeRssItem", 
				Object.class, String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "addSubscribePhotoRssItem", 
				Object.class, String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "addSubscribeFeedItem", 
				Object.class, String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "addSubscribeLargeFeedItem", 
				Object.class, String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "addSubscribePhotoFeedItem", 
				Object.class, String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "setDefaultNavigationItem", 
				String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "addSubscribeDefaultItem", 
				Object.class);
		
		MainMethods.registerMethod(clazz, "addNavigationItem", 
				Object.class);
		
		MainMethods.registerMethod(clazz, "addSubscribeOpmlGroup", 
				String.class, String.class);
		
		MainMethods.registerMethod(clazz, "addCommentItem", 
				Object.class, Object.class, String.class, Map.class);
		
		MainMethods.registerMethod(clazz, "addInformationSource", 
				String.class, Map.class, Object.class);
		
		MainMethods.registerMethod(clazz, "addPhotoSource", 
				String.class, Map.class, Object.class);
		
		MainMethods.registerMethod(clazz, "addInformationSourceAction", 
				String.class, int.class);
		
		MainMethods.registerMethod(clazz, "addPhotoSourceAction", 
				String.class, int.class);
	}
	
	public static final String ATTR_GROUPNAME = "groupName"; //Information.ATTR_GROUPNAME;
	public static final String ATTR_NAME = "name"; //Information.ATTR_NAME;
	public static final String ATTR_TITLE = "title"; //Information.ATTR_TITLE;
	public static final String ATTR_SUBTITLE = "subTitle"; //Information.ATTR_SUBTITLE;
	public static final String ATTR_DROPDOWNTITLE = "dropdownTitle"; //Information.ATTR_DROPDOWNTITLE;
	public static final String ATTR_DEFAULTCHARSET = "defaultCharset"; //Information.ATTR_DEFAULTCHARSET;
	
	public static final String ATTR_LOCATION = "reader.location"; //Information.ATTR_LOCATION;
	public static final String ATTR_ICONRES = "reader.iconRes"; //Information.ATTR_ICONRES;
	
	static Map<String,Object> newAttrs(Context context, String... paramArr) { 
		return newAttrs(context, 0, paramArr);
	}
	
	static Map<String,Object> newAttrs(Context context, int iconRes, String... paramArr) { 
		Map<String,Object> attrs = new HashMap<String,Object>();
		
		if (paramArr != null) { 
			for (String params : paramArr) {
				if (params == null) continue;
				
				StringTokenizer st = new StringTokenizer(params, "&");
				while (st.hasMoreTokens()) { 
					String param = st.nextToken();
					if (param != null && param.length() > 0) { 
						int pos = param.indexOf('=');
						if (pos > 0) { 
							String name = param.substring(0, pos); 
							String value = param.substring(pos + 1);
							
							if (name != null && name.length() > 0 && value != null && value.length() > 0) {
								//if (LOG.isDebugEnabled())
								//	LOG.debug("add Attr: " + name + "=" + value);
								
								attrs.put(name, value);
							}
						}
					}
				}
			}
		}
		
		if (iconRes != 0) 
			attrs.put(ATTR_ICONRES, iconRes);
		
		return attrs;
	}
	
	public static String newAttr(String name, String value) { 
		if (name != null && name.length() > 0 && value != null && value.length() > 0) 
			return name + "=" + value;
		else
			return null;
	}
	
	public static Object newNavigationGroup(Context context, String name, int iconRes) { 
		return newNavigationGroup(name, newAttrs(context, iconRes, 
				newAttr(ATTR_NAME, name)
			));
	}
	
	public static Object newNavigationGroup(Context context, String groupName, String name, int iconRes) { 
		return newNavigationGroup(name, newAttrs(context, iconRes, 
				newAttr(ATTR_GROUPNAME, groupName), 
				newAttr(ATTR_NAME, name)
			));
	}
	
	public static NavigationGroup newNavigationGroup(String name, Map<String,Object> attrs) { 
		return new NavigationGroup(new NavigationInfo(name, attrs));
	}
	
	public static void addSubscribeRssItem(Context context, Object group, 
			String name, String title, String subtitle, String dropdownTitle, 
			String location, int iconRes) { 
		addSubscribeRssItem(context, group, name, title, subtitle, dropdownTitle, 
				location, iconRes, null);
	}
	
	public static void addSubscribeRssItem(Context context, Object group, 
			String name, String title, String subtitle, String dropdownTitle, 
			String location, int iconRes, String params) { 
		addSubscribeRssItem(group, name, newAttrs(context, iconRes, 
				newAttr(ATTR_NAME, name), 
				newAttr(ATTR_TITLE, title), 
				newAttr(ATTR_SUBTITLE, subtitle), 
				newAttr(ATTR_DROPDOWNTITLE, dropdownTitle), 
				newAttr(ATTR_LOCATION, location), 
				params
			));
	}
	
	public static void addSubscribeRssItem(Object group, String name, Map<String,Object> attrs) { 
		final NavigationGroup subscribe = (NavigationGroup)group;
		
		subscribe.addChild(InformationBinderFactory.getInstance().createSubscribeNavItem(
				new NavigationInfo(name, attrs), false));
	}
	
	public static void addSubscribeLargeRssItem(Object group, String name, Map<String,Object> attrs) { 
		final NavigationGroup subscribe = (NavigationGroup)group;
		
		subscribe.addChild(InformationBinderFactory.getInstance().createSubscribeLargeNavItem(
				new NavigationInfo(name, attrs), false));
	}
	
	public static void addSubscribePhotoRssItem(Object group, String name, Map<String,Object> attrs) { 
		final NavigationGroup subscribe = (NavigationGroup)group;
		
		subscribe.addChild(InformationBinderFactory.getInstance().createSubscribePhotoNavItem(
				new NavigationInfo(name, attrs), false));
	}
	
	public static void addSubscribeFeedItem(Context context, Object group, 
			String name, String title, String subtitle, String dropdownTitle, 
			String location, int iconRes) { 
		addSubscribeFeedItem(context, group, name, title, subtitle, dropdownTitle, 
				location, iconRes, null);
	}
	
	public static void addSubscribeFeedItem(Context context, Object group, 
			String name, String title, String subtitle, String dropdownTitle, 
			String location, int iconRes, String params) { 
		addSubscribeFeedItem(group, name, newAttrs(context, iconRes, 
				newAttr(ATTR_NAME, name), 
				newAttr(ATTR_TITLE, title), 
				newAttr(ATTR_SUBTITLE, subtitle), 
				newAttr(ATTR_DROPDOWNTITLE, dropdownTitle), 
				newAttr(ATTR_LOCATION, location), 
				params
			));
	}
	
	public static void addSubscribeFeedItem(Object group, String name, Map<String,Object> attrs) { 
		final NavigationGroup subscribe = (NavigationGroup)group;
		
		subscribe.addChild(InformationBinderFactory.getInstance().createFeedNavItem(
				new NavigationInfo(name, attrs), false));
	}
	
	public static void addSubscribeLargeFeedItem(Object group, String name, Map<String,Object> attrs) { 
		final NavigationGroup subscribe = (NavigationGroup)group;
		
		subscribe.addChild(InformationBinderFactory.getInstance().createFeedLargeNavItem(
				new NavigationInfo(name, attrs), false));
	}
	
	public static void addSubscribePhotoFeedItem(Object group, String name, Map<String,Object> attrs) { 
		final NavigationGroup subscribe = (NavigationGroup)group;
		
		subscribe.addChild(InformationBinderFactory.getInstance().createFeedPhotoNavItem(
				new NavigationInfo(name, attrs), false));
	}
	
	public static void setDefaultNavigationItem(String name, Map<String,Object> attrs) { 
		InformationRegistry.setDefaultNavigationItem(
				InformationBinderFactory.getInstance().createDefaultNavItem(
						new NavigationInfo(name, attrs), false));
	}
	
	public static void addSubscribeDefaultItem(Object group) { 
		final NavigationGroup subscribe = (NavigationGroup)group;
		
		subscribe.addChild(InformationRegistry.getDefaultNavigationItem());
	}
	
	public static void addNavigationItem(Object group) { 
		InformationRegistry.addNavigationItem((NavigationItem)group); 
	}
	
	public static void addSubscribeOpmlGroup(String displayName, String location) { 
		ReaderHelper.registerOpml(displayName, location);
	}
	
	public static void addCommentItem(Object group, Object commentList, 
			String name, Map<String,Object> attrs) { 
		final NavigationGroup forum = (NavigationGroup)group;
		final CommentListBase list = (CommentListBase)commentList;
		
		forum.addChild(list.newNavItem(new NavigationInfo(name, attrs)));
	}
	
	public static void addInformationSource(final String location, 
			final Map<String,Object> attrs, final Object moduleClass) { 
		InformationRegistry.addSourceFactory(location, 
			new InformationRegistry.InformationSourceFactory() {
				@Override
				public InformationSource createInformationSource(String location) {
					return CommentItemMethods.newCommentItem(moduleClass, location, attrs);
				}
			});
	}
	
	public static void addPhotoSource(final String location, 
			final Map<String,Object> attrs, final Object moduleClass) { 
		InformationRegistry.addSourceFactory(location, 
			new InformationRegistry.InformationSourceFactory() {
				@Override
				public InformationSource createInformationSource(String location) {
					return CommentItemMethods.newPhotoItem(moduleClass, location, attrs);
				}
			});
	}
	
	public static void addInformationSourceAction(String location, int action) { 
		ReaderHelper.addInformationSourceAction(location, action);
	}
	
	public static void addPhotoSourceAction(String location, int action) { 
		ReaderHelper.addInformationSourceAction(location, action);
	}
	
}
