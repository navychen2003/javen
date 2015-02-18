package org.javenstudio.falcon.search.dataimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportRow {

	public static final String DELETE_DOC_BY_ID = "$deleteDocById";
	public static final String DELETE_DOC_BY_QUERY = "$deleteDocByQuery";
	
	private final Map<String, List<ImportField>> mFields = 
			new HashMap<String, List<ImportField>>(); 
	
	private Float mDocBoost = null;
	
	public ImportRow() {}
	
	public Float getBoost() { return mDocBoost; }
	public void setBoost(float boost) { mDocBoost = boost; }
	
	public Collection<String> nameSet() { 
		return mFields.keySet();
	}
	
	public List<ImportField> getFields(String fieldName) { 
		return mFields.get(fieldName);
	}
	
	public Object get(String fieldName) { 
		List<ImportField> fields = getFields(fieldName);
		if (fields != null && fields.size() > 0) 
			return fields.get(0).getValue();
		
		return null;
	}
	
	public void addField(String fieldName, Object fieldValue) { 
		if (fieldName == null || fieldValue == null) return;
		addField(new ImportField(fieldName, fieldValue));
	}
	
	public void addField(String fieldName, Object fieldValue, float boost) { 
		if (fieldName == null || fieldValue == null) return;
		ImportField field = new ImportField(fieldName, fieldValue);
		field.setBoost(boost);
		addField(field);
	}
	
	public void addField(ImportField field) { 
		if (field == null) return;
		
		List<ImportField> fields = mFields.get(field.getName());
		if (fields == null) { 
			fields = new ArrayList<ImportField>();
			mFields.put(field.getName(), fields);
		}
		
		fields.add(field);
	}
	
	public Map<String,Object> toMap() { 
		Map<String, Object> map = new HashMap<String, Object>(); 
		
		for (String fieldName : nameSet()) { 
			Object fieldValue = get(fieldName);
			if (fieldValue != null)
				map.put(fieldName, fieldValue);
		}
		
		return map;
	}
	
	public void done() {}
	
}
