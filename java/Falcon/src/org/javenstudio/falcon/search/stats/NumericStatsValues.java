package org.javenstudio.falcon.search.stats;

import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * Implementation of StatsValues that supports Double values
 */
public class NumericStatsValues extends AbstractStatsValues<Number> {
	
	protected double mSum;
	protected double mSumOfSquares;

	public NumericStatsValues(SchemaField sf) {
		super(sf);
		mMin = Double.POSITIVE_INFINITY;
		mMax = Double.NEGATIVE_INFINITY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateTypeSpecificStats(NamedList<?> stv) {
		mSum += ((Number)stv.get("sum")).doubleValue();
		mSumOfSquares += ((Number)stv.get("sumOfSquares")).doubleValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateTypeSpecificStats(Number v) {
		double value = v.doubleValue();
		mSumOfSquares += (value * value); // for std deviation
		mSum += value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateTypeSpecificStats(Number v, int count) {
		double value = v.doubleValue();
		mSumOfSquares += (value * value * count); // for std deviation
		mSum += value * count;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void updateMinMax(Number min, Number max) {
		mMin = Math.min(mMin.doubleValue(), min.doubleValue());
		mMax = Math.max(mMax.doubleValue(), max.doubleValue());
	}

	/**
	 * Adds sum, sumOfSquares, mean and standard deviation statistics to the given NamedList
	 *
	 * @param res NamedList to add the type specific statistics too
	 */
	@Override
	protected void addTypeSpecificStats(NamedList<Object> res) {
		res.add("sum", mSum);
		res.add("sumOfSquares", mSumOfSquares);
		res.add("mean", mSum / mCount);
		res.add("stddev", getStandardDeviation());
	}

	/**
	 * Calculates the standard deviation statistic
	 *
	 * @return Standard deviation statistic
	 */
	private double getStandardDeviation() {
		if (mCount <= 1.0D) 
			return 0.0D;

		return Math.sqrt(((mCount * mSumOfSquares) - (mSum * mSum)) / (mCount * (mCount - 1.0D)));
	}
	
}
