package org.javenstudio.panda.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.panda.util.CharArraySet;
import org.javenstudio.panda.util.ResourceLoader;
import org.javenstudio.panda.util.ResourceLoaderAware;

/**
 * Factory for {@link WordDelimiterFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_wd" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.WordDelimiterFilterFactory" protected="protectedword.txt"
 *             preserveOriginal="0" splitOnNumerics="1" splitOnCaseChange="1"
 *             catenateWords="0" catenateNumbers="0" catenateAll="0"
 *             generateWordParts="1" generateNumberParts="1" stemEnglishPossessive="1"
 *             types="wdfftypes.txt" /&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */
public class WordDelimiterFilterFactory extends TokenFilterFactory 
		implements ResourceLoaderAware {
	
	public static final String PROTECTED_TOKENS = "protected";
	public static final String TYPES = "types";
  
	// source => type
	private static Pattern sTypePattern = Pattern.compile("(.*)\\s*=>\\s*(.*)\\s*$");
	
	private char[] mOut = new char[256];
	
	private CharArraySet mProtectedWords = null;
	private byte[] mTypeTable = null;
	private int mFlags;
	
	@Override
	public void inform(ResourceLoader loader) throws IOException {
		String wordFiles = getArgs().get(PROTECTED_TOKENS);
		if (wordFiles != null) 
			mProtectedWords = getWordSet(loader, wordFiles, false);
		
		String types = getArgs().get(TYPES);
		if (types != null) {
			List<String> files = splitFileNames( types );
			List<String> wlist = new ArrayList<String>();
			
			for (String file : files) {
				List<String> lines = getLines(loader, file.trim());
				wlist.addAll( lines );
			}
			
			mTypeTable = parseTypes(wlist);
		}
	}

	@Override
	public void init(Map<String, String> args) {
		super.init(args);
		
		if (getInt("generateWordParts", 1) != 0) 
			mFlags |= WordDelimiterFilter.GENERATE_WORD_PARTS;
		
		if (getInt("generateNumberParts", 1) != 0) 
			mFlags |= WordDelimiterFilter.GENERATE_NUMBER_PARTS;
		
		if (getInt("catenateWords", 0) != 0) 
			mFlags |= WordDelimiterFilter.CATENATE_WORDS;
		
		if (getInt("catenateNumbers", 0) != 0) 
			mFlags |= WordDelimiterFilter.CATENATE_NUMBERS;
		
		if (getInt("catenateAll", 0) != 0) 
			mFlags |= WordDelimiterFilter.CATENATE_ALL;
		
		if (getInt("splitOnCaseChange", 1) != 0) 
			mFlags |= WordDelimiterFilter.SPLIT_ON_CASE_CHANGE;
		
		if (getInt("splitOnNumerics", 1) != 0) 
			mFlags |= WordDelimiterFilter.SPLIT_ON_NUMERICS;
		
		if (getInt("preserveOriginal", 0) != 0) 
			mFlags |= WordDelimiterFilter.PRESERVE_ORIGINAL;
		
		if (getInt("stemEnglishPossessive", 1) != 0) 
			mFlags |= WordDelimiterFilter.STEM_ENGLISH_POSSESSIVE;
		
	}

	@Override
	public WordDelimiterFilter create(ITokenStream input) {
		return new WordDelimiterFilter(input, 
				mTypeTable == null ? WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE : mTypeTable,
				mFlags, mProtectedWords);
	}
  
	// parses a list of MappingCharFilter style rules into a custom byte[] type table
	private byte[] parseTypes(List<String> rules) {
		SortedMap<Character,Byte> typeMap = new TreeMap<Character,Byte>();
		for (String rule : rules) {
			Matcher m = sTypePattern.matcher(rule);
			if (!m.find())
				throw new IllegalArgumentException("Invalid Mapping Rule : [" + rule + "]");
			
			String lhs = parseString(m.group(1).trim());
			Byte rhs = parseType(m.group(2).trim());
			if (lhs.length() != 1) {
				throw new IllegalArgumentException("Invalid Mapping Rule : [" + rule 
						+ "]. Only a single character is allowed.");
			}
			
			if (rhs == null)
				throw new IllegalArgumentException("Invalid Mapping Rule : [" + rule + "]. Illegal type.");
			
			typeMap.put(lhs.charAt(0), rhs);
		}
    
		// ensure the table is always at least as big as DEFAULT_WORD_DELIM_TABLE for performance
		byte types[] = new byte[Math.max(typeMap.lastKey()+1, 
				WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE.length)];
		
		for (int i = 0; i < types.length; i++) {
			types[i] = WordDelimiterIterator.getType(i);
		}
		
		for (Map.Entry<Character,Byte> mapping : typeMap.entrySet()) {
			types[mapping.getKey()] = mapping.getValue();
		}
		
		return types;
	}
  
	private Byte parseType(String s) {
		if (s.equals("LOWER"))
			return WordDelimiterFilter.LOWER;
		else if (s.equals("UPPER"))
			return WordDelimiterFilter.UPPER;
		else if (s.equals("ALPHA"))
			return WordDelimiterFilter.ALPHA;
		else if (s.equals("DIGIT"))
			return WordDelimiterFilter.DIGIT;
		else if (s.equals("ALPHANUM"))
			return WordDelimiterFilter.ALPHANUM;
		else if (s.equals("SUBWORD_DELIM"))
			return WordDelimiterFilter.SUBWORD_DELIM;
		else
			return null;
	}
  
	private String parseString(String s) {
		int readPos = 0;
		int len = s.length();
		int writePos = 0;
		
		while (readPos < len) {
			char c = s.charAt( readPos++ );
			if (c == '\\') {
				if (readPos >= len)
					throw new IllegalArgumentException("Invalid escaped char in [" + s + "]");
				
				c = s.charAt(readPos++);
				switch (c) {
				case '\\' : c = '\\'; break;
				case 'n' : c = '\n'; break;
				case 't' : c = '\t'; break;
				case 'r' : c = '\r'; break;
				case 'b' : c = '\b'; break;
				case 'f' : c = '\f'; break;
				case 'u' :
					if (readPos + 3 >= len)
						throw new IllegalArgumentException("Invalid escaped char in [" + s + "]");
					
					c = (char)Integer.parseInt(s.substring(readPos, readPos + 4), 16);
					readPos += 4;
					
					break;
				}
			}
			
			mOut[writePos++] = c;
		}
		
		return new String(mOut, 0, writePos);
	}
	
}
