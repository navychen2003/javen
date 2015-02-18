package org.javenstudio.falcon.util;

import java.util.ArrayList;
import java.util.List;

/**
 * An Object which represents a Plugin of any type 
 *
 */
public abstract class PluginInfo {
	
	public abstract String getName();
	public abstract String getClassName();
	public abstract String getType();
	
	public abstract NamedList<?> getInitArgs();
	public abstract List<PluginInfo> getChildren();
	
	public abstract String getAttribute(String name);
	
	public abstract boolean isEnabled();
	public abstract boolean isDefault();

	public PluginInfo getChild(String type){
		List<PluginInfo> lst = getChildren(type);
		return lst.isEmpty() ? null : lst.get(0);
	}

	/**
	 * Filter children by type
	 * @param type The type name. must not be null
	 * @return The mathcing children
	 */
	public List<PluginInfo> getChildren(String type) {
		if (getChildren().isEmpty()) 
			return getChildren();
		
		List<PluginInfo> result = new ArrayList<PluginInfo>();
		for (PluginInfo child : getChildren()) { 
			if (type.equals(child.getType())) 
				result.add(child);
		}
		
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		if (getType() != null) sb.append("type = " + getType() + ",");
		if (getName() != null) sb.append("name = " + getName() + ",");
		if (getClassName() != null) sb.append("class = " + getClassName() + ",");
		if (getInitArgs() != null && getInitArgs().size() > 0) 
			sb.append("args = " + getInitArgs());
		sb.append("}");
		return sb.toString();
	}
	
}
