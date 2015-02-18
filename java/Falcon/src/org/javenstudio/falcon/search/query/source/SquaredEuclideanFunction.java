package org.javenstudio.falcon.search.query.source;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.source.MultiValueSource;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;

/**
 * While not strictly a distance, the Sq. Euclidean Distance 
 * is often all that is needed in many applications
 * that require a distance, thus saving a sq. rt. calculation
 */
public class SquaredEuclideanFunction extends VectorDistanceFunction {
	
	protected String mName = "sqedist";

	//overriding distance, so power doesn't matter here
	public SquaredEuclideanFunction(MultiValueSource source1, MultiValueSource source2) 
			throws ErrorException {
		super(-1, source1, source2); 
	}

	@Override
	protected String getName() {
		return mName;
	}

	/**
	 * @param doc The doc to score
	 */
	@Override
	protected double distance(int doc, FunctionValues dv1, FunctionValues dv2) {
		double[] vals1 = new double[mSource1.dimension()];
		double[] vals2 = new double[mSource1.dimension()];
		
		dv1.doubleVal(doc, vals1);
		dv2.doubleVal(doc, vals2);

		return DistanceUtils.distSquaredCartesian(vals1, vals2);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || !(o instanceof SquaredEuclideanFunction)) 
			return false;
		
		if (!super.equals(o)) 
			return false;

		SquaredEuclideanFunction that = (SquaredEuclideanFunction) o;
		if (!this.mName.equals(that.mName)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + mName.hashCode();
		return result;
	}
	
}
