package org.javenstudio.panda.query;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IBooleanQuery;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CachingTokenFilter;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.query.BooleanClause;
import org.javenstudio.hornet.search.query.MultiPhraseQuery;
import org.javenstudio.hornet.search.query.PhraseQuery;
import org.javenstudio.hornet.util.DateTools;

public class QueryParserHelper {

	static void addClause(QueryParserBase parser, 
			List<IBooleanClause> clauses, int conj, int mods, IQuery q) {
		boolean required, prohibited;

		// If this term is introduced by AND, make the preceding term required,
		// unless it's already prohibited
		if (clauses.size() > 0 && conj == QueryParserBase.CONJ_AND) {
			BooleanClause c = (BooleanClause)clauses.get(clauses.size()-1);
			if (!c.isProhibited())
				c.setOccur(IBooleanClause.Occur.MUST);
		}

		if (clauses.size() > 0 && parser.mOperator == QueryParserBase.AND_OPERATOR && 
				conj == QueryParserBase.CONJ_OR) {
			// If this term is introduced by OR, make the preceding term optional,
			// unless it's prohibited (that means we leave -a OR b but +a OR b-->a OR b)
			// notice if the input is a OR b, first term is parsed as required; without
			// this modification a OR b would parsed as +a OR b
			BooleanClause c = (BooleanClause)clauses.get(clauses.size()-1);
			if (!c.isProhibited())
				c.setOccur(IBooleanClause.Occur.SHOULD);
		}

		// We might have been passed a null query; the term might have been
		// filtered away by the analyzer.
		if (q == null)
			return;

		if (parser.mOperator == QueryParserBase.OR_OPERATOR) {
			// We set REQUIRED if we're introduced by AND or +; PROHIBITED if
			// introduced by NOT or -; make sure not to set both.
			prohibited = (mods == QueryParserBase.MOD_NOT);
			required = (mods == QueryParserBase.MOD_REQ);
			
			if (conj == QueryParserBase.CONJ_AND && !prohibited) 
				required = true;
			
		} else {
			// We set PROHIBITED if we're introduced by NOT or -; We set REQUIRED
			// if not PROHIBITED and not introduced by OR
			prohibited = (mods == QueryParserBase.MOD_NOT);
			required   = (!prohibited && conj != QueryParserBase.CONJ_OR);
		}
		
		if (required && !prohibited)
			clauses.add(parser.newBooleanClause(q, BooleanClause.Occur.MUST));
		else if (!required && !prohibited)
			clauses.add(parser.newBooleanClause(q, BooleanClause.Occur.SHOULD));
		else if (!required && prohibited)
			clauses.add(parser.newBooleanClause(q, BooleanClause.Occur.MUST_NOT));
		else
			throw new RuntimeException("Clause cannot be both required and prohibited");
	}
	
	static BytesRef analyzeMultitermTerm(QueryParserBase parser, 
			String field, String part, IAnalyzer analyzerIn) {
		ITokenStream source;
		if (analyzerIn == null) 
			analyzerIn = parser.mAnalyzer;

		try {
			source = analyzerIn.tokenStream(field, new StringReader(part));
			source.reset();
		} catch (IOException e) {
			throw new RuntimeException("Unable to initialize TokenStream to " + 
					"analyze multiTerm term: " + part, e);
		}
      
		BytesRef bytes = null; 

		try {
			IToken token = source.nextToken();
			if (token == null) {
				throw new IllegalArgumentException("analyzer returned no terms for " + 
						"multiTerm term: " + part);
			}
			
			token.fillBytesRef();
			bytes = token.getBytesRef();
			token = source.nextToken();
			
			if (token != null) {
				throw new IllegalArgumentException("analyzer returned too many terms for " + 
						"multiTerm term: " + part);
			}
		} catch (IOException e) {
			throw new RuntimeException("error analyzing range part: " + part, e);
		}
      
		try {
			source.end();
			source.close();
			
		} catch (IOException e) {
			throw new RuntimeException("Unable to end & close TokenStream after analyzing " + 
					"multiTerm term: " + part, e);
		}
    
		return BytesRef.deepCopyOf(bytes);
	}
	
	static IQuery getRangeQuery(QueryParserBase parser, 
			String field, String part1, String part2, boolean startInclusive, 
			boolean endInclusive) throws ParseException {
		if (parser.mLowercaseExpandedTerms) {
			part1 = part1 == null ? null : part1.toLowerCase(parser.mLocale);
			part2 = part2 == null ? null : part2.toLowerCase(parser.mLocale);
		}

		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, parser.mLocale);
		df.setLenient(true);
		
		DateTools.Resolution resolution = parser.getDateResolution(field);
		try {
			part1 = DateTools.dateToString(df.parse(part1), resolution);
		} catch (Exception e) { 
			// do nothing
		}

		try {
			Date d2 = df.parse(part2);
			if (endInclusive) {
				// The user can only specify the date, not the time, so make sure
				// the time is set to the latest possible time of that date to really
				// include all documents:
				Calendar cal = Calendar.getInstance(parser.mTimeZone, parser.mLocale);
				cal.setTime(d2);
				
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 59);
				cal.set(Calendar.SECOND, 59);
				cal.set(Calendar.MILLISECOND, 999);
				
				d2 = cal.getTime();
			}
			
			part2 = DateTools.dateToString(d2, resolution);
		} catch (Exception e) { 
			// do nothing
		}

		return parser.newRangeQuery(field, part1, part2, startInclusive, endInclusive);
	}
	
	static IQuery newFieldQuery(QueryParserBase parser, IAnalyzer analyzer, 
			String field, String queryText, boolean quoted)  throws ParseException {
		// Use the analyzer to get all the tokens, and then build a TermQuery,
		// PhraseQuery, or nothing based on the term count
		ITokenStream source;
		try {
			source = analyzer.tokenStream(field, new StringReader(queryText));
			source.reset();
			
		} catch (IOException e) {
			ParseException p = new ParseException(
					"Unable to initialize TokenStream to analyze query text");
			p.initCause(e);
			throw p;
		}
		
		@SuppressWarnings("resource")
		CachingTokenFilter buffer = new CachingTokenFilter(source);
		buffer.reset();

		boolean severalTokensAtSamePosition = false;
		int numTokens = 0;
		int positionCount = 0;
		
		try {
			IToken token = null;
			
			while ((token = buffer.nextToken()) != null) {
				numTokens++;
				
				int positionIncrement = token.getPositionIncrement();
				if (positionIncrement != 0) 
					positionCount += positionIncrement;
				else 
					severalTokensAtSamePosition = true;
			}
		} catch (IOException e) {
			// ignore
		}
		
		try {
			// rewind the buffer stream
			buffer.reset();

			// close original stream - all tokens buffered
			source.close();
			
		} catch (IOException e) {
			ParseException p = new ParseException(
					"Cannot close TokenStream analyzing query text");
			p.initCause(e);
			throw p;
		}

		if (numTokens == 0) 
			return null;
		
		BytesRef bytes = new BytesRef();
		
    	if (numTokens == 1) {
    		try {
    			IToken nextToken = buffer.nextToken();
    			assert nextToken != null;
    			
    			nextToken.fillBytesRef(bytes);
    		} catch (IOException e) {
    			// safe to ignore, because we know the number of tokens
    		}
    		
    		return parser.newTermQuery(new Term(field, bytes));
    		
    	} else {
    		if (severalTokensAtSamePosition || (!quoted && !parser.mAutoGeneratePhraseQueries)) {
    			if (positionCount == 1 || (!quoted && !parser.mAutoGeneratePhraseQueries)) {
    				// no phrase query:
    				IBooleanQuery q = parser.newBooleanQuery(positionCount == 1);

    				IBooleanClause.Occur occur = positionCount > 1 && 
    						parser.mOperator == QueryParserBase.AND_OPERATOR ?
    						BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

    				for (int i = 0; i < numTokens; i++) {
    					try {
    						IToken nextToken = buffer.nextToken();
    						assert nextToken != null;
    						
    						nextToken.fillBytesRef(bytes);
    					} catch (IOException e) {
    						// safe to ignore, because we know the number of tokens
    					}
    					
    					IQuery currentQuery = parser.newTermQuery(new Term(field, BytesRef.deepCopyOf(bytes)));
    					q.add(currentQuery, occur);
    				}
    				
    				return q;
    				
    			} else {
    				// phrase query:
    				MultiPhraseQuery mpq = parser.newMultiPhraseQuery();
    				mpq.setSlop(parser.mPhraseSlop);
    				
    				List<Term> multiTerms = new ArrayList<Term>();
    				int position = -1;
    				
    				for (int i = 0; i < numTokens; i++) {
    					int positionIncrement = 1;
    					
    					try {
    						IToken nextToken = buffer.nextToken();
    						assert nextToken != null;
    						
    						nextToken.fillBytesRef(bytes);
    						positionIncrement = nextToken.getPositionIncrement();
    						
    					} catch (IOException e) {
    						// safe to ignore, because we know the number of tokens
    					}

    					if (positionIncrement > 0 && multiTerms.size() > 0) {
    						if (parser.mEnablePositionIncrements) 
    							mpq.add(multiTerms.toArray(new Term[0]), position);
    						else 
    							mpq.add(multiTerms.toArray(new Term[0]));
    						
    						multiTerms.clear();
    					}
    					
    					position += positionIncrement;
    					multiTerms.add(new Term(field, BytesRef.deepCopyOf(bytes)));
    				}
    				
    				if (parser.mEnablePositionIncrements) 
    					mpq.add(multiTerms.toArray(new Term[0]), position);
    				else 
    					mpq.add(multiTerms.toArray(new Term[0]));
    				
    				return mpq;
    			}
    			
    		} else {
    			PhraseQuery pq = parser.newPhraseQuery();
    			pq.setSlop(parser.mPhraseSlop);
    			
    			int position = -1;

    			for (int i = 0; i < numTokens; i++) {
    				int positionIncrement = 1;

    				try {
    					IToken nextToken = buffer.nextToken();
    					assert nextToken != null;
    					
    					nextToken.fillBytesRef(bytes);
    					positionIncrement = nextToken.getPositionIncrement();
    					
    				} catch (IOException e) {
    					// safe to ignore, because we know the number of tokens
    				}

    				if (parser.mEnablePositionIncrements) {
    					position += positionIncrement;
    					pq.add(new Term(field, BytesRef.deepCopyOf(bytes)), position);
    					
    				} else {
    					pq.add(new Term(field, BytesRef.deepCopyOf(bytes)));
    				}
    			}
    			
    			return pq;
    		}
    	}
	}
	
	// extracted from the .jj grammar
	static IQuery handleBareTokenQuery(QueryParserBase parser, 
			String qfield, ParseToken term, ParseToken fuzzySlop, boolean prefix, boolean wildcard, 
			boolean fuzzy, boolean regexp) throws ParseException {
		String charTermImage = term.mImage;
		String termImage = discardEscapeChar(charTermImage);
		IQuery q;
		
		if (wildcard) {
			q = parser.getWildcardQuery(qfield, charTermImage);
			
		} else if (prefix) {
			q = parser.getPrefixQuery(qfield, discardEscapeChar(
					charTermImage.substring(0, charTermImage.length()-1)));
			
		} else if (regexp) {
			q = parser.getRegexpQuery(qfield, charTermImage.substring(1, charTermImage.length()-1));
			
		} else if (fuzzy) {
			String fuzzySlopTermImage = fuzzySlop.mImage;
			float fms = parser.mFuzzyMinSim;
			try {
				fms = Float.valueOf(fuzzySlopTermImage.substring(1)).floatValue();
			} catch (Exception ignored) { 
				// ignore
			}
			
			if (fms < 0.0f) {
				throw new ParseException("Minimum similarity for a FuzzyQuery " + 
						"has to be between 0.0f and 1.0f !");
				
			} else if (fms >= 1.0f && fms != (int) fms) {
				throw new ParseException("Fractional edit distances are not allowed!");
			}
			
			q = parser.getFuzzyQuery(qfield, termImage, fms);
			
		} else {
			q = parser.getFieldQuery(qfield, termImage, false);
		}
		
		return q;
	}

	// extracted from the .jj grammar
	static IQuery handleQuotedTerm(QueryParserBase parser, 
			String qfield, ParseToken term, ParseToken fuzzySlop) throws ParseException {
	    String charTermImage = term.mImage;
	    
	    int s = parser.mPhraseSlop;  // default
	    if (fuzzySlop != null) {
	    	String fuzzySlopTermImage = fuzzySlop.mImage;
	    	try {
	    		s = Float.valueOf(fuzzySlopTermImage.substring(1)).intValue();
	    	} catch (Exception ignored) { 
	    		// ignore
	    	}
	    }
	    
	    return parser.getFieldQuery(qfield, discardEscapeChar(
	    		charTermImage.substring(1, charTermImage.length()-1)), s);
	}

	// extracted from the .jj grammar
	static IQuery handleBoost(IQuery q, ParseToken boost) {
		if (boost != null) {
			String boostTermImage = boost.mImage;
			float f = (float) 1.0;
			try {
				f = Float.valueOf(boostTermImage).floatValue();
				
			} catch (Exception ignored) {
				// Should this be handled somehow? (defaults to "no boost", if
				// boost number is invalid)
			}

			// avoid boosting null queries, such as those caused by stop words
			if (q != null) 
				q.setBoost(f);
		}
		
		return q;
	}

	/**
	 * Returns a String where the escape char has been
	 * removed, or kept only once if there was a double escape.
	 *
	 * Supports escaped unicode characters, e. g. translates
	 * <code>\\u0041</code> to <code>A</code>.
	 */
	static String discardEscapeChar(String input) throws ParseException {
		// Create char array to hold unescaped char sequence
		char[] output = new char[input.length()];

		// The length of the output can be less than the input
		// due to discarded escape chars. This variable holds
		// the actual length of the output
		int length = 0;

		// We remember whether the last processed character was
		// an escape character
		boolean lastCharWasEscapeChar = false;

		// The multiplier the current unicode digit must be multiplied with.
		// E. g. the first digit must be multiplied with 16^3, the second with 16^2...
		int codePointMultiplier = 0;

		// Used to calculate the codepoint of the escaped unicode character
		int codePoint = 0;

		for (int i = 0; i < input.length(); i++) {
			char curChar = input.charAt(i);
			
			if (codePointMultiplier > 0) {
				codePoint += hexToInt(curChar) * codePointMultiplier;
				codePointMultiplier >>>= 4;
				
				if (codePointMultiplier == 0) {
					output[length++] = (char)codePoint;
					codePoint = 0;
				}
				
			} else if (lastCharWasEscapeChar) {
				if (curChar == 'u') {
					// found an escaped unicode character
					codePointMultiplier = 16 * 16 * 16;
					
				} else {
					// this character was escaped
					output[length] = curChar;
					length ++;
				}
				
				lastCharWasEscapeChar = false;
				
			} else {
				if (curChar == '\\') {
					lastCharWasEscapeChar = true;
					
				} else {
					output[length] = curChar;
					length++;
				}
			}
		}

		if (codePointMultiplier > 0) 
			throw new ParseException("Truncated unicode escape sequence.");

		if (lastCharWasEscapeChar) 
			throw new ParseException("Term can not end with escape character.");

		return new String(output, 0, length);
	}

	/** Returns the numeric value of the hexadecimal character */
	public static final int hexToInt(char c) throws ParseException {
		if ('0' <= c && c <= '9') {
			return c - '0';
		} else if ('a' <= c && c <= 'f'){
			return c - 'a' + 10;
		} else if ('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		} else {
			throw new ParseException("Non-hex character in Unicode escape sequence: " + c);
		}
	}

	/**
	 * Returns a String where those characters that QueryParser
	 * expects to be escaped are escaped by a preceding <code>\</code>.
	 */
	public static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			// These characters are part of the query syntax and must be escaped
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
				|| c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
				|| c == '*' || c == '?' || c == '|' || c == '&' || c == '/') {
				sb.append('\\');
			}
			sb.append(c);
		}
		
		return sb.toString();
	}
	
}
