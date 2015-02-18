package org.javenstudio.falcon.search.schema;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.falcon.search.filter.PostFilter;
import org.javenstudio.falcon.search.hits.DelegatingCollector;
import org.javenstudio.falcon.search.query.ExtendedQueryBase;
import org.javenstudio.falcon.search.schema.spatial.DistanceUtils;

//TODO: recast as a value source that doesn't have to match all docs
public class SpatialDistanceQuery extends ExtendedQueryBase implements PostFilter {
	
	protected String mOrigField;
	protected ValueSource mLatSource;
	protected ValueSource mLonSource;
	protected double mLonMin, mLonMax;
	protected double mLon2Min, mLon2Max; 
	protected double mLatMin, mLatMax;
	protected boolean mLon2;

	protected boolean mCalcDist;  // actually calculate the distance with haversine
	protected IQuery mBBoxQuery;

	protected double mLatCenter;
	protected double mLonCenter;
	protected double mDist;
	protected double mPlanetRadius;

	@Override
	public IQuery rewrite(IIndexReader reader) throws IOException {
		return mBBoxQuery != null ? mBBoxQuery.rewrite(reader) : this;
	}

	@Override
	public void extractTerms(Set<ITerm> terms) { 
		// do nothing
	}

	protected class SpatialWeight extends Weight {
		protected ISearcher mSearcher;
		protected float mQueryNorm;
		protected float mQueryWeight;
		protected ValueSourceContext mLatContext;
		protected ValueSourceContext mLonContext;

		public SpatialWeight(ISearcher searcher) throws IOException {
			mSearcher = searcher;
			mLatContext = ValueSourceContext.create(searcher);
			mLonContext = ValueSourceContext.create(searcher);
			mLatSource.createWeight(mLatContext, searcher);
			mLonSource.createWeight(mLonContext, searcher);
		}

		@Override
		public IQuery getQuery() {
			return SpatialDistanceQuery.this;
		}

		@Override
		public float getValueForNormalization() throws IOException {
			mQueryWeight = getBoost();
			return mQueryWeight * mQueryWeight;
		}

		@Override
		public void normalize(float norm, float topLevelBoost) {
			mQueryNorm = norm * topLevelBoost;
			mQueryWeight *= mQueryNorm;
		}

		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, Bits acceptDocs) throws IOException {
			return new SpatialScorer(context, acceptDocs, this, mQueryWeight);
		}

		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc) throws IOException {
			return ((SpatialScorer)getScorer(context, true, true, 
					context.getReader().getLiveDocs())).explain(doc);
		}
	}

	protected class SpatialScorer extends Scorer {
		private final IIndexReader mReader;
		private final SpatialWeight mWeight;
		private final int mMaxDoc;
		private final float mQWeight;
		private final FunctionValues mLatVals;
		private final FunctionValues mLonVals;
		private final Bits mAcceptDocs;
		
		private final double mLonMin, mLonMax; 
		private final double mLon2Min, mLon2Max;
		private final double mLatMin, mLatMax;
		private final boolean mLon2;
		private final boolean mCalcDist;
    
		private final double mLatCenterRad;
		private final double mLonCenterRad;
		private final double mLatCenterRad_cos;
		private final double mDist;
		private final double mPlanetRadius;

		private int mDoc = -1;
		private int mLastDistDoc;
		private double mLastDist;

		public SpatialScorer(IAtomicReaderRef readerContext, Bits acceptDocs, 
				SpatialWeight w, float qWeight) throws IOException {
			super(w);
			
			mWeight = w;
			mQWeight = qWeight;
			mReader = readerContext.getReader();
			mMaxDoc = mReader.getMaxDoc();
			mAcceptDocs = acceptDocs;
			mLatVals = mLatSource.getValues(mWeight.mLatContext, readerContext);
			mLonVals = mLonSource.getValues(mWeight.mLonContext, readerContext);

			mLonMin = SpatialDistanceQuery.this.mLonMin;
			mLonMax = SpatialDistanceQuery.this.mLonMax;
			mLon2Min = SpatialDistanceQuery.this.mLon2Min;
			mLon2Max = SpatialDistanceQuery.this.mLon2Max;
			mLatMin = SpatialDistanceQuery.this.mLatMin;
			mLatMax = SpatialDistanceQuery.this.mLatMax;
			mLon2 = SpatialDistanceQuery.this.mLon2;
			mCalcDist = SpatialDistanceQuery.this.mCalcDist;

			mLatCenterRad = SpatialDistanceQuery.this.mLatCenter * DistanceUtils.DEGREES_TO_RADIANS;
			mLonCenterRad = SpatialDistanceQuery.this.mLonCenter * DistanceUtils.DEGREES_TO_RADIANS;
			mLatCenterRad_cos = mCalcDist ? Math.cos(mLatCenterRad) : 0;
			mDist = SpatialDistanceQuery.this.mDist;
			mPlanetRadius = SpatialDistanceQuery.this.mPlanetRadius;
		}

		private boolean match() {
			// longitude should generally be more restrictive than latitude
			// (e.g. in the US, it immediately separates the coasts, and in world search separates
			// US from Europe from Asia, etc.
			double lon = mLonVals.doubleVal(mDoc);
			if (!((lon >= mLonMin && lon <= mLonMax) || (mLon2 && lon >= mLon2Min && lon <= mLon2Max))) 
				return false;

			double lat = mLatVals.doubleVal(mDoc);
			if (!(lat >= mLatMin && lat <= mLatMax)) 
				return false;

			if (!mCalcDist) 
				return true;

			// TODO: test for internal box where we wouldn't need to calculate the distance

			return dist(lat, lon) <= mDist;
		}

		private double dist(double lat, double lon) {
			double latRad = lat * DistanceUtils.DEGREES_TO_RADIANS;
			double lonRad = lon * DistanceUtils.DEGREES_TO_RADIANS;
      
			// haversine, specialized to avoid a cos() call on latCenterRad
			double diffX = mLatCenterRad - latRad;
			double diffY = mLonCenterRad - lonRad;
			double hsinX = Math.sin(diffX * 0.5);
			double hsinY = Math.sin(diffY * 0.5);
			double h = hsinX * hsinX + (mLatCenterRad_cos * Math.cos(latRad) * hsinY * hsinY);
			double result = (mPlanetRadius * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h)));

			// save the results of this calculation
			mLastDistDoc = mDoc;
			mLastDist = result;
      
			return result;
		}

		@Override
		public int getDocID() { return mDoc; }

		// instead of matching all docs, we could also embed a query.
		// the score could either ignore the subscore, or boost it.
		// Containment:  floatline(foo:myTerm, "myFloatField", 1.0, 0.0f)
		// Boost:        foo:myTerm^floatline("myFloatField",1.0,0.0f)
		@Override
		public int nextDoc() throws IOException {
			for (;;) {
				++ mDoc;
				
				if (mDoc >= mMaxDoc) 
					return mDoc = NO_MORE_DOCS;
        
				if (mAcceptDocs != null && !mAcceptDocs.get(mDoc)) 
					continue;
				
				if (!match()) 
					continue;
				
				return mDoc;
			}
		}

		@Override
		public int advance(int target) throws IOException {
			// this will work even if target==NO_MORE_DOCS
			mDoc = target-1;
			return nextDoc();
		}

		@Override
		public float getScore() throws IOException {
			double dist = (mDoc == mLastDistDoc) ? mLastDist : 
				dist(mLatVals.doubleVal(mDoc), mLonVals.doubleVal(mDoc));
			
			return (float)(dist * mQWeight);
		}

		@Override
		public float getFreq() throws IOException {
			return 1;
		}

		public IExplanation explain(int doc) throws IOException {
			advance(doc);
			
			boolean matched = mDoc == doc;
			mDoc = doc;

			float sc = matched ? getScore() : 0;
			double dist = dist(mLatVals.doubleVal(doc), mLonVals.doubleVal(doc));

			String description = SpatialDistanceQuery.this.toString();

			Explanation result = new ComplexExplanation(mDoc == doc, sc, 
					description +  " product of:");
			
			//result.addDetail(new Explanation((float)dist, 
			//		"hsin(" + mLatVals.explain(doc) + "," + mLonVals.explain(doc)));
			result.addDetail(new Explanation((float)dist, 
					"hsin(" + mLatVals.doubleVal(doc) + "," + mLonVals.doubleVal(doc)));
			result.addDetail(new Explanation(getBoost(), "boost"));
			result.addDetail(new Explanation(mWeight.mQueryNorm, "queryNorm"));
			
			return result;
		}
	}

	@Override
	public DelegatingCollector getFilterCollector(ISearcher searcher) {
		try {
			return new SpatialCollector(new SpatialWeight(searcher));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unused")
	private class SpatialCollector extends DelegatingCollector {
		private final SpatialWeight mWeight;
		private SpatialScorer mSpatialScorer;
		private int mMaxdoc;
    
		public SpatialCollector(SpatialWeight weight) {
			mWeight = weight;
		}

		@Override
		public void collect(int doc) throws IOException {
			mSpatialScorer.mDoc = doc;
			if (mSpatialScorer.match()) 
				mDelegate.collect(doc);
		}

		@Override
		public void setNextReader(IAtomicReaderRef context) throws IOException {
			mMaxdoc = context.getReader().getMaxDoc();
			mSpatialScorer = new SpatialScorer(context, null, mWeight, 1.0f);
			super.setNextReader(context);
		}
	}

	@Override
	public Weight createWeight(ISearcher searcher) throws IOException {
		// if we were supposed to use bboxQuery, then we should have been rewritten using that query
		assert mBBoxQuery == null;
		return new SpatialWeight(searcher);
	}

	// Prints a user-readable version of this query. 
	@Override
	public String toString(String field) {
		float boost = getBoost();
		return (boost != 1.0 ? "(" : "") + (mCalcDist ? "geofilt" : "bbox") 
				+ "(latlonSource=" + mOrigField + "(" + mLatSource + "," + mLonSource + ")"
				+ ",latCenter=" + mLatCenter + ",lonCenter=" + mLonCenter + ",dist=" + mDist
				+ ",latMin=" + mLatMin + ",latMax=" + mLatMax
				+ ",lonMin=" + mLonMin + ",lonMax" + mLonMax
				+ ",lon2Min=" + mLon2Min + ",lon2Max" + mLon2Max
				+ ",calcDist=" + mCalcDist + ",planetRadius=" + mPlanetRadius
				// + (mBBoxQuery == null ? "" : ",bboxQuery=" + mBBoxQuery)
				+ ")" + (boost==1.0 ? "" : ")^"+boost);
	}

	// Returns true if <code>o</code> is equal to this. 
	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) return false;
		
		SpatialDistanceQuery other = (SpatialDistanceQuery)o;
		
		return this.mLatCenter == other.mLatCenter
				&& this.mLonCenter == other.mLonCenter
				&& this.mLatMin == other.mLatMin
				&& this.mLatMax == other.mLatMax
				&& this.mLonMin == other.mLonMin
				&& this.mLonMax == other.mLonMax
				&& this.mLon2Min == other.mLon2Min
				&& this.mLon2Max == other.mLon2Max
				&& this.mDist == other.mDist
				&& this.mPlanetRadius == other.mPlanetRadius
				&& this.mCalcDist == other.mCalcDist
				&& this.mLonSource.equals(other.mLonSource)
				&& this.mLatSource.equals(other.mLatSource)
				&& this.getBoost() == other.getBoost();
	}

	// Returns a hash code value for this object. 
	@Override
	public int hashCode() {
		// don't bother making the hash expensive - the center latitude + min longitude will be very unique
		long hash = Double.doubleToLongBits(mLatCenter);
		hash = hash * 31 + Double.doubleToLongBits(mLonMin);
		hash = hash * 31 + (long)super.hashCode();
		return (int)(hash >> 32 + hash);
	}

}
