package org.javenstudio.panda.analysis.synonym;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.ParseException;

import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.util.CharsRef;

/**
 * Parser for wordnet prolog format
 * <p>
 * See http://wordnet.princeton.edu/man/prologdb.5WN.html for a description of the format.
 * 
 */
// TODO: allow you to specify syntactic categories (e.g. just nouns, etc)
public class WordnetSynonymParser extends SynonymMap.Builder {
	
	private final boolean mExpand;
	private final Analyzer mAnalyzer;
  
	public WordnetSynonymParser(boolean dedup, boolean expand, Analyzer analyzer) {
		super(dedup);
		
		mExpand = expand;
		mAnalyzer = analyzer;
	}
  
	public void add(Reader in) throws IOException, ParseException {
		LineNumberReader br = new LineNumberReader(in);
		try {
			String line = null;
			String lastSynSetID = "";
			
			CharsRef synset[] = new CharsRef[8];
			int synsetSize = 0;
      
			while ((line = br.readLine()) != null) {
				String synSetID = line.substring(2, 11);

				if (!synSetID.equals(lastSynSetID)) {
					addInternal(synset, synsetSize);
					synsetSize = 0;
				}

				if (synset.length <= synsetSize+1) {
					CharsRef larger[] = new CharsRef[synset.length * 2];
					System.arraycopy(synset, 0, larger, 0, synsetSize);
					synset = larger;
				}
        
				synset[synsetSize] = parseSynonym(line, synset[synsetSize]);
				synsetSize ++;
				lastSynSetID = synSetID;
			}
      
			// final synset in the file
			addInternal(synset, synsetSize);
			
		} catch (IllegalArgumentException e) {
			ParseException ex = new ParseException("Invalid synonym rule at line " + br.getLineNumber(), 0);
			ex.initCause(e);
			throw ex;
			
		} finally {
			br.close();
		}
	}
 
	private CharsRef parseSynonym(String line, CharsRef reuse) throws IOException {
		if (reuse == null) 
			reuse = new CharsRef(8);
    
		int start = line.indexOf('\'')+1;
		int end = line.lastIndexOf('\'');
    
		String text = line.substring(start, end).replace("''", "'");
		
		return analyze(mAnalyzer, text, reuse);
	}
  
	private void addInternal(CharsRef synset[], int size) {
		if (size <= 1) 
			return; // nothing to do
    
		if (mExpand) {
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					add(synset[i], synset[j], false);
				}
			}
		} else {
			for (int i = 0; i < size; i++) {
				add(synset[i], synset[0], false);
			}
		}
	}
	
}
