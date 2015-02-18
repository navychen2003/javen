package org.javenstudio.falcon.search.schema;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.DocTermsIndexDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.source.FieldCacheSource;

public class DateFieldSource extends FieldCacheSource {
	
	// NOTE: this is bad for serialization... 
	// but we currently need the fieldType for toInternal()
	private SchemaFieldType mFieldType;

	public DateFieldSource(String name, SchemaFieldType ft) {
		super(name);
		mFieldType = ft;
	}

	@Override
	public String getDescription() {
		return "date(" + mField + ')';
	}

	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return new DocTermsIndexDocValues(this, readerContext, mField) {
			@Override
			protected String toTerm(String readableValue) {
				try {
					// needed for frange queries to work properly
					return mFieldType.toInternal(readableValue);
				} catch (ErrorException ex) { 
					throw new RuntimeException(ex);
				}
			}

			@Override
			public float floatVal(int doc) {
				return (float)intVal(doc);
			}

			@Override
			public int intVal(int doc) {
				return mTermsIndex.getOrd(doc);
			}

			@Override
			public long longVal(int doc) {
				return (long)intVal(doc);
			}

			@Override
			public double doubleVal(int doc) {
				return (double)intVal(doc);
			}

			@Override
			public String stringVal(int doc) {
				int ord = mTermsIndex.getOrd(doc);
				if (ord == 0) 
					return null;
				
				try {
					final BytesRef br = mTermsIndex.lookup(ord, mSpare);
					return mFieldType.indexedToReadable(br, mSpareChars).toString();
				} catch (ErrorException ex) { 
					throw new RuntimeException(ex);
				}
			}

			@Override
			public Object objectVal(int doc) {
				int ord = mTermsIndex.getOrd(doc);
				if (ord == 0) 
					return null;
        
				try {
					final BytesRef br = mTermsIndex.lookup(ord, new BytesRef());
					return mFieldType.toObject(null, br);
				} catch (ErrorException ex) { 
					throw new RuntimeException(ex);
				}
			}

			@Override
			public String toString(int doc) {
				return getDescription() + '=' + intVal(doc);
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || o.getClass() != this.getClass()) 
			return false;
		
		return super.equals(o);
	}

	private static int sHashCode = DateFieldSource.class.hashCode();
	@Override
	public int hashCode() {
		return sHashCode + super.hashCode();
	}
	
}
