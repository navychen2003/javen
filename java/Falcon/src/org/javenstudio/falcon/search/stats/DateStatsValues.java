package org.javenstudio.falcon.search.stats;

import java.util.Date;

import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * Implementation of StatsValues that supports Date values
 */
public class DateStatsValues extends AbstractStatsValues<Date> {

	protected long mSum = -1;
	protected double mSumOfSquares = 0;

	public DateStatsValues(SchemaField sf) {
		super(sf);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateTypeSpecificStats(NamedList<?> stv) {
		mSum += ((Date) stv.get("sum")).getTime();
		mSumOfSquares += ((Number)stv.get("sumOfSquares")).doubleValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateTypeSpecificStats(Date v) {
		long value = v.getTime();
		mSumOfSquares += (value * value); // for std deviation
		mSum += value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateTypeSpecificStats(Date v, int count) {
		long value = v.getTime();
		mSumOfSquares += (value * value * count); // for std deviation
		mSum += value * count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateMinMax(Date min, Date max) {
		if (mMin == null || mMin.after(min)) 
			mMin = min;
		
		if (mMax == null || mMax.before(min)) 
			mMax = max;
	}

	/**
	 * Adds sum and mean statistics to the given NamedList
	 *
	 * @param res NamedList to add the type specific statistics too
	 */
	@Override
	protected void addTypeSpecificStats(NamedList<Object> res) {
		if (mSum <= 0) 
			return; // date==0 is meaningless
		
		res.add("sum", new Date(mSum));
		if (mCount > 0) 
			res.add("mean", new Date(mSum / mCount));
		
		res.add("sumOfSquares", mSumOfSquares);
		res.add("stddev", getStandardDeviation());
	}
  
	/**
	 * Calculates the standard deviation.  For dates, this is really the MS deviation
	 *
	 * @return Standard deviation statistic
	 */
	private double getStandardDeviation() {
		if (mCount <= 1) 
			return 0.0D;
		
		return Math.sqrt(((mCount * mSumOfSquares) - (mSum * mSum)) / (mCount * (mCount - 1.0D)));
	}
	
}
