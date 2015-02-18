package org.javenstudio.lightning.context;

import java.io.InputStream;
import java.util.Properties;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextResource;

public class DOMContextLoader extends ContextLoader {

	public DOMContextLoader(String instanceDir) { 
		this(instanceDir, null, null);
	}
	
	public DOMContextLoader(String instanceDir, 
			ClassLoader parent, Properties properties) {
		super(instanceDir, parent, properties);
	}

	@Override
	public ContextResource openResource(String name, InputStream is,
			String prefix, boolean subProps) throws ErrorException {
		return new DOMConfig(this, name, is, prefix, subProps);
	}
	
}
