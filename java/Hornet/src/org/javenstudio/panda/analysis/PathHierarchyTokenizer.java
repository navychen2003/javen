package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.common.indexdb.util.CharTerm;

/**
 * Tokenizer for path-like hierarchies.
 * <p>
 * Take something like:
 *
 * <pre>
 *  /something/something/else
 * </pre>
 *
 * and make:
 *
 * <pre>
 *  /something
 *  /something/something
 *  /something/something/else
 * </pre>
 */
public class PathHierarchyTokenizer extends Tokenizer {

	public static final int DEFAULT_BUFFER_SIZE = 1024;
	public static final char DEFAULT_DELIMITER = '/';
	public static final int DEFAULT_SKIP = 0;

	private final CharTerm mTerm = new CharTerm();
	private final CharToken mToken = new CharToken(mTerm);
	
	private final char mDelimiter;
	private final char mReplacement;
	private final int mSkip;

	private boolean mEndDelimiter = false;
	private StringBuilder mResultToken;
	
	private int mStartPosition = 0;
	private int mSkipped = 0;
	private int mCharsRead = 0;
	
	public PathHierarchyTokenizer(Reader input) {
		this(input, DEFAULT_BUFFER_SIZE, DEFAULT_DELIMITER, DEFAULT_DELIMITER, DEFAULT_SKIP);
	}

	public PathHierarchyTokenizer(Reader input, int skip) {
		this(input, DEFAULT_BUFFER_SIZE, DEFAULT_DELIMITER, DEFAULT_DELIMITER, skip);
	}

	public PathHierarchyTokenizer(Reader input, int bufferSize, char delimiter) {
		this(input, bufferSize, delimiter, delimiter, DEFAULT_SKIP);
	}

	public PathHierarchyTokenizer(Reader input, char delimiter, char replacement) {
		this(input, DEFAULT_BUFFER_SIZE, delimiter, replacement, DEFAULT_SKIP);
	}

	public PathHierarchyTokenizer(Reader input, char delimiter, char replacement, int skip) {
		this(input, DEFAULT_BUFFER_SIZE, delimiter, replacement, skip);
	}

	public PathHierarchyTokenizer(Reader input, int bufferSize, 
			char delimiter, char replacement, int skip) {
		super(input);
		
		if (bufferSize < 0) 
			throw new IllegalArgumentException("bufferSize cannot be negative");
		if (skip < 0) 
			throw new IllegalArgumentException("skip cannot be negative");
		
		mTerm.resizeBuffer(bufferSize);

		mDelimiter = delimiter;
		mReplacement = replacement;
		mSkip = skip;
		mResultToken = new StringBuilder(bufferSize);
	}

	@Override
	public final IToken nextToken() throws IOException {
		mToken.clear();
		mToken.getTerm().append(mResultToken);
		
		if (mResultToken.length() == 0)
			mToken.setPositionIncrement(1);
		else 
			mToken.setPositionIncrement(0);
		
		int length = 0;
		boolean added = false;
		
		if (mEndDelimiter) {
			mToken.getTerm().append(mReplacement);
			length ++;
			mEndDelimiter = false;
			added = true;
		}

		while (true) {
			int c = getInput().read();
			if (c >= 0) {
				mCharsRead ++;
				
			} else {
				if (mSkipped > mSkip) {
					length += mResultToken.length();
					mToken.getTerm().setLength(length);
					mToken.setOffset(correctOffset(mStartPosition), 
							correctOffset(mStartPosition + length));
					
					if (added) {
						mResultToken.setLength(0);
						mResultToken.append(mToken.getTerm().buffer(), 0, length);
						return mToken;
					}
				}
				
				return null;
			}
			
			if (!added){
				added = true;
				mSkipped ++;
				if (mSkipped > mSkip) {
					mToken.getTerm().append(c == mDelimiter ? mReplacement : (char)c);
					length ++;
				} else 
					mStartPosition ++;
				
			} else {
				if (c == mDelimiter) {
					if (mSkipped > mSkip) {
						mEndDelimiter = true;
						break;
					}
					
					mSkipped ++;
					if (mSkipped > mSkip) {
						mToken.getTerm().append(mReplacement);
						length ++;
					} else 
						mStartPosition ++;
					
				} else {
					if (mSkipped > mSkip) {
						mToken.getTerm().append((char)c);
						length ++;
					} else 
						mStartPosition ++;
				}
			}
		}
		
		length += mResultToken.length();
		
		mToken.getTerm().setLength(length);
		mToken.setOffset(correctOffset(mStartPosition), 
				correctOffset(mStartPosition+length));
		
		mResultToken.setLength(0);
		mResultToken.append(mToken.getTerm().buffer(), 0, length);
		
		return mToken;
	}

	@Override
	public final int end() {
		// set final offset
		int finalOffset = correctOffset(mCharsRead);
		return finalOffset;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		mResultToken.setLength(0);
		mCharsRead = 0;
		mEndDelimiter = false;
		mSkipped = 0;
		mStartPosition = 0;
	}
	
}
