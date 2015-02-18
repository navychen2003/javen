package org.javenstudio.falcon.datum.index;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.Metadata;
import org.javenstudio.falcon.datum.util.TimeUtils;
import org.javenstudio.hornet.wrapper.SimpleDocument;

public class IndexDoc {
	private static final Logger LOG = Logger.getLogger(IndexDoc.class);

	private final ISection mSection;
	
	public IndexDoc(ISection section) { 
		if (section == null) throw new NullPointerException();
		mSection = section;
	}
	
	public SimpleDocument toDoc() { 
		ISection section = mSection;
		if (section == null) return null;
		
		return wrapDoc(section);
	}
	
	private SimpleDocument wrapDoc(final ISection file) { 
		if (file == null) return null;
		
		SimpleDocument row = new SimpleDocument();
		try {
			addField(row, "id", file.getContentId());
			addField(row, "library", file.getLibrary().getContentId());
			addField(row, "title", file.getName());
			addField(row, "content_type", file.getContentType());
			//addField(row, "path", normalizePath(file.getContentPath()));
			addField(row, "length", new Long(file.getContentLength()));
			addField(row, "updated", new Date(file.getModifiedTime()));
			
			addMetaFields(row, file);
			addTextFields(row, file);
			onWrapped(row);
		} catch (Throwable ex) { 
			if (LOG.isWarnEnabled())
				LOG.warn("wrapDoc: " + file + " error: " + ex, ex);
			
			onWrapErr(file, ex);
			return null;
		}
		
		return row;
	}
	
	private void addMetaFields(SimpleDocument doc, ISection file) 
			throws ErrorException { 
		if (doc == null || file == null) return;
		
		Map<String,Object> infos = new HashMap<String,Object>();
		file.getMetaInfo(infos);
		
		addDateField(doc, infos, Metadata.TAKEN_TAGS, "taken");
		
		addField(doc, infos, Metadata.MAKE_TAGS, "make_s");
		addField(doc, infos, Metadata.MODEL_TAGS, "model_s");
		addField(doc, infos, Metadata.LENSMODEL_TAGS, "lensmodel_s");
		
		addField(doc, infos, Metadata.APERTURE_TAGS, "aperture_s");
		addField(doc, infos, Metadata.ISO_TAGS, "iso_s");
		addField(doc, infos, Metadata.FOCALLENGTH_TAGS, "focal_s");
		addField(doc, infos, Metadata.EXPOSURETIME_TAGS, "exposure_s");
		
		addIntField(doc, infos, Metadata.WIDTH_TAGS, "width");
		addIntField(doc, infos, Metadata.HEIGHT_TAGS, "height");
		
		Object latitude = getMetaTag(infos, Metadata.LATITUDE_TAGS);
		Object longitude = getMetaTag(infos, Metadata.LONGITUDE_TAGS);
		
		if (latitude != null && longitude != null) { 
			String value = normalizePosition(latitude.toString(), longitude.toString());
			if (value != null && value.length() > 0)
				addField(doc, "position", value);
		}
	}
	
	private void addTextFields(SimpleDocument doc, ISection file) 
			throws ErrorException { 
		if (doc == null || file == null) return;
		
		addField(doc, "text", file.getName());
		//addField(doc, "text", normalizePath(file.getContentPath()));
	}
	
	protected void onWrapErr(ISection file, Throwable ex) {}
	protected void onWrapped(SimpleDocument doc) throws IOException {}
	
	protected int getFieldFlags(String name) { 
		if (name == null) return 0;
		
		if (name.equals("title")) { 
			return SimpleDocument.FLAG_INDEX | 
        			SimpleDocument.FLAG_TOKENIZE | 
        			SimpleDocument.FLAG_STORE_FIELD |
        			SimpleDocument.FLAG_STORE_TERMVECTORS;
			
		} else if (name.equals("text")) { 
			return SimpleDocument.FLAG_INDEX | 
        			SimpleDocument.FLAG_TOKENIZE | 
        			SimpleDocument.FLAG_STORE_TERMVECTORS;
		}
		
		return SimpleDocument.FLAG_INDEX | 
				SimpleDocument.FLAG_STORE_FIELD | 
    			SimpleDocument.FLAG_STORE_TERMVECTORS;
	}
	
	private void addDateField(SimpleDocument doc, Map<String,Object> infos, 
			String[] tags, String name) { 
		Object val = getMetaTag(infos, tags);
		if (val != null) { 
			if (val instanceof Date) {
				addField(doc, name, val);
			} else if (val instanceof Long) {
				addField(doc, name, new Date((Long)val));
			} else { 
				addField(doc, name, TimeUtils.parseDate(val.toString()));
			}
		}
	}
	
	private void addIntField(SimpleDocument doc, Map<String,Object> infos, 
			String[] tags, String name) { 
		Object val = getMetaTag(infos, tags);
		if (val != null) { 
			if (val instanceof Number) {
				addField(doc, name, new Integer(((Number)val).intValue()));
			} else { 
				try {
					addField(doc, name, Integer.valueOf(val.toString()));
				} catch (Throwable e) { 
					// ignore
				}
			}
		}
	}
	
	private void addField(SimpleDocument doc, Map<String,Object> infos, 
			String[] tags, String name) { 
		Object val = getMetaTag(infos, tags);
		if (val != null) 
			addField(doc, name, val);
	}
	
	private Object getMetaTag(Map<String,Object> infos, String[] tags) { 
		if (infos == null || tags == null) return null;
		
		for (String tag : tags) { 
			Object val = infos.get(tag);
			if (val != null) return val;
		}
		
		return null;
	}
	
	private void addField(SimpleDocument doc, String name, Object val) { 
		if (doc != null && name != null && val != null) { 
			if (val instanceof Date) {
				doc.addField(name, ((Date)val).getTime(), true);
				
			} else if (val instanceof Number) { 
				doc.addField(name, ((Number)val).floatValue(), true);
				
			} else { 
				doc.addField(name, val.toString(), getFieldFlags(name));
			}
		}
	}
	
	private static String normalizePosition(String latitude, String longitude) { 
		if (latitude != null && longitude != null) { 
			float latitudeVal = parsePosition(latitude);
			float longitudeVal = parsePosition(longitude);
			
			if (latitudeVal != 0 || longitudeVal != 0)
				return "" + latitudeVal + "," + longitudeVal;
		}
		
		return null;
	}
	
	private static float parsePosition(String text) { 
		if (text != null) { 
			String val1 = null, val2 = null, val3 = null;
			boolean flag = false;
			
			StringBuilder sbuf = new StringBuilder();
			
			for (int i=0; i < text.length(); i++) { 
				char chr = text.charAt(i);
				if ((chr >= '0' && chr <= '9') || chr == '.') {
					sbuf.append(chr);
				} else if (chr == '-') { 
					flag = true;
				} else if (sbuf.length() > 0) {
					if (val1 == null) val1 = sbuf.toString();
					else if (val2 == null) val2 = sbuf.toString();
					else if (val3 == null) val3 = sbuf.toString();
					sbuf.setLength(0);
				}
			}
			
			float num1 = 0, num2 = 0, num3 = 0;
			
			try { if (val1 != null) num1 = Float.parseFloat(val1); }
			catch (Throwable e) {}
			
			try { if (val2 != null) num2 = Float.parseFloat(val2); }
			catch (Throwable e) {}
			
			try { if (val3 != null) num3 = Float.parseFloat(val3); }
			catch (Throwable e) {}
			
			float num = num1 + (num2 / 60) + (num3 / (60 * 60));
			if (flag) num = num * (-1);
			
			return num;
		}
		
		return 0;
	}
	
}
