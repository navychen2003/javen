package org.javenstudio.panda.analysis.standard;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.common.indexdb.util.CharTerm;

/** 
 * A grammar-based tokenizer constructed with JFlex.
 * <p>
 * As of Lucene version 3.1, this class implements the Word Break rules from the
 * Unicode Text Segmentation algorithm, as specified in 
 * <a href="http://unicode.org/reports/tr29/">Unicode Standard Annex #29</a>.
 * <p/>
 * <p>Many applications have specific tokenizer needs.  If this tokenizer does
 * not suit your application, please consider copying this source code
 * directory to your project and maintaining your own grammar-based tokenizer.
 *
 * <a name="version"/>
 * <p>You must specify the required {@link Version}
 * compatibility when creating StandardTokenizer:
 * <ul>
 *   <li> As of 3.4, Hiragana and Han characters are no longer wrongly split
 *   from their combining characters. If you use a previous version number,
 *   you get the exact broken behavior for backwards compatibility.
 *   <li> As of 3.1, StandardTokenizer implements Unicode text segmentation.
 *   If you use a previous version number, you get the exact behavior of
 *   {@link ClassicTokenizer} for backwards compatibility.
 * </ul>
 */
public final class StandardTokenizer extends Tokenizer {
	//private static final Logger LOG = Logger.getLogger(StandardTokenizer.class);

	public static final int ALPHANUM          = 0;
	/** @deprecated (3.1) */
	@Deprecated
	public static final int APOSTROPHE        = 1;
	/** @deprecated (3.1) */
	@Deprecated
	public static final int ACRONYM           = 2;
	/** @deprecated (3.1) */
	@Deprecated
	public static final int COMPANY           = 3;
	public static final int EMAIL             = 4;
	/** @deprecated (3.1) */
	@Deprecated
	public static final int HOST              = 5;
	public static final int NUM               = 6;
	/** @deprecated (3.1) */
	@Deprecated
	public static final int CJ                = 7;

	/** @deprecated (3.1) */
	@Deprecated
	public static final int ACRONYM_DEP       = 8;

	public static final int SOUTHEAST_ASIAN = 9;
	public static final int IDEOGRAPHIC = 10;
	public static final int HIRAGANA = 11;
	public static final int KATAKANA = 12;
	public static final int HANGUL = 13;
  
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
		"<ACRONYM_DEP>",
		"<SOUTHEAST_ASIAN>",
		"<IDEOGRAPHIC>",
		"<HIRAGANA>",
		"<KATAKANA>",
		"<HANGUL>"
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
	 * Creates a new instance of the {@link StandardTokenizer}.  Attaches
	 * the <code>input</code> to the newly created JFlex scanner.
	 *
	 * @param input The input reader
	 *
	 * See http://issues.apache.org/jira/browse/LUCENE-1068
	 */
	public StandardTokenizer(Reader input) {
		super(input);
		mScanner = new StandardTokenizerImpl(input);
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
				
				// This 'if' should be removed in the next release. For now, it converts
				// invalid acronyms to HOST. When removed, only the 'else' part should
				// remain.
				if (tokenType == StandardTokenizer.ACRONYM_DEP) {
					mToken.setType(StandardTokenizer.TOKEN_TYPES[StandardTokenizer.HOST]);
					mTerm.setLength(mTerm.length() - 1); // remove extra '.'
					
				} else {
					mToken.setType(StandardTokenizer.TOKEN_TYPES[tokenType]);
				}
				
				//if (LOG.isDebugEnabled())
				//	LOG.debug("nextToken: " + mToken);
				
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
