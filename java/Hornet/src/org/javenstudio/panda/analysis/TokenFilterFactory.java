package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.panda.analysis.payloads.DelimitedPayloadTokenFilterFactory;
import org.javenstudio.panda.analysis.synonym.SynonymFilterFactory;
import org.javenstudio.panda.language.english.EnglishMinimalStemFilterFactory;
import org.javenstudio.panda.language.english.EnglishPossessiveFilterFactory;
import org.javenstudio.panda.language.english.PorterStemFilterFactory;

/**
 * Abstract parent class for analysis factories that create {@link TokenFilter}
 * instances.
 */
public abstract class TokenFilterFactory extends AnalysisFactory {

	@SuppressWarnings("unchecked")
	private static final AnalysisLoader<TokenFilterFactory> sLoader =
			new AnalysisLoader<TokenFilterFactory>(TokenFilterFactory.class,
					new String[] { "TokenFilterFactory", "FilterFactory" }, 
					StopFilterFactory.class, 
					SynonymFilterFactory.class, 
					LowerCaseFilterFactory.class, 
					EnglishMinimalStemFilterFactory.class, 
					EnglishPossessiveFilterFactory.class, 
					KeywordMarkerFilterFactory.class, 
					PorterStemFilterFactory.class, 
					WordDelimiterFilterFactory.class, 
					RemoveDuplicatesTokenFilterFactory.class, 
					ReversedWildcardFilterFactory.class, 
					ReverseStringFilterFactory.class, 
					TrimFilterFactory.class, 
					PatternReplaceFilterFactory.class, 
					DoubleMetaphoneFilterFactory.class, 
					DelimitedPayloadTokenFilterFactory.class);
  
	/** looks up a tokenfilter by name from context classpath */
	public static TokenFilterFactory forName(String name) {
		return sLoader.newInstance(name);
	}
  
	/** looks up a tokenfilter class by name from context classpath */
	public static Class<? extends TokenFilterFactory> lookupClass(String name) {
		return sLoader.lookupClass(name);
	}
  
	/** returns a list of all available tokenfilter names from context classpath */
	public static Set<String> availableTokenFilters() {
		return sLoader.availableServices();
	}
  
	/** 
	 * Reloads the factory list from the given {@link ClassLoader}.
	 * Changes to the factories are visible after the method ends, all
	 * iterators ({@link #availableTokenFilters()},...) stay consistent. 
	 * 
	 * <p><b>NOTE:</b> Only new factories are added, existing ones are
	 * never removed or replaced.
	 * 
	 * <p><em>This method is expensive and should only be called for discovery
	 * of new factories on the given classpath/classloader!</em>
	 */
	public static void reloadTokenFilters(ClassLoader classloader) {
		sLoader.reload(classloader);
	}

	/** Transform the specified input TokenStream */
	public abstract ITokenStream create(ITokenStream input) throws IOException;
	
}
