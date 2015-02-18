package org.javenstudio.panda.analysis.synonym;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.ParseException;
import java.util.List;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.analysis.TokenComponents;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.common.util.Logger;
import org.javenstudio.panda.analysis.LowerCaseFilter;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.analysis.TokenizerFactory;
import org.javenstudio.panda.analysis.WhitespaceTokenizer;
import org.javenstudio.panda.util.ResourceLoader;
import org.javenstudio.panda.util.ResourceLoaderAware;

public final class SynonymFilterFactory extends TokenFilterFactory 
		implements ResourceLoaderAware {
	static final Logger LOG = Logger.getLogger(SynonymFilterFactory.class);
	
	private SynonymMap mMap;
	private boolean mIgnoreCase;
  
	@SuppressWarnings("resource")
	@Override
	public ITokenStream create(ITokenStream input) {
		// if the fst is null, it means there's actually no synonyms... 
		// just return the original stream as there is nothing to do here.
		return mMap.mFst == null ? input : new SynonymFilter(input, mMap, mIgnoreCase);
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		final boolean ignoreCase = getBoolean("ignoreCase", false); 
		String tf = getArgs().get("tokenizerFactory");
		
		mIgnoreCase = ignoreCase;

		final TokenizerFactory factory = tf == null ? null : 
			loadTokenizerFactory(loader, tf);
    
		Analyzer analyzer = new Analyzer() {
				@SuppressWarnings("resource")
				@Override
				public TokenComponents createComponents(String fieldName, Reader reader)
						throws IOException {
					Tokenizer tokenizer = factory == null ? 
							new WhitespaceTokenizer(reader) : factory.create(reader);
					ITokenStream stream = ignoreCase ? 
							new LowerCaseFilter(tokenizer) : tokenizer;
					return new TokenComponents(tokenizer, stream);
				}
			};

		String format = getArgs().get("format");
		try {
			if (format == null || format.equals("panda")) {
				// TODO: expose dedup as a parameter?
				mMap = loadPandaSynonyms(loader, true, analyzer);
				
			} else if (format.equals("wordnet")) {
				mMap = loadWordnetSynonyms(loader, true, analyzer);
				
			} else {
				// TODO: somehow make this more pluggable
				throw new IllegalArgumentException("Unrecognized synonyms format: " + format);
			}
		} catch (ParseException e) {
			throw new IOException("Exception thrown while loading synonyms", e);
		}
	}
  
	/**
	 * Load synonyms from the panda format, "format=panda".
	 */
	private SynonymMap loadPandaSynonyms(ResourceLoader loader, boolean dedup, 
			Analyzer analyzer) throws IOException, ParseException {
		final boolean expand = getBoolean("expand", true);
		final SynonymParser parser = new SynonymParser(dedup, expand, analyzer);
		
		String synonyms = getArgs().get("synonyms");
		if (synonyms == null) {
			if (LOG.isWarnEnabled())
				LOG.warn("Missing required argument 'synonyms'.");
			
		} else { 
			CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
	    
			File synonymFile = new File(synonyms);
			if (synonymFile.exists()) {
				decoder.reset();
				parser.add(getResourceAsReader(loader, synonyms, decoder));
				
			} else {
				List<String> files = splitFileNames(synonyms);
				for (String file : files) {
					decoder.reset();
					parser.add(getResourceAsReader(loader, file, decoder));
				}
			}
		}
		
		return parser.build();
	}
  
	/**
	 * Load synonyms from the wordnet format, "format=wordnet".
	 */
	private SynonymMap loadWordnetSynonyms(ResourceLoader loader, boolean dedup, 
			Analyzer analyzer) throws IOException, ParseException {
		final boolean expand = getBoolean("expand", true);
		final WordnetSynonymParser parser = new WordnetSynonymParser(dedup, expand, analyzer);
		
		String synonyms = getArgs().get("synonyms");
		if (synonyms == null) {
			if (LOG.isWarnEnabled())
				LOG.warn("Missing required argument 'synonyms'.");
			
		} else { 
			CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
	    
			File synonymFile = new File(synonyms);
			if (synonymFile.exists()) {
				decoder.reset();
				parser.add(getResourceAsReader(loader, synonyms, decoder));
				
			} else {
				List<String> files = splitFileNames(synonyms);
				for (String file : files) {
					decoder.reset();
					parser.add(getResourceAsReader(loader, file, decoder));
				}
			}
		}
		
		return parser.build();
	}
  
	private TokenizerFactory loadTokenizerFactory(ResourceLoader loader, 
			String cname) throws IOException {
		try {
			TokenizerFactory tokFactory = loader.newInstance(cname, TokenizerFactory.class);
			tokFactory.init(getArgs());
			if (tokFactory instanceof ResourceLoaderAware) 
				((ResourceLoaderAware) tokFactory).inform(loader);
			
			return tokFactory;
		} catch (ClassNotFoundException ex) { 
			throw new IOException(ex);
		}
	}
	
}
