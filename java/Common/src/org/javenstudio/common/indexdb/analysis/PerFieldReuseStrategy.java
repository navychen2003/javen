package org.javenstudio.common.indexdb.analysis;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link ReuseStrategy} that reuses components per-field by
 * maintaining a Map of TokenStreamComponent per field name.
 */
public class PerFieldReuseStrategy extends ReuseStrategy {

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public TokenComponents getReusableComponents(String fieldName) {
		Map<String, TokenComponents> componentsPerField = (Map<String, TokenComponents>) getStoredValue();
		return componentsPerField != null ? componentsPerField.get(fieldName) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void setReusableComponents(String fieldName, TokenComponents components) {
		Map<String, TokenComponents> componentsPerField = (Map<String, TokenComponents>) getStoredValue();
		if (componentsPerField == null) {
			componentsPerField = new HashMap<String, TokenComponents>();
			setStoredValue(componentsPerField);
		}
		componentsPerField.put(fieldName, components);
	}
	
}