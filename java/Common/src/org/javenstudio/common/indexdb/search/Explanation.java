package org.javenstudio.common.indexdb.search;

import java.util.ArrayList;

import org.javenstudio.common.indexdb.IExplanation;

/** Expert: Describes the score computation for document and query. */
public class Explanation implements IExplanation {
	
	private float mValue;                         	// the value of this node
	private String mDescription;                  	// what it represents
	private ArrayList<IExplanation> mDetails;    	// sub-explanations

	public Explanation() {}

	public Explanation(float value, String description) {
		mValue = value;
		mDescription = description;
	}

	/**
	 * Indicates whether or not this Explanation models a good match.
	 *
	 * <p>
	 * By default, an Explanation represents a "match" if the value is positive.
	 * </p>
	 * @see #getValue
	 */
	@Override
	public boolean isMatch() {
		return (0.0f < getValue());
	}
  
	/** The value assigned to this explanation node. */
	public float getValue() { return mValue; }
	
	/** Sets the value assigned to this explanation node. */
	public void setValue(float value) { mValue = value; }

	/** A description of this explanation node. */
	public String getDescription() { return mDescription; }
	
	/** Sets the description of this explanation node. */
	public void setDescription(String description) {
		mDescription = description;
	}

	/**
	 * A short one line summary which should contain all high level
	 * information about this Explanation, without the "Details"
	 */
	protected String getSummary() {
		return getValue() + " = " + getDescription();
	}
  
	/** The sub-nodes of this explanation node. */
	public IExplanation[] getDetails() {
		if (mDetails == null)
			return null;
		
		return mDetails.toArray(new IExplanation[0]);
	}

	/** Adds a sub-node to this explanation node. */
	public void addDetail(IExplanation detail) {
		if (mDetails == null)
			mDetails = new ArrayList<IExplanation>();
		
		mDetails.add(detail);
	}

	/** Render an explanation as text. */
	@Override
	public String toString() {
		return toString(0);
	}
  
	@Override
	public String toString(int depth) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			buffer.append("  ");
		}
		buffer.append(getSummary());
		buffer.append("\n");

		IExplanation[] details = getDetails();
		if (details != null) {
			for (int i = 0 ; i < details.length; i++) {
				buffer.append(details[i].toString(depth+1));
			}
		}

		return buffer.toString();
	}

	/** Render an explanation as HTML. */
	@Override
	public String toHtml() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<ul>\n");

		buffer.append("<li>");
		buffer.append(getSummary());
		buffer.append("<br />\n");

		IExplanation[] details = getDetails();
		if (details != null) {
			for (int i = 0 ; i < details.length; i++) {
				buffer.append(details[i].toHtml());
			}
		}

		buffer.append("</li>\n");
		buffer.append("</ul>\n");

		return buffer.toString();
	}
	
}
