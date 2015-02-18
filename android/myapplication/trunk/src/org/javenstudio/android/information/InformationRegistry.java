package org.javenstudio.android.information;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.android.SourceHelper;
import org.javenstudio.cocoka.widget.model.NavigationGroup;
import org.javenstudio.cocoka.widget.model.NavigationItem;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderGroup;
import org.javenstudio.provider.publish.information.InformationProvider;

import android.content.Context;

public class InformationRegistry {

	public static interface InformationSourceFactory { 
		public InformationSource createInformationSource(String location);
	}
	
	public static class InformationSourceConf { 
		private final Class<? extends InformationSource> mItemClass;
		private final InformationSourceFactory mFactory; 
		
		public InformationSourceConf(Class<? extends InformationSource> itemClass, 
				InformationSourceFactory factory) { 
			mItemClass = itemClass; 
			mFactory = factory;
		}
		
		public final Class<? extends InformationSource> getSourceClass() { 
			return mItemClass; 
		}
		
		public final InformationSourceFactory getSourceFactory() { 
			return mFactory;
		}
	}
	
	private final static Map<String, InformationSourceConf> mInformationClassMap = 
			new HashMap<String, InformationSourceConf>(); 
	
	public static void addSourceClass(String location, Class<? extends InformationSource> clazz) { 
		if (clazz == null || location == null || location.length() == 0) 
			return; 
		
		InformationSourceConf conf = new InformationSourceConf(clazz, null);
		addSourceClass(location, conf);
	}
	
	public static void addSourceFactory(String location, InformationSourceFactory factory) { 
		if (factory == null || location == null || location.length() == 0) 
			return; 
		
		InformationSourceConf conf = new InformationSourceConf(null, factory);
		addSourceClass(location, conf);
	}
	
	private static void addSourceClass(String location, InformationSourceConf conf) { 
		if (conf == null || location == null || location.length() == 0) 
			return; 
		
		synchronized (mInformationClassMap) { 
			mInformationClassMap.put(location, conf); 
			
			int pos = location.indexOf('?'); 
			if (pos > 0) { 
				String loc = location.substring(0, pos); 
				if (!mInformationClassMap.containsKey(loc))
					mInformationClassMap.put(loc, conf); 
			}
			
			pos = location.lastIndexOf('/'); 
			if (pos > 0) { 
				String loc = location.substring(0, pos+1); 
				if (!mInformationClassMap.containsKey(loc))
					mInformationClassMap.put(loc, conf); 
			}
			
			pos = location.indexOf("://");
			if (pos > 0) { 
				int pos2 = location.indexOf('/', pos+3);
				if (pos2 > 0) { 
					int pos3 = location.indexOf('/', pos2+1); 
					if (pos3 > 0) { 
						String loc = location.substring(0, pos3+1);
						if (!mInformationClassMap.containsKey(loc))
							mInformationClassMap.put(loc, conf); 
					}
				}
			}
			
			String host = getHostPort(location); 
			if (host != null && host.length() > 0) {
				if (!mInformationClassMap.containsKey(host))
					mInformationClassMap.put(host, conf); 
			}
		}
	}
	
	public static InformationSourceConf getSourceConf(String location) { 
		if (location == null || location.length() == 0) 
			return null; 
		
		synchronized (mInformationClassMap) { 
			InformationSourceConf conf = mInformationClassMap.get(location); 
			if (conf == null) {
				int pos = location.indexOf('?'); 
				if (pos > 0) { 
					String loc = location.substring(0, pos); 
					conf = mInformationClassMap.get(loc); 
				}
				
				if (conf == null) { 
					pos = location.lastIndexOf('/'); 
					if (pos > 0) { 
						String loc = location.substring(0, pos+1); 
						conf = mInformationClassMap.get(loc); 
					}
				
					if (conf == null) {
						pos = location.indexOf("://");
						if (pos > 0) { 
							int pos2 = location.indexOf('/', pos+3);
							if (pos2 > 0) { 
								int pos3 = location.indexOf('/', pos2+1); 
								if (pos3 > 0) { 
									String loc = location.substring(0, pos3+1);
									conf = mInformationClassMap.get(loc); 
								}
							}
						}
						
						if (conf == null) { 
							String host = getHostPort(location); 
							if (host != null && host.length() > 0) 
								conf = mInformationClassMap.get(host); 
						}
					}
				}
			}
			
			return conf; 
		}
	}
	
	private static String getHostPort(String location) { 
		if (location == null || location.length() == 0) 
			return null; 
		
		try { 
			URL url = new URL(location); 
			String host = url.getHost(); 
			int port = url.getPort(); 
			if (port > 0) 
				host = host + ":" + port; 
			
			return host; 
		} catch (Exception e) { 
			// ignore
		}
		
		return null; 
	}
	
	@SuppressWarnings({"unchecked"})
	private static InformationSource newInformationSource(
			final Class<? extends InformationSource> clazz, final String location) {
		if (clazz == null) return null; 
        try {
        	Constructor<InformationSource> ctor = (Constructor<InformationSource>)
        			clazz.getConstructor(new Class<?>[]{String.class}); 
        	return ctor.newInstance(location); 
        } catch (InvocationTargetException e) {
        	throw new RuntimeException(
        			clazz + " could not be instantiated", e);
        } catch (NoSuchMethodException e) { 
        	throw new RuntimeException(
        			clazz + " could not be instantiated", e);
        } catch (InstantiationException ex) {
            throw new RuntimeException(
            		clazz + " could not be instantiated", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(
            		clazz + " could not be instantiated", ex);
        }
	}
	
	public static InformationSource createSourceFor(String location) { 
		InformationSourceConf conf = getSourceConf(location); 
		if (conf != null) {
			InformationSourceFactory factory = conf.getSourceFactory();
			if (factory != null) 
				return factory.createInformationSource(location);
			
			Class<? extends InformationSource> clazz = conf.getSourceClass();
			if (clazz != null) 
				return newInformationSource(clazz, location); 
		}
		
		return null; 
	}
	
	public static boolean existSource(String location) { 
		return getSourceConf(location) != null; 
	}
	
	private final static List<NavigationItem> mNavigationItems = new ArrayList<NavigationItem>(); 
	private static NavigationItem mDefaultItem = null; 
	
	public static int getNavigationItemCount() { 
		synchronized (mNavigationItems) { 
			return mNavigationItems.size();
		}
	}
	
	public static void addNavigationItem(NavigationItem item) { 
		if (item == null) return; 
		
		synchronized (mNavigationItems) { 
			for (NavigationItem it : mNavigationItems) { 
				//if (it == item || it.getInfo().equals(item.getInfo())) { 
				//	throw new RuntimeException(item.getClass().getSimpleName() + ": " + 
				//			item.getInfo().getName() + " already registered"); 
				//}
				if (it == item) return;
			}
			
			mNavigationItems.add(item); 
		}
	}
	
	public static NavigationItem[] getNavigationItems() { 
		synchronized (mNavigationItems) { 
			return mNavigationItems.toArray(new NavigationItem[0]); 
		}
	}
	
	public static void setDefaultNavigationItem(NavigationItem item) { 
		if (item == null) return; 
		
		synchronized (mNavigationItems) { 
			mDefaultItem = item; 
		}
	}
	
	public static NavigationItem getDefaultNavigationItem() { 
		synchronized (mNavigationItems) { 
			return mDefaultItem; 
		}
	}
	
	public static Provider[] getProviders(Context groupContext, int groupIconRes) { 
		NavigationItem[] navItems = InformationRegistry.getNavigationItems();
		if (navItems != null) { 
			Map<String, List<NavigationGroup>> groupMap = new HashMap<String, List<NavigationGroup>>();
			List<String> groupNames = new ArrayList<String>();
			List<Provider> providers = new ArrayList<Provider>();
			
			for (int i=0; i < navItems.length; i++) {
				NavigationItem navItem = navItems[i];
				if (navItem != null && navItem instanceof NavigationGroup) { 
					NavigationGroup groupItem = (NavigationGroup)navItem;
					
					String groupName = (String)groupItem.getInfo().getAttribute(Information.ATTR_GROUPNAME);
					if (groupName == null || groupName.length() == 0)
						groupName = "#NavigaiontGroup-Item-" + i;
					
					List<NavigationGroup> list = groupMap.get(groupName);
					if (list == null) { 
						list = new ArrayList<NavigationGroup>();
						groupMap.put(groupName, list);
						groupNames.add(groupName);
					}
					
					list.add(groupItem);
				}
			}
			
			for (String groupName : groupNames) { 
				List<NavigationGroup> list = groupMap.get(groupName);
				if (list == null || list.size() <= 0) 
					continue;
				
				if (list.size() == 1) { 
					NavigationGroup groupItem = list.get(0);
					if (groupItem != null) { 
						InformationProvider p = new InformationProvider(groupItem);
								//Information.getContext(groupItem.getInfo()), groupItem);
						providers.add(p);
					}
					
				} else { 
					ProviderGroup pl = null;
					
					for (NavigationGroup groupItem : list) { 
						if (groupItem != null) { 
							//Context context = Information.getContext(groupItem.getInfo()); 
							if (pl == null) { 
								//Context gcontext = groupContext;
								int iconRes = SourceHelper.getSourceIconRes(groupName, groupIconRes);
								if (iconRes == 0) {
									iconRes = Information.getItemIconRes(groupItem.getInfo());
									//gcontext = context;
								}
								//pl = new ProviderGroup(gcontext, groupName, iconRes);
								pl = new ProviderGroup(groupName, iconRes);
							}
							
							//InformationProvider p = new InformationProvider(context, groupItem);
							InformationProvider p = new InformationProvider(groupItem);
							pl.addProvider(p);
						}
					}
					
					if (pl != null) 
						providers.add(pl);
				}
			}
			
			return providers.toArray(new Provider[providers.size()]);
		}
		
		return null;
	}
	
	private static final Map<String, InformationItem> sInformationItems = 
			new HashMap<String, InformationItem>();
	
	public static void clearInformationItems() {
		synchronized (sInformationItems) { 
			sInformationItems.clear();
		}
	}
	
	public static InformationItem getInformationItem(String location) { 
		if (location == null) return null;
		synchronized (sInformationItems) { 
			return sInformationItems.get(location);
		}
	}
	
	public static void addInformationItem(String location, InformationItem item) { 
		if (location == null || item == null) return;
		synchronized (sInformationItems) { 
			sInformationItems.put(location, item);
		}
	}
	
}
