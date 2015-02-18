package org.javenstudio.lightning.core.datum;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.Metadata;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.datum.index.IndexDocs;
import org.javenstudio.falcon.datum.util.TimeUtils;
import org.javenstudio.falcon.search.dataimport.ImportContext;
import org.javenstudio.falcon.search.dataimport.ImportProcessorBase;
import org.javenstudio.falcon.search.dataimport.ImportRequest;
import org.javenstudio.falcon.search.dataimport.ImportRow;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.IParams;

public class DatumImporter extends ImportProcessorBase {
	static final Logger LOG = Logger.getLogger(DatumImporter.class);
	
	public static final String DATUM_NAME = "datum";
	
	private final IndexDocsImpl mDocs;
	private final DatumCore mCore;
	
	public DatumImporter(DatumCore core, ImportContext context, 
			String entityName) throws ErrorException { 
		super(context, entityName);
		mCore = core;
		mDocs = new IndexDocsImpl(core);
	}
	
	public final DatumCore getCore() { return mCore; }
	
    @Override
    public void init(ImportRequest req) throws ErrorException { 
    	if (LOG.isDebugEnabled())
    		LOG.debug("init: request=" + req);
    	
    	IMember user = UserHelper.checkUser(UserHelper.getUserKeyTokens(
    			req.getParams().get(IParams.TOKEN)), null);
    	
    	String accesskey = req.getParams().get("accesskey");
    	String cmd = req.getParams().get("command");
    	String deep = req.getParams().get("deepscan");
    	String[] ids = req.getParams().getParams("id");
    	
    	boolean delta = cmd != null && cmd.equalsIgnoreCase("delta-import");
    	boolean deepscan = deep != null && deep.equalsIgnoreCase("true");
    	
    	String[] names = mDocs.init(
    			SectionHelper.getData(user, ids, IData.Access.INDEX, accesskey), 
    			delta, deepscan);
    	
    	if (names != null) { 
    		for (String name : names) { 
    			if (name != null && name.length() > 0)
    				req.addSource(name);
    		}
    	}
    }
	
	@Override
    public ImportRow nextRow() throws ErrorException {
		return mDocs.nextDoc();
    }
	
	@Override
    public ImportRow nextModifiedRow() throws ErrorException {
    	return mDocs.nextModifiedRow();
    }

    @Override
    public ImportRow nextDeletedRow() throws ErrorException {
    	return mDocs.nextDeletedRow();
    }
	
	@Override
	public synchronized void close() { 
		try {
			mDocs.close();
		} catch (Throwable ex) { 
			if (LOG.isErrorEnabled())
				LOG.error("close error: " + ex.toString(), ex);
		}
	}
	
	static class IndexDocsImpl extends IndexDocs<ImportRow> {
		public IndexDocsImpl(DatumCore core) { 
			super(core);
		}
		
		@Override
		protected ImportRow wrapDoc(String id) throws ErrorException { 
			if (id == null || id.length() == 0)
				return null;
			
			ImportRow row = new ImportRow();
			try {
				addField(row, "id", id);
				
				onWrapped(row);
			} catch (Throwable ex) { 
				if (LOG.isWarnEnabled())
					LOG.warn("wrapDoc: " + id + " error: " + ex, ex);
				
				//onWrapErr(file, ex);
				return null;
			}
			
			return row;
		}
		
		@Override
		protected ImportRow wrapDoc(final ISection file) throws ErrorException { 
			if (file == null) return null;
			
			ImportRow row = new ImportRow();
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
		
		protected void addMetaFields(ImportRow doc, ISection file) 
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
		
		protected void addTextFields(ImportRow doc, ISection file) 
				throws ErrorException { 
			if (doc == null || file == null) return;
			
			addField(doc, "text", file.getName());
			//addField(doc, "text", normalizePath(file.getContentPath()));
		}
		
		protected void onWrapErr(ISection file, Throwable ex) {}
		protected void onWrapped(ImportRow doc) throws IOException {}
	}
	
	private static void addDateField(ImportRow doc, Map<String,Object> infos, 
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
	
	private static void addIntField(ImportRow doc, Map<String,Object> infos, 
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
	
	private static void addField(ImportRow doc, Map<String,Object> infos, 
			String[] tags, String name) { 
		Object val = getMetaTag(infos, tags);
		if (val != null) 
			addField(doc, name, val);
	}
	
	private static Object getMetaTag(Map<String,Object> infos, String[] tags) { 
		if (infos == null || tags == null) return null;
		
		for (String tag : tags) { 
			Object val = infos.get(tag);
			if (val != null) return val;
		}
		
		return null;
	}
	
	private static void addField(ImportRow doc, String name, Object val) { 
		if (doc != null && name != null && val != null)
			doc.addField(name, val);
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
