package org.javenstudio.common.indexdb.search;

/** 
 * Expert: Describes the score computation for document and query, and
 * can distinguish a match independent of a positive value. 
 */
public class ComplexExplanation extends Explanation {
	
	private Boolean mMatch;
  
	public ComplexExplanation() {
		super();
	}

	public ComplexExplanation(boolean match, float value, String description) {
		// NOTE: use of "boolean" instead of "Boolean" in params is conscious
		// choice to encourage clients to be specific.
		super(value, description);
		mMatch = Boolean.valueOf(match);
	}

	/**
	 * The match status of this explanation node.
	 * @return May be null if match status is unknown
	 */
	public Boolean getMatch() { return mMatch; }
	
	/**
	 * Sets the match status assigned to this explanation node.
	 * @param match May be null if match status is unknown
	 */
	public void setMatch(Boolean match) { mMatch = match; }
	
	/**
	 * Indicates whether or not this Explanation models a good match.
	 *
	 * <p>
	 * If the match status is explicitly set (i.e.: not null) this method
	 * uses it; otherwise it defers to the superclass.
	 * </p>
	 * @see #getMatch
	 */
	@Override
	public boolean isMatch() {
		Boolean m = getMatch();
		return (null != m ? m.booleanValue() : super.isMatch());
	}

	@Override
	protected String getSummary() {
		if (null == getMatch())
			return super.getSummary();
    
		return getValue() + " = "
			+ (isMatch() ? "(MATCH) " : "(NON-MATCH) ")
			+ getDescription();
	}
  
}
