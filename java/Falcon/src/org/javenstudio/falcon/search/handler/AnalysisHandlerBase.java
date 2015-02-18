package org.javenstudio.falcon.search.handler;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharTerm;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.analysis.ListBasedTokenStream;
import org.javenstudio.falcon.search.analysis.TokenizerChain;
import org.javenstudio.falcon.search.schema.SchemaFieldType;
import org.javenstudio.panda.analysis.CharFilterFactory;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.analysis.TokenizerFactory;

/**
 * A base class for all analysis request handlers.
 *
 */
public abstract class AnalysisHandlerBase {

	public void handleRequestBody(ISearchRequest req, ISearchResponse rsp) 
			throws ErrorException {
		rsp.add("analysis", doAnalysis(req));
	}

	/**
	 * Performs the analysis based on the given request and returns 
	 * the analysis result as a named list.
	 *
	 * @param req The request.
	 * @return The analysis result as a named list.
	 * @throws Exception When analysis fails.
	 */
	protected abstract NamedList<?> doAnalysis(ISearchRequest req) throws ErrorException;
	
	/**
	 * Analyzes the given value using the given Analyzer.
	 *
	 * @param value   Value to analyze
	 * @param context The {@link AnalysisContext analysis context}.
	 * @return NamedList containing the tokens produced by analyzing the given value
	 */
	@SuppressWarnings("resource")
	protected NamedList<? extends Object> analyzeValue(String value, 
			AnalysisContext context) throws IOException, ErrorException {
		IAnalyzer analyzer = context.getAnalyzer();

		if (!TokenizerChain.class.isInstance(analyzer)) {
			ITokenStream tokenStream = analyzer.tokenStream(context.getFieldName(), 
					new StringReader(value));
			
			NamedList<List<NamedList<?>>> namedList = new NamedList<List<NamedList<?>>>();
			namedList.add(tokenStream.getClass().getName(), 
					convertTokensToNamedLists(analyzeTokenStream(tokenStream), context));
			
			return namedList;
		}

		TokenizerChain tokenizerChain = (TokenizerChain) analyzer;
		CharFilterFactory[] cfiltfacs = tokenizerChain.getCharFilterFactories();
		TokenizerFactory tfac = tokenizerChain.getTokenizerFactory();
		TokenFilterFactory[] filtfacs = tokenizerChain.getTokenFilterFactories();

		NamedList<Object> namedList = new NamedList<Object>();
		if (cfiltfacs != null) {
			String source = value;
			for (CharFilterFactory cfiltfac : cfiltfacs ) {
				Reader reader = new StringReader(source);
				reader = cfiltfac.create(reader);
				source = writeCharStream(namedList, reader);
			}
		}

		ITokenStream tokenStream = tfac.create(tokenizerChain.initReader(null, 
				new StringReader(value)));
		List<IToken> tokens = analyzeTokenStream(tokenStream);

		namedList.add(tokenStream.getClass().getName(), 
				convertTokensToNamedLists(tokens, context));

		ListBasedTokenStream listBasedTokenStream = new ListBasedTokenStream(tokens);

		for (TokenFilterFactory tokenFilterFactory : filtfacs) {
			tokenStream = tokenFilterFactory.create(listBasedTokenStream);
			tokens = analyzeTokenStream(tokenStream);
			
			namedList.add(tokenStream.getClass().getName(), 
					convertTokensToNamedLists(tokens, context));
			
			listBasedTokenStream = new ListBasedTokenStream(tokens);
		}

		return namedList;
	}
	
	/**
	 * Analyzes the given TokenStream, collecting the Tokens it produces.
	 *
	 * @param tokenStream TokenStream to analyze
	 *
	 * @return List of tokens produced from the TokenStream
	 */
	protected List<IToken> analyzeTokenStream(ITokenStream tokenStream) 
			throws IOException, ErrorException {
		final List<IToken> tokens = new ArrayList<IToken>();

		tokenStream.reset();
		IToken token = null;
		while ((token = tokenStream.nextToken()) != null) {
			IToken cloned = (IToken)token.clone();
			tokens.add(cloned); // must clone
		}

		return tokens;
	}
	
	/**
	 * Converts the list of Tokens to a list of NamedLists representing the tokens.
	 *
	 * @param tokenList  Tokens to convert
	 * @param context The analysis context
	 *
	 * @return List of NamedLists containing the relevant information taken from the tokens
	 */
	protected List<NamedList<?>> convertTokensToNamedLists(final List<IToken> tokenList, 
			AnalysisContext context) throws IOException, ErrorException {
		final List<NamedList<?>> tokensNamedLists = new ArrayList<NamedList<?>>();
		final SchemaFieldType fieldType = context.getFieldType();
		//final IToken[] tokens = tokenList.toArray(new IToken[tokenList.size()]);
    
		//// sort the tokens by absoulte position
		//ArrayUtil.mergeSort(tokens, new Comparator<IToken>() {
		//		@Override
		//		public int compare(IToken a, IToken b) {
		//			return arrayCompare(new int[]{0}, new int[]{0});
		//		}
	    //  
		//		private int arrayCompare(int[] a, int[] b) {
		//			int p = 0;
		//			final int stop = Math.min(a.length, b.length);
		//			while (p < stop) {
		//				int diff = a[p] - b[p];
		//				if (diff != 0) return diff;
		//				p++;
		//			}
		//			// One is a prefix of the other, or, they are equal:
		//			return a.length - b.length;
		//		}
		//	});

		int position = 0;
		
		for (IToken token : tokenList) {
			final NamedList<Object> tokenNamedList = new NamedMap<Object>();
			
			token.fillBytesRef();
			BytesRef rawBytes = token.getBytesRef();
			
			String text = fieldType.indexedToReadable(rawBytes, 
					new CharsRef(rawBytes.getLength())).toString();
			
			tokenNamedList.add("text", text);
      
			if (token instanceof CharToken) {
				CharTerm term = ((CharToken) token).getTerm();
				final String rawText = term.toString();
				if (!rawText.equals(text)) 
					tokenNamedList.add("raw_text", rawText);
			}

			tokenNamedList.add("raw_bytes", rawBytes.toString());

			if (context.getTermsToMatch().contains(rawBytes)) 
				tokenNamedList.add("match", true);
			
			position += token.getPositionIncrement();
			
			tokenNamedList.add("start", token.getStartOffset());
			tokenNamedList.add("end", token.getEndOffset());
			tokenNamedList.add("type", token.getType());
			tokenNamedList.add("position", position);
			
			tokensNamedLists.add(tokenNamedList);
		}

		return tokensNamedLists;
	}
	
	/**
	 * Analyzes the given text using the given analyzer and returns the produced tokens.
	 *
	 * @param query    The query to analyze.
	 * @param analyzer The analyzer to use.
	 */
	protected Set<BytesRef> getQueryTokenSet(String query, IAnalyzer analyzer) throws IOException {
		final Set<BytesRef> tokens = new HashSet<BytesRef>();
		final ITokenStream tokenStream = analyzer.tokenStream("", new StringReader(query));

		tokenStream.reset();

		IToken token = null;
		while ((token = tokenStream.nextToken()) != null) {
			tokens.add(BytesRef.deepCopyOf(token.getBytesRef()));
		}

		tokenStream.end();
		tokenStream.close();
		
		return tokens;
	}
	
	static final int BUFFER_SIZE = 1024;
	
  	protected String writeCharStream(NamedList<Object> out, 
  			Reader input) throws ErrorException {
  		final char[] buf = new char[BUFFER_SIZE];
  		int len = 0;
  		
  		StringBuilder sb = new StringBuilder();
  		do {
  			try {
  				len = input.read(buf, 0, BUFFER_SIZE);
  			} catch (IOException e) {
  				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, e);
  			}
  			
  			if (len > 0)
  				sb.append(buf, 0, len);
  			
  		} while (len == BUFFER_SIZE);
  		
  		out.add(input.getClass().getName(), sb.toString());
  		
  		return sb.toString();
  	}
	
}
