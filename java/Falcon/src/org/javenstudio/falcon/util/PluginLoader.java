package org.javenstudio.falcon.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

/**
 * An abstract super class that manages standard lightning-style plugin configuration.
 * 
 */
public abstract class PluginLoader<T> {
	public static Logger LOG = Logger.getLogger(PluginLoader.class);
  
	private final Class<T> mPluginClassType;
	private final String mType;
	private final boolean mPreRegister;
	private final boolean mRequireName;
	
	public PluginLoader(String type, Class<T> pluginClassType) {
		this(type, pluginClassType, false, true);
	}
	
	/**
	 * @param type is the 'type' name included in error messages.
	 * @param preRegister if true, this will first register all Plugins, 
	 * then it will initialize them.
	 */
	public PluginLoader(String type, Class<T> pluginClassType, 
			boolean preRegister, boolean requireName) {
		mType = type;
		mPluginClassType = pluginClassType;
		mPreRegister = preRegister;
		mRequireName = requireName;
	}

	/**
	 * Where to look for classes
	 */
	//protected String[] getDefaultPackages() {
	//	return new String[]{};
	//}
  
	/**
	 * Create a plugin from an XML configuration.  Plugins are defined using:
	 * <pre class="prettyprint">
	 * {@code
	 * <plugin name="name1" class="lightning.ClassName">
	 *      ...
	 * </plugin>}
	 * </pre>
	 * 
	 * @param name - The registered name.  In the above example: "name1"
	 * @param className - class name for requested plugin.  In the above example: "lightning.ClassName"
	 * @param node - the XML node defining this plugin
	 */
	protected T create(ContextLoader loader, String name, String className, 
			ContextNode node) throws ErrorException {
		try {
			return loader.newInstance(className, mPluginClassType);
		} catch (ClassNotFoundException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
  
	/**
	 * Register a plugin with a given name.
	 * @return The plugin previously registered to this name, or null
	 */
	protected abstract T register(String name, T plugin) throws ErrorException;

	/**
	 * Initialize the plugin.  
	 * 
	 * @param plugin - the plugin to initialize
	 * @param node - the XML node defining this plugin
	 */
	protected abstract void init(T plugin, ContextNode node) throws ErrorException;

	/**
	 * Initializes and registers each plugin in the list.
	 * Given a NodeList from XML in the form:
	 * <pre class="prettyprint">
	 * {@code
	 * <plugins>
	 *    <plugin name="name1" class="lightning.ClassName" >
	 *      ...
	 *    </plugin>
	 *    <plugin name="name2" class="lightning.ClassName" >
	 *      ...
	 *    </plugin>
	 * </plugins>}
	 * </pre>
	 * 
	 * This will initialize and register each plugin from the list.  A class will 
	 * be generated for each class name and registered to the given name.
	 * 
	 * If 'preRegister' is true, each plugin will be registered *before* it is initialized
	 * This may be useful for implementations that need to inspect other registered 
	 * plugins at startup.
	 * 
	 * One (and only one) plugin may declare itself to be the 'default' plugin using:
	 * <pre class="prettyprint">
	 * {@code
	 *    <plugin name="name2" class="lightning.ClassName" default="true">}
	 * </pre>
	 * If a default element is defined, it will be returned from this function.
	 * 
	 */
	public T load(ContextLoader loader, Iterator<ContextNode> nodes) throws ErrorException {
		List<PluginInitInfo> infos = new ArrayList<PluginInitInfo>();
		T defaultPlugin = null;
    
		if (nodes != null ) {
			while (nodes.hasNext()) {
				ContextNode node = nodes.next();
  
				String name = null;
				try {
					name              = node.getAttribute("name", mRequireName ? mType : null);
					String className  = node.getAttribute("class", mType);
					String defaultStr = node.getAttribute("default", null);
            
					T plugin = create(loader, name, className, node );
					if (LOG.isDebugEnabled()) {
						LOG.debug("created " + ((name != null) ? name : "") + ": " 
								+ plugin.getClass().getName());
					}
					
					// Either initialize now or wait till everything has been registered
					if (mPreRegister) 
						infos.add(new PluginInitInfo(plugin, node));
					else 
						init(plugin, node);
          
					T old = register(name, plugin);
					if (old != null && !(name == null && !mRequireName)) {
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"Multiple " + mType + " registered to the same name: " + name + 
								" ignoring: " + old);
					}
          
					if (defaultStr != null && Boolean.parseBoolean(defaultStr)) {
						if (defaultPlugin != null) {
							throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
									"Multiple default " + mType + " plugins: " + defaultPlugin + 
									" AND " + name);
						}
						defaultPlugin = plugin;
					}
				} catch (Exception ex) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
							"Plugin init failure for " + mType + (null != name ? (" \"" + name + "\"") : "") +
							": " + ex.getMessage(), ex);
				}
			}
		}
      
		// If everything needs to be registered *first*, this will initialize later
		for (PluginInitInfo pinfo : infos) {
			try {
				init(pinfo.mPlugin, pinfo.mNode);
			} catch (Exception ex) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Plugin Initializing failure for " + mType, ex);
			}
		}
		
		return defaultPlugin;
	}
  
	/**
	 * Initializes and registers a single plugin.
	 * 
	 * Given a NodeList from XML in the form:
	 * <pre class="prettyprint">
	 * {@code
	 * <plugin name="name1" class="lightning.ClassName" > ... </plugin>}
	 * </pre>
	 * 
	 * This will initialize and register a single plugin. A class will be
	 * generated for the plugin and registered to the given name.
	 * 
	 * If 'preRegister' is true, the plugin will be registered *before* it is
	 * initialized This may be useful for implementations that need to inspect
	 * other registered plugins at startup.
	 * 
	 * The created class for the plugin will be returned from this function.
	 * 
	 */
	public T loadSingle(ContextLoader loader, ContextNode node) throws ErrorException {
		List<PluginInitInfo> info = new ArrayList<PluginInitInfo>();
		T plugin = null;

		try {
			String name = node.getAttribute("name", mRequireName ? mType : null);
			String className = node.getAttribute("class", mType);
			
			plugin = create(loader, name, className, node);
			if (LOG.isDebugEnabled())
				LOG.debug("created " + name + ": " + plugin.getClass().getName());

			// Either initialize now or wait till everything has been registered
			if (mPreRegister) 
				info.add(new PluginInitInfo(plugin, node));
			else 
				init(plugin, node);

			T old = register(name, plugin);
			if (old != null && !(name == null && !mRequireName)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
						"Multiple " + mType + " registered to the same name: " + name + " ignoring: " + old);
			}

		} catch (Exception ex) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Plugin init failure for " + mType, ex);
		}

		// If everything needs to be registered *first*, this will initialize later
		for (PluginInitInfo pinfo : info) {
			try {
				init(pinfo.mPlugin, pinfo.mNode);
			} catch (Exception ex) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Plugin init failure for " + mType, ex);
			}
		}
		
		return plugin;
	}
  
	/**
	 * Internal class to hold onto initialization info so that it can be initialized 
	 * after it is registered.
	 */
	private class PluginInitInfo {
		public final T mPlugin;
		public final ContextNode mNode;
    
		public PluginInitInfo(T plugin, ContextNode node) {
			mPlugin = plugin;
			mNode = node;
		}
	}
	
}
