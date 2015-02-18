package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

/**
 * Abstract parent class for analysis factories that create {@link CharFilter}
 * instances.
 */
public abstract class CharFilterFactory extends AnalysisFactory {

	@SuppressWarnings("unchecked")
	private static final AnalysisLoader<CharFilterFactory> sLoader =
			new AnalysisLoader<CharFilterFactory>(CharFilterFactory.class, 
					PatternReplaceCharFilterFactory.class);
  
	/** looks up a charfilter by name from context classpath */
	public static CharFilterFactory forName(String name) {
		return sLoader.newInstance(name);
	}
  
	/** looks up a charfilter class by name from context classpath */
	public static Class<? extends CharFilterFactory> lookupClass(String name) {
		return sLoader.lookupClass(name);
	}
  
	/** returns a list of all available charfilter names */
	public static Set<String> availableCharFilters() {
		return sLoader.availableServices();
	}

	/** 
	 * Reloads the factory list from the given {@link ClassLoader}.
	 * Changes to the factories are visible after the method ends, all
	 * iterators ({@link #availableCharFilters()},...) stay consistent. 
	 * 
	 * <p><b>NOTE:</b> Only new factories are added, existing ones are
	 * never removed or replaced.
	 * 
	 * <p><em>This method is expensive and should only be called for discovery
	 * of new factories on the given classpath/classloader!</em>
	 */
	public static void reloadCharFilters(ClassLoader classloader) {
		sLoader.reload(classloader);
	}

	/** Wraps the given Reader with a CharFilter. */
	public abstract Reader create(Reader input) throws IOException;
	
}
