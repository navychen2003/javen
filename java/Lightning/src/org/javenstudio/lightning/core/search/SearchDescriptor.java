package org.javenstudio.lightning.core.search;

import java.util.Properties;

import org.javenstudio.falcon.Constants;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.core.CoreContainer;
import org.javenstudio.lightning.core.CoreDescriptor;

public class SearchDescriptor extends CoreDescriptor {

	private String mSchemaName;
	
	public SearchDescriptor(CoreContainer cores, String name, 
			String instanceDir) throws ErrorException {
		super(cores, name, instanceDir);
		mSchemaName = getDefaultSchemaName();
	}
	
	public SearchDescriptor(CoreDescriptor descr) {
		super(descr);
		mSchemaName = ((SearchDescriptor)descr).mSchemaName;
	}
	
	/** @return the default schema name. */
	public String getDefaultSchemaName() {
		return Constants.SCHEMA_XML_FILENAME;
	}
	
	/**Sets the core schema resource name. */
	public void setSchemaName(String name) {
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("name can not be null or empty");
		
		mSchemaName = name;
	}

	/**@return the core schema resource name. */
	public String getSchemaName() {
		return mSchemaName;
	}
	
	@Override
	protected Properties initImplicitProperties() {
		Properties props = new Properties(getContainer().getProperties());
		props.setProperty("lightning.core.name", getName());
		props.setProperty("lightning.core.instanceDir", getInstanceDir());
		props.setProperty("lightning.core.dataDir", getDataDir());
		props.setProperty("lightning.core.configName", getConfigName());
		props.setProperty("lightning.core.schemaName", getSchemaName());
		return props;
	}
	
}
