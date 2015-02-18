package org.javenstudio.falcon.datum.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.james.mime4j.field.datetime.DateTime;
import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.common.util.Logger;

public class TimeUtils {
	private static final Logger LOG = Logger.getLogger(TimeUtils.class);

	static final SimpleDateFormat sFormater  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	static final SimpleDateFormat sFormater0  = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss"); 
	static final SimpleDateFormat sFormater1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 
	static final SimpleDateFormat sFormater2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); 
	static final SimpleDateFormat sFormater3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); 
	static final SimpleDateFormat sFormater4 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); 
	static final SimpleDateFormat sFormater5 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); 
	static final SimpleDateFormat sFormater6 = new SimpleDateFormat("yyyy-MM-dd"); 
	
	static final SimpleDateFormat sFormater10  = new SimpleDateFormat("HH:mm:ss"); 
	
	public static Date parseDate(String text) { 
		Date date = null; 
		
		if (text != null && text.length() > 0) { 
			try { 
				if (date == null) { 
					DateTime dt = DateTime.parse(text); 
					if (dt != null) 
						date = dt.getDate(); 
				}
			} catch (Exception e) { }
			
			if (date == null) { 
				StringBuilder sbuf = new StringBuilder(); 
				boolean prespace = false; 
				text = text.toUpperCase();
				for (int i=0; i < text.length(); i++) { 
					char chr = text.charAt(i); 
					if ((chr >= '0' && chr <= '9') || chr == ':' || chr == '-' || 
						 chr == 'T' || chr == 'Z' || chr == '.') { 
						sbuf.append(chr); prespace = false; 
						continue; 
					}
					if (prespace == false) sbuf.append(' '); 
					prespace = true; 
				}
				text = ParseUtils.trim(sbuf.toString()); 
			}
			
			try { 
				if (date == null && text.length() == 19)
					date = sFormater.parse(text); 
			} catch (Exception e) {}
			
			try { 
				if (date == null && text.length() == 19)
					date = sFormater0.parse(text); 
			} catch (Exception e) {}
			
			try { 
				if (date == null && text.length() == 20)
					date = sFormater1.parse(text); 
			} catch (Exception e) {}
			
			try { 
				if (date == null && text.length() == 24)
					date = sFormater2.parse(text); 
			} catch (Exception e) {}
			
			try { 
				if (date == null && text.length() == 19)
					date = sFormater3.parse(text); 
			} catch (Exception e) {}
			
			try { 
				if (date == null && text.length() > 19)
					date = sFormater4.parse(text); 
			} catch (Exception e) {}
			
			try { 
				if (date == null && text.length() > 19)
					date = sFormater5.parse(text); 
			} catch (Exception e) {}
			
			try { 
				if (date == null && text.length() > 9)
					date = sFormater6.parse(text); 
			} catch (Exception e) {}
		}
		
		if (date == null && LOG.isDebugEnabled())
			LOG.debug("parseDate: cannot parse \"" + text + "\"");
		
		return date; 
	}
	
	public static long parseTime(String text) { 
		Date date = parseDate(text); 
		if (date != null) 
			return date.getTime();
		
		return 0;
	}
	
	public static String formatDate(String text) { 
		if (text != null && text.length() > 0) { 
			Date date = parseDate(text);
			if (date == null) { 
				int pos1 = text.lastIndexOf('+');
				int pos2 = text.lastIndexOf('-');
				int pos = pos1 > pos2 ? pos1 : pos2;
				if (pos > 0) { 
					String txt = text.substring(0, pos);
					date = parseDate(txt);
				}
			}
			
			if (date != null) 
				text = sFormater.format(date); 
		}
		
		return text; 
	}
	
	public static String formatDate(long time) { 
		if (time > 0) 
			return formatDate(new Date(time));
		
		return null;
	}
	
	public static String formatDate(Date date) { 
		if (date != null) 
			return sFormater.format(date);
		
		return null;
	}
	
	public static String formatTimeOnly(long time) { 
		if (time > 0) 
			return formatTimeOnly(new Date(time));
		
		return null;
	}
	
	public static String formatTimeOnly(Date date) { 
		if (date != null) 
			return sFormater10.format(date);
		
		return null;
	}
	
}
