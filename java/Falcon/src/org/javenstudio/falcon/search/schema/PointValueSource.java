package org.javenstudio.falcon.search.schema;

import java.util.List;

import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.VectorValueSource;

public class PointValueSource extends VectorValueSource {
	
	private final SchemaField mSchemaField;
  
	public PointValueSource(SchemaField sf, List<ValueSource> sources) {
		super(sources);
		mSchemaField = sf;
	}

	@Override
	public String getName() {
		return "point";
	}

	@Override
	public String getDescription() {
		return getName() + "(" + mSchemaField.getName() + ")";
	}
	
}
