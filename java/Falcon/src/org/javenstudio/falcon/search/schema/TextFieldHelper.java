package org.javenstudio.falcon.search.schema;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CachingTokenFilter;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.hornet.search.query.MultiPhraseQuery;
import org.javenstudio.hornet.search.query.PhraseQuery;
import org.javenstudio.hornet.search.query.TermQuery;

public class TextFieldHelper {

	public static BytesRef analyzeMultiTerm(String field, String part, 
			IAnalyzer analyzerIn) throws ErrorException {
		if (part == null) return null;

		ITokenStream source;
		try {
			source = analyzerIn.tokenStream(field, new StringReader(part));
			source.reset();
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unable to initialize TokenStream to analyze multiTerm term: " + part 
					+ " of field: " + field, e);
		}

		IToken token = null;
		BytesRef bytes = new BytesRef();
		
		try {
			if ((token = source.nextToken()) == null) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"analyzer returned no terms for multiTerm term: " + part 
						+ " of field: " + field);
			}
			
			token.fillBytesRef(bytes);
			
			if ((token = source.nextToken()) != null) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"analyzer returned too many terms for multiTerm term: " + part 
						+ " of field: " + field);
			}
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"error analyzing range part: " + part + " of field: " + field, e);
		}

		try {
			source.end();
			source.close();
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unable to end & close TokenStream after analyzing multiTerm term: " + part 
					+ " of field: " + field, e);
		}

		return bytes;
	}
	
	public static IQuery parseFieldQuery(QueryBuilder parser, IAnalyzer analyzer, 
			String field, String queryText) throws ErrorException {
	    int phraseSlop = 0;

	    // most of the following code is taken from the Lucene QueryParser

	    // Use the analyzer to get all the tokens, and then build a TermQuery,
	    // PhraseQuery, or nothing based on the term count

	    ITokenStream source;
	    try {
	    	source = analyzer.tokenStream(field, new StringReader(queryText));
	    	source.reset();
	    } catch (IOException e) {
	    	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
	    			"Unable to initialize TokenStream to analyze query text: " + queryText 
	    			+ " of field: " + field, e);
	    }
	    
		@SuppressWarnings("resource")
		CachingTokenFilter buffer = new CachingTokenFilter(source);
	    buffer.reset();

	    boolean severalTokensAtSamePosition = false;
	    IToken token = null;
	    int numTokens = 0;
	    int positionCount = 0;

	    try {
	    	while ((token = buffer.nextToken()) != null) {
	    		numTokens ++;
	    		
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
	    	// ignore
	    }

	    if (numTokens == 0) {
	    	return null;
	    	
	    } else if (numTokens == 1) {
	    	String term = null;
	    	try {
	    		token = buffer.nextToken();
	    		assert token != null;
	    		
	    		if (token instanceof CharToken)
	    			term = ((CharToken)token).getTerm().toString();
	    		
	    	} catch (IOException e) {
	    		// safe to ignore, because we know the number of tokens
	    	}
	    	
	    	// return newTermQuery(new Term(field, term));
	    	return new TermQuery(new Term(field, term));
	    	
	    } else {
	    	if (severalTokensAtSamePosition) {
	    		if (positionCount == 1) {
	    			// no phrase query:
	    			// BooleanQuery q = newBooleanQuery(true);
	    			BooleanQuery q = new BooleanQuery(true);
	    			
	    			for (int i = 0; i < numTokens; i++) {
	    				String term = null;
	    				try {
	    					token = buffer.nextToken();
	    					assert token != null;
	    					
	    					if (token instanceof CharToken)
	    		    			term = ((CharToken)token).getTerm().toString();
	    					
	    				} catch (IOException e) {
	    					// safe to ignore, because we know the number of tokens
	    				}

	    				// Query currentQuery = newTermQuery(new Term(field, term));
	    				IQuery currentQuery = new TermQuery(new Term(field, term));
	    				q.add(currentQuery, IBooleanClause.Occur.SHOULD);
	    			}
	    			
	    			return q;
	    		} else {
	    			// phrase query:
	    			// MultiPhraseQuery mpq = newMultiPhraseQuery();
	    			MultiPhraseQuery mpq = new MultiPhraseQuery();
	    			mpq.setSlop(phraseSlop);
	    			
	    			List<ITerm> multiTerms = new ArrayList<ITerm>();
	    			int position = -1;
	    			
	    			for (int i = 0; i < numTokens; i++) {
	    				String term = null;
	    				int positionIncrement = 1;
	    				
	    				try {
	    					token = buffer.nextToken();
	    					assert token != null;
	    					
	    					if (token instanceof CharToken)
	    		    			term = ((CharToken)token).getTerm().toString();
	    					
	    					positionIncrement = token.getPositionIncrement();
              
	    				} catch (IOException e) {
	    					// safe to ignore, because we know the number of tokens
	    				}

	    				if (positionIncrement > 0 && multiTerms.size() > 0) {
	    					mpq.add((Term[])multiTerms.toArray(new Term[multiTerms.size()]),position);
	    					multiTerms.clear();
	    				}
	    				
	    				position += positionIncrement;
	    				multiTerms.add(new Term(field, term));
	    			}
	    			
	    			mpq.add((Term[])multiTerms.toArray(new Term[multiTerms.size()]),position);
	    			return mpq;
	    		}
	    		
	    	} else {
	    		// PhraseQuery pq = newPhraseQuery();
	    		PhraseQuery pq = new PhraseQuery();
	    		pq.setSlop(phraseSlop);
	    		
	    		int position = -1;

	    		for (int i = 0; i < numTokens; i++) {
	    			String term = null;
	    			int positionIncrement = 1;

	    			try {
	    				token = buffer.nextToken();
	    				assert token != null;
	    				
	    				if (token instanceof CharToken)
	    					term = ((CharToken)token).getTerm().toString();
            
	    				positionIncrement = token.getPositionIncrement();
            
	    			} catch (IOException e) {
	    				// safe to ignore, because we know the number of tokens
	    			}

	    			position += positionIncrement;
	    			pq.add(new Term(field, term),position);
	    		}
	    		
	    		return pq;
	    	}
	    }

	}
	
}
