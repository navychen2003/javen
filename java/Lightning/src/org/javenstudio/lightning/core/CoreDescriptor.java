package org.javenstudio.lightning.core;

import java.io.File;
import java.util.Properties;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.Constants;

public class CoreDescriptor {
	
	private final CoreContainer mCores;
	private Properties mProperties;
	private String mName;
	private String mInstanceDir;
	private String mDataDir;
	private String mConfigName;
	private String mPropertiesName;
	
	public CoreDescriptor(CoreContainer cores, String name, 
			String instanceDir) throws ErrorException {
		mCores = cores;
		mName = name;
    
		if (name == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Core needs a name");
		}
    
		if (instanceDir == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Missing required \'instanceDir\'");
		}
		
		instanceDir = ContextLoader.normalizeDir(instanceDir);
		mInstanceDir = instanceDir;
		mConfigName = getDefaultConfigName();
	}

	public CoreDescriptor(CoreDescriptor descr) {
		mInstanceDir = descr.mInstanceDir;
		mConfigName = descr.mConfigName;
		mName = descr.mName;
		mDataDir = descr.mDataDir;
		mCores = descr.mCores;
	}

	protected Properties initImplicitProperties() {
		Properties props = new Properties(mCores.getProperties());
		props.setProperty("falcon.core.name", mName);
		props.setProperty("falcon.core.instanceDir", mInstanceDir);
		props.setProperty("falcon.core.dataDir", getDataDir());
		props.setProperty("falcon.core.configName", mConfigName);
		return props;
	}

	public boolean isDefault() { 
		return mName.equals(mCores.getDefaultCoreName());
	}
	
	/** @return the default config name. */
	public String getDefaultConfigName() {
		return Constants.CONFIG_XML_FILENAME;
	}

	/**@return the default data directory. */
	public String getDefaultDataDir() {
		return "data" + File.separator;
	}

	public String getPropertiesName() {
		return mPropertiesName;
	}

	public void setPropertiesName(String propertiesName) {
		mPropertiesName = propertiesName;
	}

	public String getDataDir() {
		String dataDir = mDataDir;
		if (dataDir == null) dataDir = getDefaultDataDir();
		if (new File(dataDir).isAbsolute()) 
			return dataDir;
		
		if (new File(mInstanceDir).isAbsolute()) {
			return ContextLoader.normalizeDir(
					ContextLoader.normalizeDir(mInstanceDir) + dataDir);
		} else  {
			return ContextLoader.normalizeDir(mCores.getHomeDir() +
					ContextLoader.normalizeDir(mInstanceDir) + dataDir);
		}
	}

	public void setDataDir(String s) {
		mDataDir = s;
		// normalize zero length to null.
		if (mDataDir != null && mDataDir.length() == 0) 
			mDataDir = null;
	}
  
	public boolean usingDefaultDataDir() {
		return mDataDir == null;
	}

	/** @return the core instance directory. */
	public String getInstanceDir() {
		return mInstanceDir;
	}

	/** Sets the core configuration resource name. */
	public void setConfigName(String name) {
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("name can not be null or empty");
		
		mConfigName = name;
	}

	/** @return the core configuration resource name. */
	public String getConfigName() {
		return mConfigName;
	}

	/**@return the initial core name */
	public String getName() {
		return mName;
	}

	public void setName(String name) { 
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("name can not be null or empty");
		
		mName = name;
	}
  
	public CoreContainer getContainer() {
		return mCores;
	}

	public Properties getProperties() {
		return mProperties;
	}

	/**
	 * Set this core's properties. Please note that some implicit values will be added to the
	 * Properties instance passed into this method. This means that the Properties instance
	 * set to this method will have different (less) key/value pairs than the Properties
	 * instance returned by #getCoreProperties method.
	 */
	public void setProperties(Properties coreProperties) {
		if (mProperties == null) {
			Properties p = initImplicitProperties();
			mProperties = new Properties(p);
			if(coreProperties != null)
				mProperties.putAll(coreProperties);
		}
	}
	
}
