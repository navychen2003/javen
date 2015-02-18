package org.javenstudio.falcon.setting.cluster;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.common.util.Logger;

final class AnyboxLibrary {
	private static final Logger LOG = Logger.getLogger(AnyboxLibrary.class);

	static class LibraryData implements ILibraryInfo {
		private final String mId;
		private String mName;
		private String mHostName;
		private String mType;
		private String mPoster;
		private String mBackground;
		private long mCtime;
		private long mMtime;
		private long mItime;
		private int mSubCount;
		private long mSubLen;
		
		public LibraryData(String id) {
			if (id == null) throw new NullPointerException();
			mId = id;
		}
		
		public String getContentId() { return mId; }
		public String getName() { return mName; }
		public String getHostName() { return mHostName; }
		public String getContentType() { return mType; }
		public String getPoster() { return mPoster; }
		public String getBackground() { return mBackground; }
		public long getCreatedTime() { return mCtime; }
		public long getModifiedTime() { return mMtime; }
		public long getIndexedTime() { return mItime; }
		public int getSubCount() { return mSubCount; }
		public long getSubLen() { return mSubLen; }
	}
	
	static LibraryData[] loadLibraries(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		String[] names = data.getNames();
		ArrayList<LibraryData> list = new ArrayList<LibraryData>();
		
		if (names != null) {
			for (String name : names) {
				LibraryData lib = loadLibrary(data.get(name));
				if (lib != null) list.add(lib);
			}
		}
		
		return list.toArray(new LibraryData[list.size()]);
	}
	
	static LibraryData loadLibrary(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadLibrary: data=" + data);
		
		String id = data.getString("id");
		if (id == null) return null;
		
		LibraryData lib = new LibraryData(id);
		lib.mName = data.getString("name");
		lib.mHostName = data.getString("hostname");
		lib.mType = data.getString("type");
		lib.mPoster = data.getString("poster");
		lib.mBackground = data.getString("background");
		lib.mCtime = data.getLong("ctime", 0);
		lib.mMtime = data.getLong("mtime", 0);
		lib.mItime = data.getLong("itime", 0);
		lib.mSubCount = data.getInt("subcount", 0);
		lib.mSubLen = data.getLong("sublen", 0);
		
		return lib;
	}
	
}
