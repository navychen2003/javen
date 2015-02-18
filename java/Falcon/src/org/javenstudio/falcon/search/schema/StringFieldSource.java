package org.javenstudio.falcon.search.schema;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.query.DocTermsIndexDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.query.source.FieldCacheSource;

public class StringFieldSource extends FieldCacheSource {

	public StringFieldSource(String field) {
		super(field);
	}

	@Override
	public String getDescription() {
		return "string(" + getField() + ')';
	}
	
	@Override
	public FunctionValues getValues(ValueSourceContext context, 
			IAtomicReaderRef readerContext) throws IOException {
		return new DocTermsIndexDocValues(this, readerContext, getField()) {
			@Override
			protected String toTerm(String readableValue) {
				return readableValue;
			}

			@Override
			public int ordVal(int doc) {
				return mTermsIndex.getOrd(doc);
			}

			@Override
			public int numOrd() {
				return mTermsIndex.getNumOrd();
			}

			@Override
			public Object objectVal(int doc) {
				return stringVal(doc);
			}

			@Override
			public String toString(int doc) {
				return getDescription() + '=' + stringVal(doc);
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		return o != null && (o instanceof StringFieldSource) && super.equals(o);
	}

	private static int sHashCode = StringFieldSource.class.hashCode();
	@Override
	public int hashCode() {
		return sHashCode + super.hashCode();
	};
	
}
