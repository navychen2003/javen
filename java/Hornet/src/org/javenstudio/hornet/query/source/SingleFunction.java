package org.javenstudio.hornet.query.source;

import java.io.IOException;

import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;

/** 
 * A function with a single argument
 */
 public abstract class SingleFunction extends ValueSource {
	 
	 protected final ValueSource mSource;

	 public SingleFunction(ValueSource source) {
		 mSource = source;
	 }

	 protected abstract String getName();

	 @Override
	 public String getDescription() {
		 return getName() + '(' + mSource.getDescription() + ')';
	 }

	 @Override
	 public int hashCode() {
		 return mSource.hashCode() + getName().hashCode();
	 }

	 @Override
	 public boolean equals(Object o) {
		 if (this.getClass() != o.getClass()) 
			 return false;
		 
		 SingleFunction other = (SingleFunction)o;
		 return this.getName().equals(other.getName())
				 && this.mSource.equals(other.mSource);
	 }

	 @Override
	 public void createWeight(ValueSourceContext context, 
			 ISearcher searcher) throws IOException {
		 mSource.createWeight(context, searcher);
	 }
	 
}