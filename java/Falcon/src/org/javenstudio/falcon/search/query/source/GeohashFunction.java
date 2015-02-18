package org.javenstudio.falcon.search.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.falcon.search.schema.spatial.GeohashUtils;

/**
 * Takes in a latitude and longitude ValueSource and produces a GeoHash.
 * <p/>
 * Ex: geohash(lat, lon)
 *
 * <p/>
 * Note, there is no reciprocal function for this.
 **/
public class GeohashFunction extends ValueSource {
	
	protected ValueSource mLatSource, mLonSource;

	public GeohashFunction(ValueSource lat, ValueSource lon) {
		mLatSource = lat;
		mLonSource = lon;
	}

	protected String getName() {
		return "geohash";
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final FunctionValues latDV = mLatSource.getValues(context, readerContext);
		final FunctionValues lonDV = mLonSource.getValues(context, readerContext);

    return new FunctionValues() {
    	@Override
    	public String stringVal(int doc) {
    		return GeohashUtils.encodeLatLon(latDV.doubleVal(doc), lonDV.doubleVal(doc));
    	}

    	@Override
    	public String toString(int doc) {
    		StringBuilder sb = new StringBuilder();
    		sb.append(getName()).append('(');
    		sb.append(latDV.toString(doc)).append(',').append(lonDV.toString(doc));
    		sb.append(')');
    		return sb.toString();
    	}
    	};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || !(o instanceof GeohashFunction)) 
			return false;

		GeohashFunction that = (GeohashFunction) o;

		if (!mLatSource.equals(that.mLatSource)) return false;
		if (!mLonSource.equals(that.mLonSource)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = mLatSource.hashCode();
		result = 29 * result - mLonSource.hashCode();
		return result;
	}

	@Override  
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append('(');
		sb.append(mLatSource).append(',').append(mLonSource);
		sb.append(')');
		return sb.toString();
	}
	
}
