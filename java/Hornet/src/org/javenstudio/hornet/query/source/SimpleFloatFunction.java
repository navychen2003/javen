package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.query.FloatDocValues;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/** 
 * A simple float function with a single argument
 */
 public abstract class SimpleFloatFunction extends SingleFunction {
	 
	 public SimpleFloatFunction(ValueSource source) {
		 super(source);
	 }

	 protected abstract float callFunc(int doc, FunctionValues vals);

	 @Override
	 public FunctionValues getValues(ValueSourceContext context, 
			 IAtomicReaderRef readerContext) throws IOException {
		 final FunctionValues vals =  mSource.getValues(context, readerContext);
		 
		 return new FloatDocValues(this) {
			 @Override
			 public float floatVal(int doc) {
				 return callFunc(doc, vals);
			 }
			 
			 @Override
			 public String toString(int doc) {
				 return getName() + '(' + vals.toString(doc) + ')';
			 }
		 };
	 }
	 
}
