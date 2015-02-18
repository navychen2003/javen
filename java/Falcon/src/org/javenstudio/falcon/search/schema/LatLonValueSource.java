package org.javenstudio.falcon.search.schema;

import java.util.List;

import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.VectorValueSource;

public class LatLonValueSource extends VectorValueSource {
	
	private final SchemaField mField;

	public LatLonValueSource(SchemaField sf, List<ValueSource> sources) {
		super(sources);
		mField = sf;
	}

	@Override
	public String getName() {
		return "latlon";
	}

	@Override
	public String getDescription() {
		return getName() + "(" + mField.getName() + ")";
	}
	
}
