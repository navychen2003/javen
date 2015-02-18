package org.javenstudio.common.indexdb.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.util.NumericTerm;
import org.javenstudio.common.indexdb.util.NumericUtil;

/**
 * Numeric TokenStream implements for int/long/float/double.
 */
public final class NumericTokenStream implements ITokenStream {

	/** The full precision token gets this token type assigned. */
	public static final String TOKEN_TYPE_FULL_PREC  = "fullPrecNumeric";

	/** The lower precision tokens gets this token type assigned. */
	public static final String TOKEN_TYPE_LOWER_PREC = "lowerPrecNumeric";

	private final NumericTerm mTerm = new NumericTerm();
	private final NumericToken mToken = new NumericToken(mTerm);

	private final int mPrecisionStep;
	private int mValSize = 0; // valSize==0 means not initialized
	
	/**
	 * Creates a token stream for numeric values using the default <code>precisionStep</code>
	 * {@link NumericUtil#PRECISION_STEP_DEFAULT} (4). The stream is not yet initialized,
	 * before using set a value using the various set<em>???</em>Value() methods.
	 */
	public NumericTokenStream() {
		this(NumericUtil.PRECISION_STEP_DEFAULT);
	}
  
	/**
	 * Creates a token stream for numeric values with the specified
	 * <code>precisionStep</code>. The stream is not yet initialized,
	 * before using set a value using the various set<em>???</em>Value() methods.
	 */
	public NumericTokenStream(final int precisionStep) {
		super();
		mPrecisionStep = precisionStep;
		if (precisionStep < 1)
			throw new IllegalArgumentException("precisionStep must be >=1");
	}

	/**
	 * Initializes the token stream with the supplied <code>long</code> value.
	 * @param value the value, for which this TokenStream should enumerate tokens.
	 * @return this instance, because of this you can use it the following way:
	 * <code>new Field(name, new NumericTokenStream(precisionStep).setLongValue(value))</code>
	 */
	public NumericTokenStream setLongValue(final long value) {
		mTerm.init(value, mValSize = 64, mPrecisionStep, -mPrecisionStep);
		return this;
	}
  
	/**
	 * Initializes the token stream with the supplied <code>int</code> value.
	 * @param value the value, for which this TokenStream should enumerate tokens.
	 * @return this instance, because of this you can use it the following way:
	 * <code>new Field(name, new NumericTokenStream(precisionStep).setIntValue(value))</code>
	 */
	public NumericTokenStream setIntValue(final int value) {
		mTerm.init(value, mValSize = 32, mPrecisionStep, -mPrecisionStep);
		return this;
	}
  
	/**
	 * Initializes the token stream with the supplied <code>double</code> value.
	 * @param value the value, for which this TokenStream should enumerate tokens.
	 * @return this instance, because of this you can use it the following way:
	 * <code>new Field(name, new NumericTokenStream(precisionStep).setDoubleValue(value))</code>
	 */
	public NumericTokenStream setDoubleValue(final double value) {
		mTerm.init(NumericUtil.doubleToSortableLong(value), mValSize = 64, mPrecisionStep, -mPrecisionStep);
		return this;
	}
  
	/**
	 * Initializes the token stream with the supplied <code>float</code> value.
	 * @param value the value, for which this TokenStream should enumerate tokens.
	 * @return this instance, because of this you can use it the following way:
	 * <code>new Field(name, new NumericTokenStream(precisionStep).setFloatValue(value))</code>
	 */
	public NumericTokenStream setFloatValue(final float value) {
		mTerm.init(NumericUtil.floatToSortableInt(value), mValSize = 32, mPrecisionStep, -mPrecisionStep);
		return this;
	}
  
	@Override
	public int end() throws IOException {
		return 0;
	}
  
	@Override
	public void reset() {
		if (mValSize == 0)
			throw new IllegalStateException("call set???Value() before usage");
		
		mTerm.setShift(-mPrecisionStep);
	}

	@Override
	public void close() {}
  
	@Override
	public IToken nextToken() {
		mToken.clear();
		
		if (mValSize == 0)
			throw new IllegalStateException("call set???Value() before usage");
		
		final int shift = mTerm.increaseShift();
		if (shift >= mValSize)
			return null;

		final String type = (shift == 0) ? TOKEN_TYPE_FULL_PREC : TOKEN_TYPE_LOWER_PREC;
		final int positionIncrement = (shift == 0) ? 1 : 0;
		
		mToken.setType(type);
		mToken.setPositionIncrement(positionIncrement);
		
		return mToken;
	}
  
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(); 
		sb.append("(numeric,valSize=").append(mValSize);
		sb.append(",precisionStep=").append(mPrecisionStep).append(')');
		return sb.toString();
	}

	/** Returns the precision step. */
	public int getPrecisionStep() {
		return mPrecisionStep;
	}
	
}
