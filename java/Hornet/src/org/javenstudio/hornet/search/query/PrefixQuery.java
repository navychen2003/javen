package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.util.StringHelper;

/** 
 * A Query that matches documents containing terms with a specified prefix. A PrefixQuery
 * is built by QueryParser for input like <code>app*</code>.
 *
 * <p>This query uses the {@link
 * MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT}
 * rewrite method. 
 */
public class PrefixQuery extends MultiTermQuery {
	
	private ITerm mPrefix;

	/** Constructs a query for terms starting with <code>prefix</code>. */
	public PrefixQuery(ITerm prefix) {
		super(prefix.getField());
		mPrefix = prefix;
	}

	/** Returns the prefix of this query. */
	public ITerm getPrefix() { return mPrefix; }
  
	@Override  
	protected ITermsEnum getTermsEnum(ITerms terms) throws IOException {
		ITermsEnum tenum = terms.iterator(null);
    
		if (mPrefix.getBytes().getLength() == 0) {
			// no prefix -- match all terms for this field:
			return tenum;
		}
		
		return new PrefixTermsEnum(tenum, mPrefix.getBytes());
	}

	/** Prints a user-readable version of this query. */
	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		if (!getFieldName().equals(field)) {
			buffer.append(getFieldName());
			buffer.append(":");
		}
		buffer.append(mPrefix.getText());
		buffer.append('*');
		buffer.append(StringHelper.toBoostString(getBoost()));
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mPrefix == null) ? 0 : mPrefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		PrefixQuery other = (PrefixQuery) obj;
		if (mPrefix == null) {
			if (other.mPrefix != null)
				return false;
		} else if (!mPrefix.equals(other.mPrefix)) {
			return false;
		}
		
		return true;
	}

}
