package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.hornet.index.segment.CompositeIndexReader;
import org.javenstudio.hornet.index.segment.SlowCompositeReaderWrapper;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.IntDocValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueInt;

/**
 * Obtains the ordinal of the field value from the default Lucene 
 * {@link FieldCache} using getStringIndex().
 * <br>
 * The native lucene index order is used to assign an ordinal value 
 * for each field value.
 * <br>Field values (terms) are lexicographically ordered by unicode value, 
 * and numbered starting at 1.
 * <br>
 * Example:<br>
 *  If there were only three field values: "apple","banana","pear"
 * <br>then ord("apple")=1, ord("banana")=2, ord("pear")=3
 * <p>
 * WARNING: ord() depends on the position in an index and can thus change 
 * when other documents are inserted or deleted,
 *  or if a MultiSearcher is used.
 * <br>WARNING: as of Solr 1.4, ord() and rord() can cause excess memory use 
 * since they must use a FieldCache entry
 * at the top level reader, while sorting and function queries now use entries 
 * at the segment level.  Hence sorting
 * or using a different function query, in addition to ord()/rord() 
 * will double memory use.
 *
 */
public class OrdFieldSource extends ValueSource {
  
	protected final String mField;

	public OrdFieldSource(String field) {
		mField = field;
	}

	@Override
	public String getDescription() {
		return "ord(" + mField + ')';
	}

	// TODO: this is trappy? perhaps this query instead should 
	// make you pass a slow reader yourself?
	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		final int off = readerContext.getDocBase();
		final IIndexReader topReader = ReaderUtil.getTopLevel(readerContext).getReader();
		
		@SuppressWarnings("resource")
		final IAtomicReader r = topReader instanceof CompositeIndexReader 
				? new SlowCompositeReaderWrapper((CompositeIndexReader)topReader) 
        	: (IAtomicReader) topReader;
				
		final IDocTermsIndex sindex = FieldCache.DEFAULT.getTermsIndex(r, mField);
		
		return new IntDocValues(this) {
			@SuppressWarnings("unused")
			protected String toTerm(String readableValue) {
				return readableValue;
			}
			
			@Override
			public int intVal(int doc) {
				return sindex.getOrd(doc+off);
			}
			
			@Override
			public int ordVal(int doc) {
				return sindex.getOrd(doc+off);
			}
			
			@Override
			public int numOrd() {
				return sindex.getNumOrd();
			}

			@Override
			public boolean exists(int doc) {
				return sindex.getOrd(doc+off) != 0;
			}

			@Override
			public ValueFiller getValueFiller() {
				return new ValueFiller() {
					private final MutableValueInt mVal = new MutableValueInt();

					@Override
					public MutableValue getValue() {
						return mVal;
					}

					@Override
					public void fillValue(int doc) {
						mVal.set(sindex.getOrd(doc));
						mVal.setExists(mVal.get() != 0);
					}
				};
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		return o != null && o.getClass() == OrdFieldSource.class && 
				this.mField.equals(((OrdFieldSource)o).mField);
	}

	private static final int sHashCode = OrdFieldSource.class.hashCode();
	
	@Override
	public int hashCode() {
		return sHashCode + mField.hashCode();
	}

}
