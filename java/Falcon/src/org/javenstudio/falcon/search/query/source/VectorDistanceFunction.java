package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.DoubleDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.source.MultiValueSource;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;

/**
 * Calculate the p-norm for a Vector.  See http://en.wikipedia.org/wiki/Lp_space
 * <p/>
 * Common cases:
 * <ul>
 * <li>0 = Sparseness calculation</li>
 * <li>1 = Manhattan distance</li>
 * <li>2 = Euclidean distance</li>
 * <li>Integer.MAX_VALUE = infinite norm</li>
 * </ul>
 *
 * @see SquaredEuclideanFunction for the special case
 */
public class VectorDistanceFunction extends ValueSource {
	
	protected MultiValueSource mSource1, mSource2;
	protected float mPower;
	protected float mOneOverPower;

	public VectorDistanceFunction(float power, 
			MultiValueSource source1, MultiValueSource source2) throws ErrorException {
		if ((source1.dimension() != source2.dimension())) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Illegal number of sources");
		}
		
		mPower = power;
		mOneOverPower = 1 / power;
		mSource1 = source1;
		mSource2 = source2;
	}

	protected String getName() {
		return "dist";
	}

	/**
	 * Calculate the distance
	 *
	 * @param doc The current doc
	 * @param dv1 The values from the first MultiValueSource
	 * @param dv2 The values from the second MultiValueSource
	 * @return The distance
	 */
	protected double distance(int doc, FunctionValues dv1, FunctionValues dv2) {
		//Handle some special cases:
		double[] vals1 = new double[mSource1.dimension()];
		double[] vals2 = new double[mSource2.dimension()];
		
		dv1.doubleVal(doc, vals1);
		dv2.doubleVal(doc, vals2);
		
		return DistanceUtils.vectorDistance(vals1, vals2, mPower, mOneOverPower);
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues vals1 = mSource1.getValues(context, readerContext);
		final FunctionValues vals2 = mSource2.getValues(context, readerContext);

		return new DoubleDocValues(this) {
			@Override
			public double doubleVal(int doc) {
				return distance(doc, vals1, vals2);
			}

			@Override
			public String toString(int doc) {
				StringBuilder sb = new StringBuilder();
				sb.append(getName()).append('(').append(mPower).append(',');
				sb.append(vals1.toString(doc)).append(',');
				sb.append(vals2.toString(doc));
				sb.append(')');
				return sb.toString();
			}
		};
	}

	@Override
	public void createWeight(ValueSourceContext context, ISearcher searcher) 
			throws IOException {
		mSource1.createWeight(context, searcher);
		mSource2.createWeight(context, searcher);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || !(o instanceof VectorDistanceFunction)) 
			return false;

		VectorDistanceFunction that = (VectorDistanceFunction) o;

		if (Float.compare(that.mPower, this.mPower) != 0) return false;
		if (!this.mSource1.equals(that.mSource1)) return false;
		if (!this.mSource2.equals(that.mSource2)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = mSource1.hashCode();
		result = 31 * result + mSource2.hashCode();
		result = 31 * result + Float.floatToRawIntBits(mPower);
		return result;
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append('(').append(mPower).append(',');
		sb.append(mSource1).append(',');
		sb.append(mSource2);
		sb.append(')');
		return sb.toString();
	}

}
