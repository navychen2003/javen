package org.javenstudio.panda.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.util.Logger;
import org.javenstudio.panda.util.CharArraySet;
import org.javenstudio.panda.util.ResourceLoader;
import org.javenstudio.panda.util.ResourceLoaderAware;
import org.javenstudio.panda.util.WordlistLoader;

/**
 * Abstract parent class for analysis factories {@link TokenizerFactory},
 * {@link TokenFilterFactory} and {@link CharFilterFactory}.
 * <p>
 * The typical lifecycle for a factory consumer is:
 * <ol>
 *   <li>Create factory via its a no-arg constructor
 *   <li>Set version emulation by calling {@link #setLuceneMatchVersion(Version)}
 *   <li>Calls {@link #init(Map)} passing arguments as key-value mappings.
 *   <li>(Optional) If the factory uses resources such as files, 
 *   {@link ResourceLoaderAware#inform(ResourceLoader)} is called to 
 *   initialize those resources.
 *   <li>Consumer calls create() to obtain instances.
 * </ol>
 */
public abstract class AnalysisFactory {
	static final Logger LOG = Logger.getLogger(AnalysisFactory.class);

	/** The init args */
	private Map<String,String> mArgs;

	/**
	 * Initialize this factory via a set of key-value pairs.
	 */
	public void init(Map<String,String> args) {
		mArgs = args;
	}

	public synchronized Map<String,String> getArgs() {
		if (mArgs == null) 
			mArgs = new HashMap<String,String>();
		
		return mArgs;
	}

	protected int getInt(String name) {
		return getInt(name, -1, false);
	}

	protected int getInt(String name, int defaultVal) {
		return getInt(name, defaultVal, true);
	}

	protected int getInt(String name, int defaultVal, boolean useDefault) {
		String s = getArgs().get(name);
		if (s == null) {
			if (useDefault) 
				return defaultVal;
			
			throw new IllegalArgumentException(
					"Configuration Error: missing parameter '" + name + "'");
		}
		
		return Integer.parseInt(s);
	}

	protected boolean getBoolean(String name, boolean defaultVal) {
		return getBoolean(name, defaultVal, true);
	}

	protected boolean getBoolean(String name, boolean defaultVal, boolean useDefault) {
		String s = getArgs().get(name);
		if (s == null) {
			if (useDefault) 
				return defaultVal;
			
			throw new IllegalArgumentException(
					"Configuration Error: missing parameter '" + name + "'");
		}
		
		return Boolean.parseBoolean(s);
	}

	/**
	 * Compiles a pattern for the value of the specified argument key <code>name</code> 
	 */
	protected Pattern getPattern(String name) {
		try {
			String pat = getArgs().get(name);
			if (pat == null) {
				throw new IllegalArgumentException(
						"Configuration Error: missing parameter '" + name + "'");
			}
			
			return Pattern.compile(mArgs.get(name));
			
		} catch (PatternSyntaxException e) {
			throw new IllegalArgumentException("Configuration Error: '" + name 
					+ "' can not be parsed in " + getClass().getSimpleName(), e);
		}
	}

	/**
	 * Returns as {@link CharArraySet} from wordFiles, which
	 * can be a comma-separated list of filenames
	 */
	protected CharArraySet getWordSet(ResourceLoader loader,
			String wordFiles, boolean ignoreCase) throws IOException {
		CharArraySet words = null;
		
		List<String> files = splitFileNames(wordFiles);
		if (files.size() > 0) {
			// default stopwords list has 35 or so words, but maybe don't make it that
			// big to start
			words = new CharArraySet(files.size() * 10, ignoreCase);
			
			for (String file : files) {
				List<String> wlist = getLines(loader, file.trim());
				words.addAll(StopFilter.makeStopSet(wlist, ignoreCase));
			}
		}
		
		return words;
	}
  
	/**
	 * Returns the resource's lines (with content treated as UTF-8)
	 */
	protected List<String> getLines(ResourceLoader loader, String resource) throws IOException {
		try { 
			InputStream is = loader.openResource(resource);
			return WordlistLoader.getLines(is, IOUtils.CHARSET_UTF_8);
			
		} catch (FileNotFoundException ex) { 
			if (LOG.isWarnEnabled()) 
				LOG.warn("Resource: " + resource + " not found, return empty set.");
			
			return new ArrayList<String>();
		}
	}

	protected Reader getResourceAsReader(ResourceLoader loader, String resource, 
			CharsetDecoder decoder) throws IOException { 
		try { 
			InputStream is = loader.openResource(resource);
			return new InputStreamReader(is, decoder);
			
		} catch (FileNotFoundException ex) { 
			if (LOG.isWarnEnabled()) 
				LOG.warn("Resource: " + resource + " not found, return empty reader.");
			
			return new StringReader("");
		}
	}
	
	/** 
	 * same as {@link #getWordSet(ResourceLoader, String, boolean)},
	 * except the input is in snowball format. 
	 */
	protected CharArraySet getSnowballWordSet(ResourceLoader loader,
			String wordFiles, boolean ignoreCase) throws IOException {
		CharArraySet words = null;
		
		List<String> files = splitFileNames(wordFiles);
		if (files.size() > 0) {
			// default stopwords list has 35 or so words, but maybe don't make it that
			// big to start
			words = new CharArraySet(files.size() * 10, ignoreCase);
			
			for (String file : files) {
				InputStream stream = null;
				Reader reader = null;
				try {
					stream = loader.openResource(file.trim());
					CharsetDecoder decoder = IOUtils.CHARSET_UTF_8.newDecoder()
							.onMalformedInput(CodingErrorAction.REPORT)
							.onUnmappableCharacter(CodingErrorAction.REPORT);
					
					reader = new InputStreamReader(stream, decoder);
					WordlistLoader.getSnowballWordSet(reader, words);
				} finally {
					IOUtils.closeWhileHandlingException(reader, stream);
				}
			}
		}
		
		return words;
	}

	/**
	 * Splits file names separated by comma character.
	 * File names can contain comma characters escaped by backslash '\'
	 *
	 * @param fileNames the string containing file names
	 * @return a list of file names with the escaping backslashed removed
	 */
	protected List<String> splitFileNames(String fileNames) {
		if (fileNames == null)
			return Collections.<String>emptyList();

		List<String> result = new ArrayList<String>();
		for (String file : fileNames.split("(?<!\\\\),")) {
			result.add(file.replaceAll("\\\\(?=,)", ""));
		}

		return result;
	}
	
}
