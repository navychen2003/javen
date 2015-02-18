package org.javenstudio.hornet.query.source;

import org.javenstudio.hornet.query.ValueSource;

/**
 * A {@link ValueSource} that abstractly represents {@link ValueSource}s for
 * poly fields, and other things.
 */
public abstract class MultiValueSource extends ValueSource {

	public abstract int dimension();
	
}
