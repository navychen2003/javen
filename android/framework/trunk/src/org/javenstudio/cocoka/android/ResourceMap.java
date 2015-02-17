package org.javenstudio.cocoka.android;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class ResourceMap {

	public static final String ANIM = "anim"; 
	public static final String ARRAY = "array"; 
	public static final String ATTR = "attr"; 
	public static final String COLOR = "color"; 
	public static final String DRAWABLE = "drawable"; 
	public static final String ID = "id"; 
	public static final String LAYOUT = "layout"; 
	public static final String PLURALS = "plurals"; 
	public static final String STRING = "string"; 
	public static final String XML = "xml"; 
	
	protected static class FieldMap { 
		private Map<Integer, String> sIdToNameMap = null; 
		private Map<String, Integer> sNameToIdMap = null; 
		
		public FieldMap() {}
		
		synchronized void addField(String name, int value) { 
			if (name == null) return; 
			
			if (sIdToNameMap == null) 
				sIdToNameMap = new HashMap<Integer, String>(); 
			if (sNameToIdMap == null) 
				sNameToIdMap = new HashMap<String, Integer>(); 
			
			sIdToNameMap.put(value, name); 
			sNameToIdMap.put(name, value); 
		}
		
		synchronized int getId(String name) { 
			if (sNameToIdMap != null && name != null) { 
				Integer num = sNameToIdMap.get(name); 
				if (num != null) 
					return num.intValue();
			}
			return 0; 
		}
		
		synchronized String getName(int id) { 
			if (sIdToNameMap != null) { 
				return sIdToNameMap.get(id); 
			}
			return null;
		}
	}
	
	private static final Object sLock = new Object(); 
	private static Map<String, FieldMap> sFieldMaps = null; 
	private static Class<?> sResourceClass = null; 
	
	private static FieldMap getFieldMap(String fieldName) { 
		if (fieldName == null) 
			return null;
		
		synchronized (sLock) { 
			if (sResourceClass == null) 
				return null;
			
			if (sFieldMaps == null) 
				sFieldMaps = new HashMap<String, FieldMap>(); 
			
			FieldMap fieldMap = sFieldMaps.get(fieldName); 
			if (fieldMap == null) { 
				Class<?>[] classes = sResourceClass.getClasses(); 
				Class<?> fieldClass = null; 
				
				String endsName = "$" + fieldName; 
				for (int i=0; classes != null && i < classes.length; i++) { 
					Class<?> clazz = classes[i]; 
					if (clazz == null) continue;
					
					if (clazz.getName().endsWith(endsName)) { 
						fieldClass = clazz; 
						break; 
					}
				}
				
				fieldMap = new FieldMap(); 
				if (fieldClass != null) { 
					Field[] fields = fieldClass.getFields(); 
					for (int j=0; fields != null && j < fields.length; j++) { 
						Field field = fields[j]; 
						if (field == null) continue; 
						try { 
							String name = field.getName(); 
							int value = field.getInt(fieldClass); 
							
							fieldMap.addField(name, value); 
						} catch (Exception e) { 
							// ignore
						}
					}
				}
				
				sFieldMaps.put(fieldName, fieldMap); 
			}
			
			return fieldMap;
		}
	}
	
	public static void setResourceClass(Class<?> resourceClass) { 
		if (resourceClass == null) return;
		
		synchronized (sLock) { 
			if (sResourceClass == null) 
				sResourceClass = resourceClass; 
		}
	}
	
	public static int getResourceId(String fieldName, String name) { 
		if (fieldName == null || name == null) 
			return 0;
		
		FieldMap fieldMap = getFieldMap(fieldName); 
		if (fieldMap != null) 
			return fieldMap.getId(name); 
		
		return 0;
	}
	
	public static String getResourceName(String fieldName, int id) { 
		if (fieldName == null || id == 0) 
			return null;
		
		FieldMap fieldMap = getFieldMap(fieldName); 
		if (fieldMap != null) 
			return fieldMap.getName(id); 
		
		return null;
	}
	
}
