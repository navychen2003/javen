package org.javenstudio.common.indexdb;

public interface IExplanation {

	/**
	 * Indicates whether or not this Explanation models a good match.
	 *
	 * <p>
	 * By default, an Explanation represents a "match" if the value is positive.
	 * </p>
	 * @see #getValue
	 */
	public boolean isMatch();
	
	/** The value assigned to this explanation node. */
	public float getValue();
	
	/** A description of this explanation node. */
	public String getDescription();
	
	/** The sub-nodes of this explanation node. */
	public IExplanation[] getDetails();
	
	/** Render an explanation as text. */
	public String toString(int depth);
	
	/** Render an explanation as HTML. */
	public String toHtml();
	
}
