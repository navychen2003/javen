package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * Subclass of FilteredTermEnum for enumerating all terms that match the
 * specified prefix filter term.
 * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * greater than all that precede it.</p>
 */
public class PrefixTermsEnum extends FilteredTermsEnum {

	private final BytesRef mPrefixRef;

	public PrefixTermsEnum(ITermsEnum tenum, BytesRef prefixText) {
		super(tenum);
		setInitialSeekTerm(mPrefixRef = prefixText);
	}

	@Override
	protected AcceptStatus accept(BytesRef term) {
		if (StringHelper.startsWith(term, mPrefixRef)) 
			return AcceptStatus.YES;
		else 
			return AcceptStatus.END;
	}
	
}
