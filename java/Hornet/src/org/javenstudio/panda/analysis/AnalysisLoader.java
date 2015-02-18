package org.javenstudio.panda.analysis;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.ServiceConfigurationError;

import org.javenstudio.common.util.Logger;
import org.javenstudio.panda.util.ClassIterator;

/**
 * Helper class for loading named SPIs from classpath (e.g. Tokenizers, TokenStreams).
 * 
 */
final class AnalysisLoader<S extends AnalysisFactory> {
	private static final Logger LOG = Logger.getLogger(AnalysisLoader.class);

	private volatile Map<String,Class<? extends S>> mServices = Collections.emptyMap();
	
	private final Class<S> mClazz;
	private final String[] mSuffixes;
	private final Class<? extends S>[] mDefaultCls;
  
	public AnalysisLoader(Class<S> clazz) { 
		this(clazz, (Class<? extends S>[])null);
	}
  
	@SuppressWarnings("unchecked")
	public AnalysisLoader(Class<S> clazz, Class<? extends S>... defaults) {
		this(clazz, new String[] { clazz.getSimpleName() }, defaults);
	}

	public AnalysisLoader(Class<S> clazz, ClassLoader loader) { 
		this(clazz, loader, (Class<? extends S>[])null);
	}
  
	@SuppressWarnings("unchecked")
	public AnalysisLoader(Class<S> clazz, ClassLoader loader, Class<? extends S>... defaults) {
		this(clazz, new String[] { clazz.getSimpleName() }, loader, defaults);
	}

	public AnalysisLoader(Class<S> clazz, String[] suffixes) { 
		this(clazz, suffixes, (Class<? extends S>[])null);
	}
  
	@SuppressWarnings("unchecked")
	public AnalysisLoader(Class<S> clazz, String[] suffixes, Class<? extends S>... defaults) {
		this(clazz, suffixes, Thread.currentThread().getContextClassLoader(), defaults);
	}
  
	public AnalysisLoader(Class<S> clazz, String[] suffixes, ClassLoader classloader) { 
		this(clazz, suffixes, classloader, (Class<? extends S>[])null);
	}
  
	@SuppressWarnings("unchecked")
	public AnalysisLoader(Class<S> clazz, String[] suffixes, ClassLoader classloader, 
			Class<? extends S>... defaults) {
		mClazz = clazz;
		mSuffixes = suffixes;
		mDefaultCls = defaults;
		
		reload(classloader);
	}
  
	/** 
	 * Reloads the internal SPI list from the given {@link ClassLoader}.
	 * Changes to the service list are visible after the method ends, all
	 * iterators (e.g., from {@link #availableServices()},...) stay consistent. 
	 * 
	 * <p><b>NOTE:</b> Only new service providers are added, existing ones are
	 * never removed or replaced.
	 * 
	 * <p><em>This method is expensive and should only be called for discovery
	 * of new service providers on the given classpath/classloader!</em>
	 */
	public void reload(ClassLoader classloader) {
		final LinkedHashMap<String,Class<? extends S>> services = 
				new LinkedHashMap<String,Class<? extends S>>();
		if (mDefaultCls != null) { 
			for (Class<? extends S> service : mDefaultCls) { 
				putService(services, service);
			}
		}
		
		ClassIterator<S> loader = ClassIterator.get(mClazz, classloader);
		while (loader.hasNext()) {
			final Class<? extends S> service = loader.next();
			putService(services, service);
		}
		
		mServices = Collections.unmodifiableMap(services);
	}
  
	private void putService(final Map<String,Class<? extends S>> services, 
			final Class<? extends S> service) { 
		final String clazzName = service.getSimpleName();
		
		String name = null;
		for (String suffix : mSuffixes) {
			if (clazzName.endsWith(suffix)) {
				name = clazzName.substring(0, clazzName.length() - suffix.length())
						.toLowerCase(Locale.ROOT);
				break;
			}
		}
		
		if (name == null) {
			throw new ServiceConfigurationError("The class name " + service.getName() +
					" has wrong suffix, allowed are: " + Arrays.toString(mSuffixes));
		}
		
		// only add the first one for each name, later services will be ignored
		// this allows to place services before others in classpath to make 
		// them used instead of others
		//
		// TODO: Should we disallow duplicate names here?
		// Allowing it may get confusing on collisions, as different packages
		// could contain same factory class, which is a naming bug!
		// When changing this be careful to allow reload()!
		if (!services.containsKey(name)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("put service: name=" + name + " class=" + service.getName() 
						+ " for " + mClazz.getName());
			}
			
			services.put(name, service);
		}
	}
  
	public S newInstance(String name) {
		final Class<? extends S> service = lookupClass(name);
		try {
			return service.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("SPI class of type " + mClazz.getName() 
					+ " with name '" + name + "' cannot be instantiated. " 
					+ "This is likely due to a misconfiguration of the java class '" 
					+ service.getName() + "': ", e);
		}
	}
  
	public Class<? extends S> lookupClass(String name) {
		final Class<? extends S> service = mServices.get(name.toLowerCase(Locale.ROOT));
		if (service != null) {
			return service;
			
		} else {
			throw new IllegalArgumentException("A SPI class of type " + mClazz.getName() 
					+ " with name '" + name + "' does not exist. " 
					+ "You need to add the corresponding JAR file supporting this SPI to your classpath." 
					+ "The current classpath supports the following names: " + availableServices());
		}
	}

	public Set<String> availableServices() {
		return mServices.keySet();
	}
	
}
