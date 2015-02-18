package org.javenstudio.falcon.datum.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.datum.util.ImageUtils;
import org.javenstudio.falcon.datum.util.TimeUtils;
import org.javenstudio.raptor.io.Text;
import org.javenstudio.util.StringUtils;

public class FileMetaCollector {
	private static final Logger LOG = Logger.getLogger(FileMetaCollector.class);

	static class TagAction { 
		public final String mName;
		public final boolean mReplace;
		
		public TagAction(String name) { 
			this(name, false);
		}
		public TagAction(String name, boolean rep) { 
			if (name == null) throw new NullPointerException();
			mName = name;
			mReplace = rep;
		}
		public String toString(Object value) { 
			return value != null ? value.toString() : "";
		}
	}
	
	static class YearAction extends TagAction { 
		public YearAction(String name) { 
			super(name);
		}
		public YearAction(String name, boolean rep) { 
			super(name, rep);
		}
		@Override
		public String toString(Object value) { 
			String text = value != null ? value.toString() : "";
			long time = TimeUtils.parseTime(text);
			if (time > 0) { 
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(time);
				int year = cal.get(Calendar.YEAR);
				return Integer.toString(year);
			}
			return text;
		}
	}
	
	static class GenreAction extends TagAction { 
		public GenreAction(String name) { 
			super(name);
		}
		public GenreAction(String name, boolean rep) { 
			super(name, rep);
		}
		@Override
		public String toString(Object value) { 
			String text = value != null ? value.toString() : "";
			return StringUtils.trimChars(text, " \t\r\n\"\'[](){}<>,.;?:|/\\`~!@#$%^&*+=");
		}
	}
	
	private static final Map<String,TagAction> sTagActions = 
			new HashMap<String,TagAction>();
	
	private static void addAction(String name, TagAction action) { 
		if (name == null || action == null) return;
		sTagActions.put(name.toLowerCase(), action);
	}
	
	static { 
		addAction("author", new TagAction(FileMetaInfo.AUTHOR, true));
		addAction("meta:author", new TagAction(FileMetaInfo.AUTHOR));
		addAction("artist", new TagAction(FileMetaInfo.AUTHOR));
		addAction("creator", new TagAction(FileMetaInfo.AUTHOR));
		addAction("dc:creator", new TagAction(FileMetaInfo.AUTHOR));
		addAction("xmpdm:artist", new TagAction(FileMetaInfo.AUTHOR));
		
		addAction("title", new TagAction(FileMetaInfo.TITLE, true));
		addAction("dc:title", new TagAction(FileMetaInfo.TITLE));
		
		addAction("album", new TagAction(FileMetaInfo.ALBUM, true));
		addAction("xmpdm:album", new TagAction(FileMetaInfo.ALBUM));
		
		addAction("genre", new GenreAction(FileMetaInfo.GENRE));
		addAction("genrecustom", new GenreAction(FileMetaInfo.GENRE));
		addAction("xmpdm:genre", new GenreAction(FileMetaInfo.GENRE));
		addAction("year", new YearAction(FileMetaInfo.YEAR));
		addAction("xmpdm:releasedate", new YearAction(FileMetaInfo.YEAR));
	}
	
	private final List<FileMetaTag> mTags = new ArrayList<FileMetaTag>();
	private final Map<String,FileMetaInfo> mInfos = new HashMap<String,FileMetaInfo>();
	
	private final List<FileScreenshot> mShots = new ArrayList<FileScreenshot>();
	
	private final FileSource.Item mFile;
	
	public FileMetaCollector(FileSource.Item file) {
		if (file == null) throw new NullPointerException();
		mFile = file;
	}
	
	public synchronized void addScreenshot(FileScreenshot shot) { 
		if (shot == null) return;
		
		synchronized (mShots) { 
			for (FileScreenshot fs : mShots) { 
				if (fs == shot) return;
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("addScreenshot: name=" + shot.getName() 
						+ " mimeType=" + shot.getMimeType() + " size=" + shot.getSize() 
						+ " bufferSize=" + shot.getBufferSize());
			}
			
			mShots.add(shot);
		}
	}
	
	public synchronized void addScreenshot(String name, String mimeType, byte[] data) { 
		if (mimeType == null || mimeType.length() == 0 || 
			data == null || data.length == 0)
			return;
		
		if (name == null) name = "";
		int width = 0, height = 0;
		
		InputStream input = new ByteArrayInputStream(data);
		try { 
			if (input != null) {
				int[] sizes = ImageUtils.readImageSize(input);
				if (sizes != null && sizes.length >= 2) { 
					width = sizes[0];
					height = sizes[1];
				}
			}
		} catch (IOException e) { 
			if (LOG.isWarnEnabled()) {
				LOG.warn("addScreenshot: error: " + e, e);
			}
			//throw e;
		} finally { 
			try {
				if (input != null) input.close();
			} catch (Throwable e) { 
				// ignore
			}
		}
		
		synchronized (mShots) { 
			FileScreenshot shot = new FileScreenshot(name, mimeType, width, height, data);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("addScreenshot: name=" + shot.getName() 
						+ " mimeType=" + shot.getMimeType() 
						+ " width=" + shot.getWidth() + " height=" + shot.getHeight() 
						+ " bufferSize=" + shot.getBufferSize());
			}
			
			mShots.add(shot);
		}
	}
	
	public synchronized void addMetaTag(String name, Object value) { 
		if (name == null || value == null) return;
		
		synchronized (mTags) { 
			mTags.add(new FileMetaTag(new Text(name), new Text(value.toString())));
		}
		
		addMetaInfo(name, value);
	}
	
	private void addMetaInfo(String name, Object value) { 
		if (name == null || value == null) return;
		
		synchronized (mInfos) { 
			String tagname = name.toLowerCase();
			if (tagname.equalsIgnoreCase("duration"))
				setDuration(value, 1000);
			else if (tagname.equalsIgnoreCase("xmpDM:duration"))
				setDuration(value, 1);
			else if (tagname.equalsIgnoreCase("width"))
				setWidth(value);
			else if (tagname.equalsIgnoreCase("height"))
				setHeight(value);
			
			TagAction action = sTagActions.get(tagname);
			if (action != null) {
				FileMetaInfo existed = mInfos.get(action.mName);
				if (existed == null || action.mReplace) {
					String val = action.toString(value);
					name = action.mName;
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("addMetaInfo: name=" + name + " value=" + val 
								+ " orig=" + value);
					}
					
					mInfos.put(action.mName, new FileMetaInfo(
							new Text(name), new Text(val)));
				}
			}
		}
	}
	
	private void setWidth(Object value) { 
		if (value == null) return;
		
		try { 
			String str = StringUtils.trim(value.toString());
			double val = Double.parseDouble(str);
			if (LOG.isDebugEnabled())
				LOG.debug("setWidth: value=" + value + " width=" + val);
			
			if (val > 0 && mFile.getAttrs().getWidth() <= 0) 
				mFile.getAttrs().setWidth((int)val);
		} catch (Throwable e) { 
			// ignore
		}
	}
	
	private void setHeight(Object value) { 
		if (value == null) return;
		
		try { 
			String str = StringUtils.trim(value.toString());
			double val = Double.parseDouble(str);
			if (LOG.isDebugEnabled())
				LOG.debug("setHeight: value=" + value + " height=" + val);
			
			if (val > 0 && mFile.getAttrs().getHeight() <= 0) 
				mFile.getAttrs().setHeight((int)val);
		} catch (Throwable e) { 
			// ignore
		}
	}
	
	private void setDuration(Object value, int n) { 
		if (value == null) return;
		
		try { 
			String str = StringUtils.trim(value.toString());
			double val = Double.parseDouble(str);
			if (LOG.isDebugEnabled())
				LOG.debug("setDuration: value=" + value + " duration=" + val);
			
			if (val > 0 && mFile.getAttrs().getDuration() <= 0) {
				if (n > 0) val = val * n;
				mFile.getAttrs().setDuration((long)val);
			}
		} catch (Throwable e) { 
			// ignore
		}
	}
	
	public synchronized FileScreenshot[] getScreenshots() { 
		synchronized (mShots) { 
			return mShots.toArray(new FileScreenshot[mShots.size()]);
		}
	}
	
	public synchronized FileMetaTag[] getMetaTags() { 
		synchronized (mTags) { 
			return mTags.toArray(new FileMetaTag[mTags.size()]);
		}
	}
	
	public synchronized FileMetaInfo[] getMetaInfos() { 
		synchronized (mInfos) { 
			return mInfos.values().toArray(new FileMetaInfo[mInfos.size()]);
		}
	}
	
}
