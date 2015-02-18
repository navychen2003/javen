package org.javenstudio.falcon.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.Constants;
import org.javenstudio.falcon.ErrorException;

public abstract class ContextLoader {
	private static final Logger LOG = Logger.getLogger(ContextLoader.class);

	private static final Charset UTF_8 = Constants.UTF_8;
	
	private static final String PROJECT = Constants.PROJECT;
	private static final String BASE = Constants.PROJECT_BASE;
	private static final String[] PACKAGES = Constants.PROJECT_PACKAGES;
	
	private final Properties mProperties;
	private final String mInstanceDir;
	private URLClassLoader mClassLoader;
	
	public ContextLoader(String instanceDir) { 
		this(instanceDir, null, null);
	}
	
	public ContextLoader(String instanceDir, ClassLoader parent, 
			Properties properties) { 
		mInstanceDir = normalizeDir(instanceDir);
		mClassLoader = createClassLoader(null, parent);
		addToClassLoader("./lib/", null);
		mProperties = properties;
	}
	
	public final Properties getProperties() { return mProperties; }
	public final ClassLoader getClassLoader() { return mClassLoader; }
	public final String getInstanceDir() { return mInstanceDir; }
	public final String getConfigDir() { return mInstanceDir + "conf/"; }
	
	public String getProjectName() { return PROJECT; }
	public String getProjectBase() { return BASE; }
	public String[] getProjectPackages() { return PACKAGES; }
	
	public String getConfigRootName() { return PROJECT; }
	
	public static String locateHome(String projectName) { 
		String home = null;
	    // Try JNDI
	    try {
	    	Context c = new InitialContext();
	    	home = (String)c.lookup("java:comp/env/" + projectName + "/home");
	    	
	    	if (LOG.isInfoEnabled())
	    		LOG.info("Using JNDI " + projectName + ".home: " + home);
	    } catch (NoInitialContextException e) {
	    	if (LOG.isInfoEnabled())
	    		LOG.info("JNDI not configured for " + projectName + " (NoInitialContextEx)");
	    } catch (NamingException e) {
	    	if (LOG.isInfoEnabled())
	    		LOG.info("No /" + projectName + "/home in JNDI");
	    } catch (RuntimeException ex) {
	    	if (LOG.isWarnEnabled())
	    		LOG.warn("Odd RuntimeException while testing for JNDI: " + ex.getMessage());
	    } 
	    
	    // Now try system property
	    if (home == null) {
	    	String prop = projectName + ".home";
	    	home = System.getProperty(prop);
	    	if (home != null) {
	    		if (LOG.isInfoEnabled())
	    			LOG.info("using system property " + prop + ": " + home );
	    	}
	    }
	    
	    // if all else fails, try 
	    if (home == null) {
	    	home = projectName + '/';
	    	
	    	if (LOG.isInfoEnabled()) {
	    		LOG.info(projectName + " home defaulted to '" + home 
	    				+ "' (could not find system property or JNDI)");
	    	}
	    }
	    
	    return normalizeDir(home);
	}
	
	/** Ensures a directory name always ends with a '/'. */
	public static String normalizeDir(String path) {
		return (path != null && !(path.endsWith("/") || path.endsWith("\\")))? path + File.separator : path;
	}
	
	static URLClassLoader replaceClassLoader(final URLClassLoader oldLoader,
			final File base, final FileFilter filter) {
		if (null != base && base.canRead() && base.isDirectory()) {
			File[] files = base.listFiles(filter);
			if (null == files || 0 == files.length) 
				return oldLoader;

			URL[] oldElements = oldLoader.getURLs();
			URL[] elements = new URL[oldElements.length + files.length];
			System.arraycopy(oldElements, 0, elements, 0, oldElements.length);

			for (int j = 0; j < files.length; j++) {
				try {
					URL element = files[j].toURI().normalize().toURL();
					
					if (LOG.isInfoEnabled())
						LOG.info("Adding '" + element.toString() + "' to classloader");
					
					elements[oldElements.length + j] = element;
				} catch (MalformedURLException e) {
					if (LOG.isErrorEnabled())
						LOG.error("Can't add element to classloader: " + files[j], e);
				}
			}
			return URLClassLoader.newInstance(elements, oldLoader.getParent());
		}
		
		// are we still here?
		return oldLoader;
	}
	
	/**
	 * Convenience method for getting a new ClassLoader using all files found
	 * in the specified lib directory.
	 */
	public static URLClassLoader createClassLoader(final File libDir, ClassLoader parent) {
		if ( null == parent ) 
			parent = Thread.currentThread().getContextClassLoader();
		
		return replaceClassLoader(URLClassLoader.newInstance(new URL[0], parent),
				libDir, null);
	}
	
	private void addToClassLoader(final String baseDir, final FileFilter filter) {
	    File base = FileUtils.resolvePath(new File(getInstanceDir()), baseDir);
	    mClassLoader = replaceClassLoader(mClassLoader, base, filter);
	}
	
	@SuppressWarnings("unused")
	private void addToClassLoader(final String path) {
		final File file = FileUtils.resolvePath(new File(getInstanceDir()), path);
		if (file.canRead()) {
			mClassLoader = replaceClassLoader(mClassLoader, file.getParentFile(),
					new FileFilter() {
						public boolean accept(File pathname) {
							return pathname.equals(file);
						}
				});
		} else {
			if (LOG.isErrorEnabled())
				LOG.error("Can't find (or read) file to add to classloader: " + file);
		}
	}
	
	public ContextResource openResource(String name, InputStream is) 
			throws ErrorException { 
		return openResource(name, is, null, true);
	}
	
	public ContextResource openResource(String name, 
			InputStream is, String prefix) throws ErrorException { 
		return openResource(name, is, prefix, true);
	}
	
	public abstract ContextResource openResource(String name, 
			InputStream is, String prefix, boolean subProps) throws ErrorException;
	
	/** 
	 * Opens any resource by its name.
	 * By default, this will look in multiple locations to load the resource:
	 * $configDir/$resource (if resource is not absolute)
	 * $CWD/$resource
	 * otherwise, it will look for it in any jar accessible through the class loader.
	 * Override this method to customize loading resources.
	 * @return the stream for the named resource
	 */
	public InputStream openResourceAsStream(String resource) throws IOException { 
	    InputStream is = null;
	    
	    if (LOG.isDebugEnabled())
	    	LOG.debug("openResource: configDir=" + getConfigDir() + " resource=" + resource);
	    
	    try {
	    	File f0 = new File(resource);
	    	File f = f0;
	    	if (!f.isAbsolute()) {
	    		// try $CWD/$configDir/$resource
	    		f = new File(getConfigDir() + resource);
	    	}
	    	
	    	if (f.isFile() && f.canRead()) {
	    		return new FileInputStream(f);
	    		
	    	} else if (f != f0) { // no success with $CWD/$configDir/$resource
	    		if (f0.isFile() && f0.canRead())
	    			return new FileInputStream(f0);
	    	}
	    	
	    	// delegate to the class loader (looking into $INSTANCE_DIR/lib jars)
	    	is = mClassLoader.getResourceAsStream(resource);
	    	if (is == null)
	    		is = mClassLoader.getResourceAsStream(getConfigDir() + resource);
	    	
	    } catch (FileNotFoundException ex) { 
	    	if (LOG.isWarnEnabled())
	    		LOG.warn("Resource: " + resource + " not found: " + ex.toString(), ex);
	    	
	    	is = null;
	    	//throw ex;
	    } catch (Throwable e) {
	    	throw new IOException("Error opening " + resource, e);
	    }
	    
	    if (is == null) {
	    	throw new FileNotFoundException("Can't find resource '" + resource + "' in classpath or '" 
	    			+ getConfigDir() + "', cwd=" + System.getProperty("user.dir"));
	    }
	    
	    return is;
	}
	
	/**
	 * Accesses a resource by name and returns the (non comment) lines
	 * containing data.
	 *
	 * <p>
	 * A comment line is any line that starts with the character "#"
	 * </p>
	 *
	 * @return a list of non-blank non-comment lines with whitespace trimmed
	 * from front and back.
	 * @throws IOException If there is a low-level I/O error.
	 */
	public List<String> getLines(String resource) throws ErrorException {
		return getLines(resource, UTF_8);
	}

	/**
	 * Accesses a resource by name and returns the (non comment) lines containing
	 * data using the given character encoding.
	 *
	 * <p>
	 * A comment line is any line that starts with the character "#"
	 * </p>
	 *
	 * @param resource the file to be read
	 * @return a list of non-blank non-comment lines with whitespace trimmed
	 * @throws IOException If there is a low-level I/O error.
	 */
	public List<String> getLines(String resource, String encoding) throws ErrorException {
		return getLines(resource, Charset.forName(encoding));
	}

	public List<String> getLines(String resource, Charset charset) throws ErrorException {
		try {
			return readLines(openResourceAsStream(resource), charset);
		} catch (Exception ex) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Error loading resource (wrong encoding?): " + resource, ex);
		}
	}
	
	/**
	 * Accesses a resource by name and returns the (non comment) lines containing
	 * data using the given character encoding.
	 *
	 * <p>
	 * A comment line is any line that starts with the character "#"
	 * </p>
	 *
	 * @return a list of non-blank non-comment lines with whitespace trimmed
	 * @throws IOException If there is a low-level I/O error.
	 */
	static List<String> readLines(InputStream stream, Charset charset) 
			throws IOException {
		BufferedReader input = null;
		ArrayList<String> lines = null;
		
		boolean success = false;
		try {
			input = getBufferedReader(IOUtils.getDecodingReader(stream, charset));
			lines = new ArrayList<String>();
			
			for (String word=null; (word = input.readLine()) != null;) {
				// skip initial bom marker
				if (lines.isEmpty() && word.length() > 0 && word.charAt(0) == '\uFEFF')
					word = word.substring(1);
				
				// skip comments
				if (word.startsWith("#")) 
					continue;
				
				word = word.trim();
				
				// skip blank lines
				if (word.length() == 0) 
					continue;
				
				lines.add(word);
			}
			
			success = true;
			return lines;
			
		} finally {
			if (success) 
				IOUtils.close(input);
			else 
				IOUtils.closeWhileHandlingException(input);
		}
	}
  
	private static BufferedReader getBufferedReader(Reader reader) {
		return (reader instanceof BufferedReader) ? (BufferedReader) reader
				: new BufferedReader(reader);
	}
	
	/** A static map of short class name to fully qualified class name */
	private static final Map<String, String> sClassNameCache = 
			new ConcurrentHashMap<String, String>();
	
	/**
	 * This method loads a class either with it's FQN or a short-name 
	 * (falcon.class-simplename or class-simplename).
	 * It tries to load the class with the name that is given first and if it fails,
	 *  it tries all the known
	 * packages. This method caches the FQN of a short-name in a static map 
	 * in-order to make subsequent lookups
	 * for the same class faster. The caching is done only if the class 
	 * is loaded by the webapp classloader and it
	 * is loaded using a shortname.
	 *
	 * @param cname The name or the short name of the class.
	 * @param subpackages the packages to be tried if the cnams starts with server.
	 * @return the loaded class. An exception is thrown if it fails
	 */
	public <T> Class<? extends T> findClass(String cname, Class<T> expectedType) 
			throws ClassNotFoundException {
		if (LOG.isDebugEnabled()) {
			LOG.debug(getClass().getSimpleName() + ".findClass: cname=" + cname 
					+ " expectedType=" + expectedType);
		}
		
		final String projectName = getProjectName(); 
		final String projectBase = getProjectBase();
		final String[] projectPackages = getProjectPackages();
		
		Class<? extends T> clazz = findClassInternal(cname, expectedType, 
				projectName, projectBase, projectPackages, true);
		
		if (clazz == null && projectPackages != PACKAGES) {
			clazz = findClassInternal(cname, expectedType, 
					PROJECT, BASE, PACKAGES, false);
		}
		
		return clazz;
	}
	
	protected <T> Class<? extends T> lookupClassInternal(String cname, Class<T> expectedType) 
			throws ClassNotFoundException { 
		return null;
	}
	
	protected String normalizeClassName(String cname, String projectName) { 
		String newName = cname;
    	if (newName.startsWith(projectName + ".")) {
    		newName = cname.substring(projectName.length() + 1);
    	} else if (newName.startsWith(PROJECT + ".")) {
    		newName = cname.substring(PROJECT.length() + 1);
    	}
    	
    	return newName;
	}
	
	private <T> Class<? extends T> findClassInternal(String cname, 
			Class<T> expectedType, String projectName, String projectBase, String[] projectPackages, 
			boolean nothrow) throws ClassNotFoundException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("findClassInternal: cname=" + cname + " expectedType=" + expectedType 
					+ " projectName=" + projectName + " projectBase=" + projectBase 
					+ " projectPackages=" + projectPackages);
		}
		
		if (cname != null) {
			String  c = sClassNameCache.get(cname);
			if (c != null) {
				try {
					return Class.forName(c, true, mClassLoader).asSubclass(expectedType);
				} catch (ClassNotFoundException e) {
					//this is unlikely
					if (LOG.isErrorEnabled()) {
						LOG.error("Unable to load cached class-name :  " + c 
								+ " for shortname : " + cname + e);
					}
				}
			}
		}
		
		Class<? extends T> clazz = lookupClassInternal(cname, expectedType);
		if (clazz != null) 
			return clazz;
		
	    // first try cname == full name
	    try {
	    	return Class.forName(cname, true, mClassLoader).asSubclass(expectedType);
	    	
	    } catch (ClassNotFoundException e) {
	    	String newName = normalizeClassName(cname, projectName);
	    	
	    	for (String subpackage : projectPackages) {
	    		try {
	    			String name = projectBase + '.' + subpackage + newName;
	    			if (LOG.isDebugEnabled())
	    				LOG.debug("Trying class name " + name);
	    			
	    			return clazz = Class.forName(name,true,mClassLoader).asSubclass(expectedType);
	    		} catch (ClassNotFoundException e1) {
	    			// ignore... assume first exception is best.
	    		}
	    	}
	  
	    	if (nothrow) return null;
	    	
	    	throw e;
	    	//throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    	//		"Error loading class '" + cname + "'", e);
	    	
	    } finally {
	    	//cache the shortname vs FQN if it is loaded by the webapp classloader  and it is loaded
	    	// using a shortname
	    	if (clazz != null && clazz.getClassLoader() == ContextLoader.class.getClassLoader() &&
	    		!cname.equals(clazz.getName())) {
	    		//store in the cache
	    		sClassNameCache.put(cname, clazz.getName());
	    	}
	    }
	}
	
	public <T> T newInstance(String cname, Class<T> expectedType) 
			throws ClassNotFoundException {
	    Class<? extends T> clazz = findClass(cname, expectedType);
	    if (clazz == null) {
	    	throw new ClassNotFoundException("Can not find class: " 
	    			+ cname + " in " + mClassLoader);
	    }
	    
	    T obj = null;
	    try {
	    	obj = clazz.newInstance();
	    } catch (Exception e) {
	    	throw new RuntimeException("Error instantiating class: '" 
	    			+ clazz.getName() + "'", e);
	    }
	    
	    if (obj != null)
	    	onInstanceCreated(obj);
	    
	    return obj;
	}
	
	protected void onInstanceCreated(Object instance) { 
		// do nothing
	}
	
	/** 
	 * Creates an instance by trying a constructor that accepts a Core before
	 *  trying the default (no arg) constructor.
	 * @param className the instance class to create
	 * @param cast the class or interface that the instance should extend or implement
	 * @param msg a message helping compose the exception error if any occurs.
	 * @return the desired instance
	 * @throws ErrorException if the object could not be instantiated
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T,C> T createInstance(Class<C> initClass, C initObj, 
			String className, Class<T> cast) throws ErrorException {
		Class<? extends T> clazz = null;
		try {
			clazz = findClass(className, cast);
			
			// most of the classes do not have constructors which takes Core argument. 
			// It is recommended to obtain Core by implementing CoreAware.
			// So invariably always it will cause a  NoSuchMethodException. 
			// So iterate though the list of available constructors
			Constructor[] cons =  clazz.getConstructors();
			for (Constructor con : cons) {
				Class[] types = con.getParameterTypes();
				if (types.length == 1 && types[0] == initClass) 
					return (T)con.newInstance(initObj);
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Could not find Constructor(" + initClass.getName() 
						+ ") of Class: " + clazz.getName() + ", try the empty constructor.");
			}
			
			//use the empty constructor
			return newInstance(className, cast);
			
		} catch (Throwable e) {
			// The JVM likes to wrap our helpful ErrorExceptions in things like
			// "InvocationTargetException" that have no useful getMessage
			if (e instanceof ErrorException) 
				throw (ErrorException)e;
			
			if (e.getCause() != null && e.getCause() instanceof ErrorException) {
				ErrorException inner = (ErrorException) e.getCause();
				throw inner;
			}

			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Error Instantiating " + cast.getSimpleName() + ", " + className + 
					" failed to instantiate " + cast.getName(), 
					e);
		}
	}
	
	public <T,C> T createPlugin(Class<C> initClass, C initObj, 
			PluginInfo info, Class<T> cast, String defClassName) throws ErrorException {
		if (info == null) return null;
		
		T o = createInstance(initClass, initObj, 
				(info.getClassName() == null ? defClassName : info.getClassName()), 
				cast);
		
		if (o instanceof PluginInfoInitialized) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("init PluginInfoInitialized instance, class=" 
						+ o.getClass().getName() + ", pluginInfo=" + info);
			}
			
			((PluginInfoInitialized) o).init(info);
			
		} else if (o instanceof NamedListPlugin) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("init NamedListPlugin instance, class=" 
						+ o.getClass().getName() + ", pluginInfo=" + info);
			}
			
			((NamedListPlugin) o).init(info.getInitArgs());
		}
		
		return o;
	}
	
}
