package org.javenstudio.common.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.util.StringUtils;

public class Strings {
	private static final Logger LOG = Logger.getLogger(Strings.class);

	private static class StringMap { 
		private final Map<String,String> mItems;
		private final String mName;
		
		public StringMap(String name) { 
			mName = name;
			mItems = new HashMap<String,String>();
		}
		
		@SuppressWarnings("unused")
		public String getName() { return mName; }
		public String get(String name) { return mItems.get(name); }
		public void put(String name, String value) { mItems.put(name, value); }
	}
	
	public static interface StringLoader { 
		public String[] getResourceNames();
		public Map<String,String> loadStrings(String name);
		public void reload();
	}
	
	public static abstract class AbstractLoader implements StringLoader {
		private final String mResourceDir;
		private Map<String,Map<String,String>> mResources = null;
		
		public AbstractLoader(String path) { 
			if (path == null) throw new NullPointerException();
			mResourceDir = path;
		}
		
		public final String getResourceDir() { return mResourceDir; }
		
		@Override
		public boolean equals(Object o) { 
			if (o == this) return true;
			if (o == null || o.getClass() != this.getClass())
				return false;
			
			AbstractLoader other = (AbstractLoader)o;
			return this.getResourceDir().equals(other.getResourceDir());
		}
		
		@Override
		public void reload() { 
			loadResources(true);
		}
		
		@Override
		public String[] getResourceNames() {
			Map<String,Map<String,String>> items = loadResources(false);
			if (items != null)
				return items.keySet().toArray(new String[items.size()]);
			return null;
		}

		@Override
		public Map<String, String> loadStrings(String name) {
			Map<String,Map<String,String>> items = loadResources(false);
			if (items != null)
				return items.get(name);
			return null;
		}
		
		protected synchronized Map<String,Map<String,String>> 
				loadResources(boolean reload) {
			if (!reload && mResources != null) 
				return mResources;
			
			File dir = new File(mResourceDir);
			if (!dir.exists() || !dir.isDirectory())
				return null;
			
			File[] files = dir.listFiles();
			Map<String,Map<String,String>> items = 
					new HashMap<String,Map<String,String>>();
			
			for (int i=0; files != null && i < files.length; i++) { 
				File file = files[i];
				if (file == null || !file.exists() || !file.isFile())
					continue;
				
				String name = file.getName();
				Map<String,String> strings = loadResource(file);
				
				int pos = name.indexOf('.');
				if (pos >= 0)
					name = name.substring(0, pos);
				
				if (name != null && strings != null)
					items.put(name, strings);
			}
			
			mResources = items;
			return items;
		}
		
		protected abstract Map<String,String> loadResource(File file);
		
		@Override
		public String toString() { 
			return getClass().getSimpleName() + "{dir=" + mResourceDir + "}";
		}
	}
	
	public static final String NAMES_NAME = "names";
	
	private String mDefaultLang = null;
	
	private final Map<String, String> mLangs = 
			new HashMap<String, String>();
	
	private final List<StringLoader> mLoaders = 
			new ArrayList<StringLoader>();
	
	private final Map<String, StringMap> mStrings = 
			new HashMap<String, StringMap>();
	
	private final Object mLock = new Object();
	private boolean mDirty = true;
	
	private Strings() {}
	
	private static final Strings sInstance = new Strings();
	public static Strings getInstance() { return sInstance; }
	
	public static void addJsonDir(String path) { 
		if (path == null) return;
		Strings.getInstance().addLoader(new JSONStrings(path));
	}
	
	public static String get(String name) { 
		return get((String)null, name);
	}
	
	public static String get(String lang, String name) { 
		return getInstance().getString(lang, name);
	}
	
	public static String format(String format, Object... values) { 
		String value = get(format);
		if (value == null) value = format;
		
		if (value != null && values != null) 
			return String.format(value, values);
		
		return value;
	}
	
	public void addLoader(StringLoader loader) { 
		if (loader == null) return;
		
		synchronized (mLock) { 
			for (StringLoader sl : mLoaders) { 
				if (sl == loader || sl.equals(loader))
					return;
			}
			
			mLoaders.add(loader);
			mDirty = true;
			
			if (LOG.isDebugEnabled())
				LOG.debug("addLoader: " + loader);
		}
	}
	
	public String getDefaultLanguage() { 
		synchronized (mLock) { 
			initStrings();
			
			return mDefaultLang;
		}
	}
	
	public void setDefaultLanguage(String lang) { 
		if (lang == null || lang.length() == 0) 
			return;
		
		lang = getLanguage(lang);
		
		synchronized (mLock) { 
			initStrings();
			
			if (mStrings.containsKey(lang)) {
				mDefaultLang = lang;
				
				if (LOG.isDebugEnabled())
					LOG.debug("setDefaultLanguage: lang=" + lang);
			}
		}
	}
	
	public String getLanguage(String lang) { 
		final String origlang = lang;
		lang = StringUtils.trim(lang);
		
		if (lang == null || lang.length() == 0)
			return getDefaultLanguage();
		
		synchronized (mLock) { 
			initStrings();
			
			if (mStrings.containsKey(lang))
				return lang;
			
			lang = lang.toLowerCase();
			
			if (mStrings.containsKey(lang))
				return lang;
			
			String[] langSubs = splitLang(origlang);
			if (langSubs != null) { 
				for (String sub : langSubs) {
					if (sub == null || sub.length() == 0)
						continue;
					
					if (mStrings.containsKey(sub))
						return sub;
					
					sub = sub.toLowerCase();
					
					if (mStrings.containsKey(sub))
						return sub;
				}
			}
		}
		
		return getDefaultLanguage();
	}
	
	private static String[] splitLang(String lang) { 
		if (lang == null || lang.length() == 0)
			return null;
		
		ArrayList<String> list = new ArrayList<String>();
		StringBuilder sbuf = new StringBuilder();
		
		for (int i=0; i < lang.length(); i++) { 
			char chr = lang.charAt(i);
			if ((chr >= 'a' && chr <= 'z') || 
				(chr >= 'A' && chr <= 'Z') || 
				(chr >= '0' && chr <= '9')) { 
				sbuf.append(chr);
				continue;
			} else { 
				String str = sbuf.toString();
				sbuf.setLength(0);
				if (str != null && str.length() > 0)
					list.add(str);
			}
		}
		
		return list.toArray(new String[list.size()]);
	}
	
	public String[] getLanguages() { 
		synchronized (mLock) { 
			initStrings();
			return mLangs.keySet().toArray(new String[mLangs.size()]);
		}
	}
	
	public String getResourceName(String name) { 
		if (name == null) return null;
		
		synchronized (mLock) { 
			initStrings();
			String value = mLangs.get(name);
			return value != null ? value : name;
		}
	}
	
	public boolean hasResourceName(String name) { 
		if (name == null) return false;
		
		synchronized (mLock) { 
			initStrings();
			return mStrings.containsKey(name);
		}
	}
	
	public String getString(String lang, String name) { 
		if (name == null) return name;
		
		synchronized (mLock) { 
			initStrings();
			if (lang == null || lang.length() == 0)
				lang = mDefaultLang;
			
			StringMap map = mStrings.get(lang);
			String value = map != null ? map.get(name) : name;
			if (value != null) return value;
		}
		
		return name;
	}
	
	private void initStrings() { 
		synchronized (mLock) { 
			if (!mDirty) return;
			mDirty = false;
			
			if (LOG.isDebugEnabled())
				LOG.debug("initStrings");
			
			mStrings.clear();
			mLangs.clear();
			
			for (StringLoader loader : mLoaders) { 
				if (loader == null) continue;
				
				loader.reload();
				String[] names = loader.getResourceNames();
				
				for (int i=0; names != null && i < names.length; i++) {
					String name = names[i];
					Map<String, String> strings = loader.loadStrings(name);
					
					if (name == null || strings == null)
						continue;
					
					if (name.equals(NAMES_NAME))
						addLanguages(strings);
					else
						addStrings(name, strings);
				}
			}
			
			String lang = mDefaultLang;
			
			if (lang == null || lang.length() == 0)
				lang = System.getProperty("user.language");
			
			if (lang == null || lang.length() == 0)
				lang = "en";
			
			setDefaultLanguage(lang);
		}
	}
	
	private void addLanguages(Map<String, String> strings) { 
		if (strings == null)
			return;
		
		synchronized (mLock) { 
			for (Map.Entry<String, String> entry : strings.entrySet()) { 
				String key = entry.getKey();
				String value = entry.getValue();
				
				if (key != null && value != null) 
					mLangs.put(key, value);
			}
		}
	}
	
	private void addStrings(String name, Map<String, String> strings) { 
		if (name == null || strings == null)
			return;
		
		synchronized (mLock) { 
			StringMap map = mStrings.get(name);
			if (map == null) { 
				map = new StringMap(name);
				mStrings.put(name, map);
			}
			
			for (Map.Entry<String, String> entry : strings.entrySet()) { 
				String key = entry.getKey();
				String value = entry.getValue();
				
				if (key != null && value != null) {
					String old = map.get(key);
					if (old != null && LOG.isDebugEnabled()) {
						LOG.debug("addStrings: override \"" + key + "\"=\"" + old + "\" with \"" 
								+ value + "\", name=" + name);
					}
					
					map.put(key, value);
				}
			}
		}
	}
	
}
