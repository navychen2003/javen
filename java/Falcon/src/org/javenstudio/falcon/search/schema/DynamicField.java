package org.javenstudio.falcon.search.schema;

import org.javenstudio.falcon.ErrorException;

//
// Instead of storing a type, this could be implemented as a hierarchy
// with a virtual matches().
// Given how often a search will be done, however, speed is the overriding
// concern and I'm not sure which is faster.
//
public class DynamicField extends DynamicReplacement {
	
	private final SchemaField mPrototype;

	public DynamicField(SchemaField prototype) throws ErrorException {
		super(prototype.getName());
		mPrototype = prototype;
	}

	public final SchemaField getPrototype() { return mPrototype; }
	
	public SchemaField makeSchemaField(String name) throws ErrorException {
		// could have a cache instead of returning a new one each time, but it might
		// not be worth it.
		// Actually, a higher level cache could be worth it to avoid too many
		// .startsWith() and .endsWith() comparisons.  it depends on how many
		// dynamic fields there are.
		return new SchemaField(mPrototype, name);
	}

	@Override
	public String toString() {
		return mPrototype.toString();
	}
	
}
