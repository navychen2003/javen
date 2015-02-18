package org.javenstudio.falcon.search.analysis;

import java.io.Reader;

import org.javenstudio.falcon.search.schema.TrieTypes;
import org.javenstudio.panda.analysis.TokenizerFactory;

/**
 * Tokenizer for trie fields. It uses NumericTokenStream to create 
 * multiple trie encoded string per number.
 * Each string created by this tokenizer for a given number differs 
 * from the previous by the given precisionStep.
 * For query time token streams that only contain the highest 
 * precision term, use 32/64 as precisionStep.
 * <p/>
 * Refer to {@link NumericRangeQuery} for more details.
 *
 * @see NumericRangeQuery
 * @see TrieField
 * @since 1.4
 */
public class TrieTokenizerFactory extends TokenizerFactory {
	
	protected final int mPrecisionStep;
	protected final TrieTypes mType;

	public TrieTokenizerFactory(TrieTypes type, int precisionStep) {
		mType = type;
		mPrecisionStep = precisionStep;
	}

	@Override
	public TrieTokenizer create(Reader input) {
		return new TrieTokenizer(input, mType, mPrecisionStep, 
				TrieTokenizer.getNumericTokenStream(mPrecisionStep));
	}
	
}
