package org.javenstudio.panda.analysis;

import java.util.Map;

import org.javenstudio.common.indexdb.ITokenStream;

/**
 * Factory for {@link ReversedWildcardFilter}-s. When this factory is
 * added to an analysis chain, it will be used both for filtering the
 * tokens during indexing, and to determine the query processing of
 * this field during search.
 * <p>This class supports the following init arguments:
 * <ul>
 * <li><code>withOriginal</code> - if true, then produce both original and reversed tokens at
 * the same positions. If false, then produce only reversed tokens.</li>
 * <li><code>maxPosAsterisk</code> - maximum position (1-based) of the asterisk wildcard
 * ('*') that triggers the reversal of query term. Asterisk that occurs at
 * positions higher than this value will not cause the reversal of query term.
 * Defaults to 2, meaning that asterisks on positions 1 and 2 will cause
 * a reversal.</li>
 * <li><code>maxPosQuestion</code> - maximum position (1-based) of the question
 * mark wildcard ('?') that triggers the reversal of query term. Defaults to 1.
 * Set this to 0, and <code>maxPosAsterisk</code> to 1 to reverse only
 * pure suffix queries (i.e. ones with a single leading asterisk).</li>
 * <li><code>maxFractionAsterisk</code> - additional parameter that
 * triggers the reversal if asterisk ('*') position is less than this
 * fraction of the query token length. Defaults to 0.0f (disabled).</li>
 * <li><code>minTrailing</code> - minimum number of trailing characters in query
 * token after the last wildcard character. For good performance this should be
 * set to a value larger than 1. Defaults to 2.
 * </ul>
 * Note 1: This filter always reverses input tokens during indexing.
 * Note 2: Query tokens without wildcard characters will never be reversed.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_rvswc" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer type="index"&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.ReversedWildcardFilterFactory" withOriginal="true"
 *             maxPosAsterisk="2" maxPosQuestion="1" minTrailing="2" maxFractionAsterisk="0"/&gt;
 *   &lt;/analyzer&gt;
 *   &lt;analyzer type="query"&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 */
public class ReversedWildcardFilterFactory extends TokenFilterFactory {
  
	private char mMarkerChar = ReverseStringFilter.START_OF_HEADING_MARKER;
	private float mMaxFractionAsterisk;
	private boolean mWithOriginal;
	private int mMaxPosAsterisk;
	private int mMaxPosQuestion;
	private int mMinTrailing;

	@Override
	public void init(Map<String, String> args) {
		super.init(args);
		
		mWithOriginal = getBoolean("withOriginal", true);
		mMaxPosAsterisk = getInt("maxPosAsterisk", 2);
		mMaxPosQuestion = getInt("maxPosQuestion", 1);
		mMinTrailing = getInt("minTrailing", 2);
		mMaxFractionAsterisk = getFloat("maxFractionAsterisk", 0.0f);
	}

	@Override
	public ITokenStream create(ITokenStream input) {
		return new ReversedWildcardFilter(input, mWithOriginal, mMarkerChar);
	}
  
	/**
	 * This method encapsulates the logic that determines whether
	 * a query token should be reversed in order to use the
	 * reversed terms in the index.
	 * @param token input token.
	 * @return true if input token should be reversed, false otherwise.
	 */
	public boolean shouldReverse(String token) {
		int posQ = token.indexOf('?');
		int posA = token.indexOf('*');
		if (posQ == -1 && posA == -1) // not a wildcard query
			return false;
		
		int pos;
		int lastPos;
		int len = token.length();
		
		lastPos = token.lastIndexOf('?');
		pos = token.lastIndexOf('*');
		if (pos > lastPos) 
			lastPos = pos;
		
		if (posQ != -1) {
			pos = posQ;
			if (posA != -1) 
				pos = Math.min(posQ, posA);
		} else {
			pos = posA;
		}
		
		if (len - lastPos < mMinTrailing) // too few trailing chars
			return false;
		
		if (posQ != -1 && posQ < mMaxPosQuestion) // leading '?'
			return true;
		
		if (posA != -1 && posA < mMaxPosAsterisk) // leading '*'
			return true;
		
		// '*' in the leading part
		if (mMaxFractionAsterisk > 0.0f && pos < (float)token.length() * mMaxFractionAsterisk) 
			return true;
		
		return false;
	}
  
	public char getMarkerChar() { return mMarkerChar; }
  
	protected float getFloat(String name, float defValue) {
		String val = getArgs().get(name);
		if (val == null) 
			return defValue;
		else 
			return Float.parseFloat(val);
	}
	
}
