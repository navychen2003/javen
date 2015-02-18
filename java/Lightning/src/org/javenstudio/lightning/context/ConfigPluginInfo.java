package org.javenstudio.lightning.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.PluginInfo;

public class ConfigPluginInfo extends PluginInfo {

	@SuppressWarnings("rawtypes")
	public static final PluginInfo EMPTY_INFO = new ConfigPluginInfo("", 
			Collections.<String,String>emptyMap(), 
			new NamedList(), Collections.<PluginInfo>emptyList());

	private static final Set<String> NL_TAGS = new HashSet<String>(
			Arrays.asList("lst", "arr", "bool", "str", "int", "long", "float", "double"));
	
	private final String mName, mClassName, mType;
	private final NamedList<?> mInitArgs;
	private final Map<String, String> mAttributes;
	private final List<PluginInfo> mChildren;

	public ConfigPluginInfo(String type, Map<String,String> attrs, 
			NamedList<?> initArgs, List<PluginInfo> children) {
		mType = type;
		mName = attrs.get("name");
		mClassName = attrs.get("class");
		mInitArgs = initArgs;
		mAttributes = Collections.unmodifiableMap(attrs);
		mChildren = (children == null) ? Collections.<PluginInfo>emptyList() : 
			Collections.unmodifiableList(children);
	}

	public ConfigPluginInfo(ContextNode node, String error, 
			boolean requireName, boolean requireClass) throws ErrorException {
		mType = node.getNodeName();
		mName = node.getAttribute("name", requireName ? error : null);
		mClassName = node.getAttribute("class", requireClass ? error : null);
		mInitArgs = node.getChildNodesAsNamedList();
		mAttributes = Collections.unmodifiableMap(node.getAttributes());
		mChildren = loadSubPlugins(node);
	}

	public final String getName() { return mName; }
	public final String getClassName() { return mClassName; }
	public final String getType() { return mType; }
	
	public final NamedList<?> getInitArgs() { return mInitArgs; }
	public final List<PluginInfo> getChildren() { return mChildren; }
	
	@Override
	public final String getAttribute(String name) { 
		return mAttributes.get(name);
	}
	
	private List<PluginInfo> loadSubPlugins(ContextNode node) throws ErrorException {
		List<PluginInfo> children = new ArrayList<PluginInfo>();
		
		//if there is another sub tag with a non namedlist tag that has to be another plugin
		Iterator<ContextNode> nodes = node.getChildNodes();
		while (nodes.hasNext()) {
			ConfigNode nd = (ConfigNode)nodes.next();
			if (!nd.isElementNode()) 
				continue;
			if (NL_TAGS.contains(nd.getNodeName())) 
				continue;
			
			PluginInfo pluginInfo = new ConfigPluginInfo(nd, null, false, false);
			if (pluginInfo.isEnabled()) 
				children.add(pluginInfo);
		}
		
		return children.isEmpty() ? Collections.<PluginInfo>emptyList() : 
			Collections.unmodifiableList(children);
	}

	@Override
	public boolean isEnabled() {
		String enable = mAttributes.get("enable");
		return enable == null || Boolean.parseBoolean(enable); 
	}

	@Override
	public boolean isDefault() {
		return Boolean.parseBoolean(mAttributes.get("default"));
	}

}
