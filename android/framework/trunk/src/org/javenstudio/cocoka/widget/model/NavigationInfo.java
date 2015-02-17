package org.javenstudio.cocoka.widget.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NavigationInfo {

	public static final String ATTR_NAME = "name";
	public static final String ATTR_TITLE = "title";
	public static final String ATTR_SUBTITLE = "subTitle";
	public static final String ATTR_DROPDOWNTITLE = "dropdownTitle";
	
	private final String mName;
	private final Map<String, Object> mAttrs;
	
	public NavigationInfo(String name) { 
		this(name, new HashMap<String, Object>());
	}
	
	public NavigationInfo(String name, Map<String, Object> attrs) { 
		mName = name;
		mAttrs = attrs;
		
		if (mName == null) 
			throw new NullPointerException("name is null");
		
		if (mAttrs == null) 
			throw new NullPointerException("attrs is null");
		
		//if (!mAttrs.containsKey(ATTR_NAME))
		//	setName(name);
		
		if (!mAttrs.containsKey(ATTR_TITLE))
			setTitle(name);
	}
	
	public final String getName() { return mName; }
	
	//public String getName() { 
	//	return (String)getAttribute(ATTR_NAME); 
	//}
	
	//public void setName(String name) { 
	//	setAttribute(ATTR_NAME, name); 
	//}
	
	public String getTitle() { 
		return (String)getAttribute(ATTR_TITLE); 
	}
	
	public void setTitle(String title) { 
		setAttribute(ATTR_TITLE, title); 
	}
	
	public String getSubTitle() { 
		return (String)getAttribute(ATTR_SUBTITLE); 
	}
	
	public void setSubTitle(String title) { 
		setAttribute(ATTR_SUBTITLE, title); 
	}
	
	public String getDropdownTitle() { 
		return (String)getAttribute(ATTR_DROPDOWNTITLE); 
	}
	
	public void setDropdownTitle(String title) { 
		setAttribute(ATTR_DROPDOWNTITLE, title); 
	}
	
	public boolean hasAttribute(String key) { 
		return mAttrs.containsKey(key);
	}
	
	public Object getAttribute(String key) { 
		return mAttrs.get(key);
	}
	
	public void setAttribute(String key, Object value) { 
		mAttrs.put(key, value);
	}
	
	public Object removeAttribute(String key) { 
		return mAttrs.remove(key);
	}
	
	public Collection<String> getAttributeNames() { 
		return mAttrs.keySet();
	}
	
	@Override
	public boolean equals(Object o) { 
		if (o == this) return true;
		if (o == null || !(o instanceof NavigationInfo)) 
			return false;
		
		NavigationInfo other = (NavigationInfo)o;
		return other.mName.equals(this.mName);
				//&& other.mAttrs.equals(this.mAttrs);
	}
	
	@Override
	public String toString() { 
		StringBuilder sbuf = new StringBuilder();
		sbuf.append(getClass().getSimpleName());
		sbuf.append("{name=").append(mName);
		sbuf.append(",attrs={");
		if (mAttrs != null) { 
			int count = 0;
			for (String name : getAttributeNames()) { 
				Object value = getAttribute(name);
				if ((count++) > 0) sbuf.append(",");
				sbuf.append(name);
				sbuf.append("=");
				sbuf.append(value);
			}
		}
		sbuf.append("}}");
		return sbuf.toString();
	}
	
}
