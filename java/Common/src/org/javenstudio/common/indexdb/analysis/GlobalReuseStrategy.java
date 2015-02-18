package org.javenstudio.common.indexdb.analysis;

/**
 * Implementation of {@link ReuseStrategy} that reuses the same components for
 * every field.
 */
public final class GlobalReuseStrategy extends ReuseStrategy {

	/**
	 * {@inheritDoc}
	 */
	public TokenComponents getReusableComponents(String fieldName) {
		return (TokenComponents) getStoredValue();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setReusableComponents(String fieldName, TokenComponents components) {
		setStoredValue(components);
	}
	
}