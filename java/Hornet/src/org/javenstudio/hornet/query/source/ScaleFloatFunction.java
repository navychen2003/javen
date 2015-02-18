package org.javenstudio.hornet.query.source;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/**
 * Scales values to be between min and max.
 * <p>This implementation currently traverses all of the source values to obtain
 * their min and max.
 * <p>This implementation currently cannot distinguish when documents have been
 * deleted or documents that have no value, and 0.0 values will be used for
 * these cases.  This means that if values are normally all greater than 0.0, one can
 * still end up with 0.0 as the min value to map from.  In these cases, an
 * appropriate map() function could be used as a workaround to change 0.0
 * to a value in the real range.
 */
public class ScaleFloatFunction extends ValueSource {
	
	protected final ValueSource mSource;
	protected final float mMin;
	protected final float mMax;

	public ScaleFloatFunction(ValueSource source, float min, float max) {
		mSource = source;
		mMin = min;
		mMax = max;
	}

	@Override
	public String getDescription() {
		return "scale(" + mSource.getDescription() + "," + mMin + "," + mMax + ")";
	}

	private static class ScaleInfo {
		float minVal;
		float maxVal;
	}

	private ScaleInfo createScaleInfo(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final List<IAtomicReaderRef> leaves = ReaderUtil.getTopLevel(readerContext).getLeaves();

		float minVal = Float.POSITIVE_INFINITY;
		float maxVal = Float.NEGATIVE_INFINITY;

		for (IAtomicReaderRef leaf : leaves) {
			int maxDoc = leaf.getReader().getMaxDoc();
			FunctionValues vals = mSource.getValues(context, leaf);
			
			for (int i=0; i < maxDoc; i++) {
				float val = vals.floatVal(i);
				
				if ((Float.floatToRawIntBits(val) & (0xff<<23)) == 0xff<<23) {
					// if the exponent in the float is all ones, then this is +Inf, -Inf or NaN
					// which don't make sense to factor into the scale function
					continue;
				}
				
				if (val < minVal) 
					minVal = val;
      
				if (val > maxVal) 
					maxVal = val;
			}
		}

		if (minVal == Float.POSITIVE_INFINITY) {
			// must have been an empty index
			minVal = maxVal = 0;
		}

		ScaleInfo scaleInfo = new ScaleInfo();
		scaleInfo.minVal = minVal;
		scaleInfo.maxVal = maxVal;
		
		context.put(mSource, scaleInfo);
		
		return scaleInfo;
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {

		ScaleInfo scaleInfo = (ScaleInfo)context.get(mSource);
		if (scaleInfo == null) 
			scaleInfo = createScaleInfo(context, readerContext);
    
		final float scale = (scaleInfo.maxVal - scaleInfo.minVal == 0) ? 0 : 
			(mMax - mMin) / (scaleInfo.maxVal - scaleInfo.minVal);
		
		final float minSource = scaleInfo.minVal;
		final float maxSource = scaleInfo.maxVal;

		final FunctionValues vals =  mSource.getValues(context, readerContext);

		return new FloatDocValues(this) {
			@Override
			public float floatVal(int doc) {
				return (vals.floatVal(doc) - minSource) * scale + mMin;
			}
			
			@Override
			public String toString(int doc) {
				return "scale(" + vals.toString(doc) + ",toMin=" + mMin + ",toMax=" + mMax
						+ ",fromMin=" + minSource + ",fromMax=" + maxSource
						+ ")";
			}
		};
	}

	@Override
	public void createWeight(ValueSourceContext context, 
			ISearcher searcher) throws IOException {
		mSource.createWeight(context, searcher);
	}

	@Override
	public int hashCode() {
		int h = Float.floatToIntBits(mMin);
		h = h*29;
		h += Float.floatToIntBits(mMax);
		h = h*29;
		h += mSource.hashCode();
		return h;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || ScaleFloatFunction.class != o.getClass()) 
			return false;
		
		ScaleFloatFunction other = (ScaleFloatFunction)o;
		return this.mMin == other.mMin
				&& this.mMax == other.mMax
				&& this.mSource.equals(other.mSource);
	}
	
}
