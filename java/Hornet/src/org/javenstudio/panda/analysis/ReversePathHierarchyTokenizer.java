package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.common.indexdb.util.CharTerm;

/**
 * Tokenizer for domain-like hierarchies.
 * <p>
 * Take something like:
 *
 * <pre>
 * www.site.co.uk
 * </pre>
 *
 * and make:
 *
 * <pre>
 * www.site.co.uk
 * site.co.uk
 * co.uk
 * uk
 * </pre>
 *
 */
public class ReversePathHierarchyTokenizer extends Tokenizer {

	public static final int DEFAULT_BUFFER_SIZE = 1024;
	public static final char DEFAULT_DELIMITER = '/';
	public static final int DEFAULT_SKIP = 0;

	private final CharTerm mTerm = new CharTerm();
	private final CharToken mToken = new CharToken(mTerm);
	
	private List<Integer> mDelimiterPositions;
	private int mDelimitersCount = -1;
	private char[] mResultTokenBuffer;
	
	private StringBuilder mResultToken;
	private int mEndPosition = 0;
	private int mFinalOffset = 0;
	private int mSkipped = 0;
	
	private final char mDelimiter;
	private final char mReplacement;
	private final int mSkip;
	
	public ReversePathHierarchyTokenizer(Reader input) {
		this(input, DEFAULT_BUFFER_SIZE, DEFAULT_DELIMITER, DEFAULT_DELIMITER, DEFAULT_SKIP);
	}

	public ReversePathHierarchyTokenizer(Reader input, int skip) {
		this(input, DEFAULT_BUFFER_SIZE, DEFAULT_DELIMITER, DEFAULT_DELIMITER, skip);
	}

	public ReversePathHierarchyTokenizer(Reader input, int bufferSize, char delimiter) {
		this(input, bufferSize, delimiter, delimiter, DEFAULT_SKIP);
	}

	public ReversePathHierarchyTokenizer(Reader input, char delimiter, char replacement) {
		this(input, DEFAULT_BUFFER_SIZE, delimiter, replacement, DEFAULT_SKIP);
	}

	public ReversePathHierarchyTokenizer(Reader input, int bufferSize, char delimiter, char replacement) {
		this(input, bufferSize, delimiter, replacement, DEFAULT_SKIP);
	}

	public ReversePathHierarchyTokenizer(Reader input, char delimiter, int skip) {
		this(input, DEFAULT_BUFFER_SIZE, delimiter, delimiter, skip);
	}

	public ReversePathHierarchyTokenizer(Reader input, char delimiter, char replacement, int skip) {
		this(input, DEFAULT_BUFFER_SIZE, delimiter, replacement, skip);
	}

	public ReversePathHierarchyTokenizer(Reader input, int bufferSize, 
			char delimiter, char replacement, int skip) {
		super(input);
		if (bufferSize < 0) {
			throw new IllegalArgumentException("bufferSize cannot be negative");
		}
		if (skip < 0) {
			throw new IllegalArgumentException("skip cannot be negative");
		}
		mTerm.resizeBuffer(bufferSize);
		mDelimiter = delimiter;
		mReplacement = replacement;
		mSkip = skip;
		mResultToken = new StringBuilder(bufferSize);
		mResultTokenBuffer = new char[bufferSize];
		mDelimiterPositions = new ArrayList<Integer>(bufferSize/10);
	}

	@Override
	public final IToken nextToken() throws IOException {
		mToken.clear();
		
		if (mDelimitersCount == -1) {
			int length = 0;
			mDelimiterPositions.add(0);
			
			while (true) {
				int c = getInput().read();
				if (c < 0) 
					break;
				
				length ++;
				if (c == mDelimiter) {
					mDelimiterPositions.add(length);
					mResultToken.append(mReplacement);
				} else {
					mResultToken.append((char)c);
				}
			}
			
			mDelimitersCount = mDelimiterPositions.size();
			if (mDelimiterPositions.get(mDelimitersCount-1) < length) {
				mDelimiterPositions.add(length);
				mDelimitersCount ++;
			}
			
			if (mResultTokenBuffer.length < mResultToken.length()) 
				mResultTokenBuffer = new char[mResultToken.length()];
		
			mResultToken.getChars(0, mResultToken.length(), mResultTokenBuffer, 0);
			mResultToken.setLength(0);
			
			int idx = mDelimitersCount - 1 - mSkip;
			if (idx >= 0) {
				// otherwise its ok, because we will skip and return false
				mEndPosition = mDelimiterPositions.get(idx);
			}
			
			mFinalOffset = correctOffset(length);
			mToken.setPositionIncrement(1);
			
		} else {
			mToken.setPositionIncrement(0);
		}

		while (mSkipped < mDelimitersCount - mSkip - 1) {
			int start = mDelimiterPositions.get(mSkipped);
			
			mToken.getTerm().copyBuffer(mResultTokenBuffer, start, mEndPosition - start);
			mToken.setOffset(correctOffset(start), correctOffset(mEndPosition));
			
			mSkipped ++;
			
			return mToken;
		}

		return null;
	}

	@Override
	public final int end() {
		// set final offset
		return mFinalOffset;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		
		mResultToken.setLength(0);
		mFinalOffset = 0;
		mEndPosition = 0;
		mSkipped = 0;
		mDelimitersCount = -1;
		mDelimiterPositions.clear();
	}
	
}
