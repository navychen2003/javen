package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.ITerm;

/**
 * A Filter that restricts search results to values that have a matching prefix in a given
 * field.
 */
public class PrefixFilter extends MultiTermQueryWrapperFilter<PrefixQuery> {

	public PrefixFilter(ITerm prefix) {
		super(new PrefixQuery(prefix));
	}

	public ITerm getPrefix() { return mQuery.getPrefix(); }

	/** Prints a user-readable version of this query. */
	@Override
	public String toString () {
		StringBuilder buffer = new StringBuilder();
		buffer.append("PrefixFilter{");
		buffer.append(getPrefix().toString());
		buffer.append("}");
		return buffer.toString();
	}

}
