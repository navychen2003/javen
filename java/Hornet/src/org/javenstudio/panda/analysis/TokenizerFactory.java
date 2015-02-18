package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.panda.analysis.standard.StandardTokenizerFactory;

/**
 * Abstract parent class for analysis factories that create {@link Tokenizer}
 * instances.
 */
public abstract class TokenizerFactory extends AnalysisFactory {

	@SuppressWarnings("unchecked")
	private static final AnalysisLoader<TokenizerFactory> sLoader =
			new AnalysisLoader<TokenizerFactory>(TokenizerFactory.class, 
					KeywordTokenizerFactory.class, 
					WhitespaceTokenizerFactory.class, 
					StandardTokenizerFactory.class, 
					LowerCaseTokenizerFactory.class, 
					PathHierarchyTokenizerFactory.class);
  
	/** looks up a tokenizer by name from context classpath */
	public static TokenizerFactory forName(String name) {
		return sLoader.newInstance(name);
	}
  
	/** looks up a tokenizer class by name from context classpath */
	public static Class<? extends TokenizerFactory> lookupClass(String name) {
		return sLoader.lookupClass(name);
	}
  
	/** returns a list of all available tokenizer names from context classpath */
	public static Set<String> availableTokenizers() {
		return sLoader.availableServices();
	}
  
	/** 
	 * Reloads the factory list from the given {@link ClassLoader}.
	 * Changes to the factories are visible after the method ends, all
	 * iterators ({@link #availableTokenizers()},...) stay consistent. 
	 * 
	 * <p><b>NOTE:</b> Only new factories are added, existing ones are
	 * never removed or replaced.
	 * 
	 * <p><em>This method is expensive and should only be called for discovery
	 * of new factories on the given classpath/classloader!</em>
	 */
	public static void reloadTokenizers(ClassLoader classloader) {
		sLoader.reload(classloader);
	}

	/** Creates a TokenStream of the specified input */
	public abstract Tokenizer create(Reader input) throws IOException;
	
}
