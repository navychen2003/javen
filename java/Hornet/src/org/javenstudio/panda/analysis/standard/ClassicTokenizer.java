package org.javenstudio.panda.analysis.standard;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.common.indexdb.util.CharTerm;

/** 
 * A grammar-based tokenizer constructed with JFlex
 *
 * <p> This should be a good tokenizer for most European-language documents:
 *
 * <ul>
 *   <li>Splits words at punctuation characters, removing punctuation. However, a 
 *     dot that's not followed by whitespace is considered part of a token.
 *   <li>Splits words at hyphens, unless there's a number in the token, in which case
 *     the whole token is interpreted as a product number and is not split.
 *   <li>Recognizes email addresses and internet hostnames as one token.
 * </ul>
 *
 * <p>Many applications have specific tokenizer needs.  If this tokenizer does
 * not suit your application, please consider copying this source code
 * directory to your project and maintaining your own grammar-based tokenizer.
 *
 * ClassicTokenizer was named StandardTokenizer in Lucene versions prior to 3.1.
 * As of 3.1, {@link StandardTokenizer} implements Unicode text segmentation,
 * as specified by UAX#29.
 */
public final class ClassicTokenizer extends Tokenizer {

	public static final int ALPHANUM          = 0;
	public static final int APOSTROPHE        = 1;
	public static final int ACRONYM           = 2;
	public static final int COMPANY           = 3;
	public static final int EMAIL             = 4;
	public static final int HOST              = 5;
	public static final int NUM               = 6;
	public static final int CJ                = 7;

	public static final int ACRONYM_DEP       = 8;

	/** String token types that correspond to token type int constants */
	public static final String [] TOKEN_TYPES = new String [] {
		"<ALPHANUM>",
		"<APOSTROPHE>",
		"<ACRONYM>",
		"<COMPANY>",
		"<EMAIL>",
		"<HOST>",
		"<NUM>",
		"<CJ>",
		"<ACRONYM_DEP>"
	};

	private final CharTerm mTerm = new CharTerm();
	private final CharToken mToken = new CharToken(mTerm);
	
	/** A private instance of the JFlex-constructed scanner */
	private final StandardTokenizerInterface mScanner;
	
	private int mMaxTokenLength = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

  	/** 
  	 * Set the max allowed token length.  Any token longer
  	 *  than this is skipped. 
  	 */
  	public void setMaxTokenLength(int length) {
  		mMaxTokenLength = length;
  	}

  	/** @see #setMaxTokenLength */
  	public int getMaxTokenLength() {
  		return mMaxTokenLength;
  	}

  	/**
  	 * Creates a new instance of the {@link ClassicTokenizer}.  Attaches
  	 * the <code>input</code> to the newly created JFlex scanner.
  	 *
  	 * @param input The input reader
  	 *
  	 * See http://issues.apache.org/jira/browse/LUCENE-1068
  	 */
  	public ClassicTokenizer(Reader input) {
  		super(input);
  		mScanner = new ClassicTokenizerImpl(input);
  	}

  	/**
  	 * (non-Javadoc)
  	 *
  	 * @see TokenStream#next()
  	 */
  	@Override
  	public final IToken nextToken() throws IOException {
  		mToken.clear();
  		int posIncr = 1;

  		while (true) {
  			int tokenType = mScanner.getNextToken();
  			if (tokenType == StandardTokenizerInterface.YYEOF) 
  				return null;

  			if (mScanner.yylength() <= mMaxTokenLength) {
  				mToken.setPositionIncrement(posIncr);
  				mScanner.getText(mTerm);
  				
  				final int start = mScanner.yychar();
  				mToken.setOffset(correctOffset(start), correctOffset(start+mTerm.length()));

  				if (tokenType == ClassicTokenizer.ACRONYM_DEP) {
  					mToken.setType(ClassicTokenizer.TOKEN_TYPES[ClassicTokenizer.HOST]);
  					mTerm.setLength(mTerm.length() - 1); // remove extra '.'
  				} else {
  					mToken.setType(ClassicTokenizer.TOKEN_TYPES[tokenType]);
  				}
  				
  				return mToken;
  				
  			} else {
  				// When we skip a too-long term, we still increment the
  				// position increment
  				posIncr ++;
  			}
  		}
  	}
  
  	@Override
  	public final int end() {
  		// set final offset
  		int finalOffset = correctOffset(mScanner.yychar() + mScanner.yylength());
  		return finalOffset;
  	}

  	@Override
  	public void reset() throws IOException {
  		mScanner.yyreset(getInput());
  	}
  	
}
