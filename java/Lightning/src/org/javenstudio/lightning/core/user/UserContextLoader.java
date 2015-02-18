package org.javenstudio.lightning.core.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.common.util.Logger;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.context.DOMContextLoader;
import org.javenstudio.panda.util.ResourceLoader;

public class UserContextLoader extends DOMContextLoader 
		implements ResourceLoader {
	private static final Logger LOG = Logger.getLogger(UserContextLoader.class);

	// Using this pattern, legacy analysis components from previous versions 
	// are identified and delegated to SPI loader:
	private static final Pattern sLegacyAnalysisPattern = 
			Pattern.compile("((\\Q" + Constants.PROJECT_BASE + ".analysis.\\E)|(\\Q" 
					+ Constants.PROJECT + ".\\E))([\\p{L}_$][\\p{L}\\p{N}_$]+?)" 
					+ "(TokenFilter|Filter|Tokenizer|CharFilter)Factory");

	
	public UserContextLoader(String instanceDir) { 
		this(instanceDir, null, null);
	}
	
	public UserContextLoader(String instanceDir, ClassLoader parent, 
			Properties properties) { 
		super(instanceDir, parent, properties);
	}
	
	public String getProjectName() { return Constants.PROJECT; }
	public String getProjectBase() { return Constants.PROJECT_BASE; }
	public String[] getProjectPackages() { return Constants.PROJECT_PACKAGES; }
	public String getConfigRootName() { return Constants.PROJECT; }
	
	@Override
	public InputStream openResource(String resource) throws IOException { 
		return openResourceAsStream(resource);
	}
	
	@Override
	protected String normalizeClassName(String cname, String projectName) { 
		if (cname.startsWith(Constants.PROJECT + ".")) 
    		return cname.substring(Constants.PROJECT.length() + 1);
    	
		return super.normalizeClassName(cname, projectName);
	}
	
	@Override
	protected <T> Class<? extends T> lookupClassInternal(String cname, Class<T> expectedType) 
			throws ClassNotFoundException { 
	    // first try legacy analysis patterns, now replaced by Analysis package:
	    final Matcher m = sLegacyAnalysisPattern.matcher(cname);
	    if (m.matches()) {
	    	final String name = m.group(4);
	    	if (LOG.isDebugEnabled())
	    		LOG.debug("Trying to load class from analysis SPI using name=" + name);
	    	
	    	try {
	    		//if (CharFilterFactory.class.isAssignableFrom(expectedType)) {
	    		//	return CharFilterFactory.lookupClass(name).asSubclass(expectedType);
	    		//} else if (TokenizerFactory.class.isAssignableFrom(expectedType)) {
	    		//	return TokenizerFactory.lookupClass(name).asSubclass(expectedType);
	    		//} else if (TokenFilterFactory.class.isAssignableFrom(expectedType)) {
	    		//	return TokenFilterFactory.lookupClass(name).asSubclass(expectedType);
	    		//} else {
	    			if (LOG.isWarnEnabled()) {
	    				LOG.warn("'" + cname + "' looks like an analysis factory, " 
	    						+ "but caller requested different class type: " + expectedType.getName());
	    			}
	    		//}
	    	} catch (IllegalArgumentException ex) { 
	    		// ok, we fall back to legacy loading
	    	}
	    }
		
		return null;
	}
	
}
