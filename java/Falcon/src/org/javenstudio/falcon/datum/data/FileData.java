package org.javenstudio.falcon.datum.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.io.BytesWritable;
import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;

public class FileData implements Writable { 
	private static final int VERSION = 1;
	
	public static interface Collector { 
		public void addFileData(Text key, FileData data) throws ErrorException;
	}
	
	private Text mFileKey;
	private FileAttrs mAttrs;
	private FileMetaTag[] mMetaTags;
	private FileMetaInfo[] mMetaInfos;
	private FileScreenshot[] mScreenshots;
	private BytesWritable mFileData;
	
	public FileData() {}
	
	public FileData(Text fileKey, FileAttrs attrs, 
			FileMetaTag[] metadata, FileMetaInfo[] metainfo, 
			FileScreenshot[] shots, byte[] data) { 
		if (fileKey == null || data == null || attrs == null) 
			throw new NullPointerException();
		mFileKey = fileKey;
		mAttrs = new FileAttrs();
		mMetaTags = metadata;
		mMetaInfos = metainfo;
		mScreenshots = shots;
		mFileData = new BytesWritable(data);
		
		mAttrs.copyFrom(attrs);
	}
	
	public Text getFileKey() { return mFileKey; }
	public FileAttrs getAttrs() { return mAttrs; }
	
	public FileMetaTag[] getMetaTags() { return mMetaTags; }
	public FileMetaInfo[] getMetaInfos() { return mMetaInfos; }
	public FileScreenshot[] getScreenshots() { return mScreenshots; }
	public BytesWritable getFileData() { return mFileData; }

	//public void setMetaTags(FileMetaTag[] metas) { mMetaTags = metas; }
	public void setMetaInfos(FileMetaInfo[] metas) { mMetaInfos = metas; }
	//public void setScreenshots(FileScreenshot[] shots) { mScreenshots = shots; }
	
	public int getScreenshotCount() { 
		FileScreenshot[] shots = mScreenshots;
		return shots != null ? shots.length : 0;
	}
	
	public int getMetaTagCount() { 
		FileMetaTag[] tags = mMetaTags;
		return tags != null ? tags.length : 0;
	}
	
	public int getMetaInfoCount() { 
		FileMetaInfo[] infos = mMetaInfos;
		return infos != null ? infos.length : 0;
	}
	
	public long getBufferSize() { 
		long size = mFileData != null ? mFileData.getLength() : 0;
		
		size += mAttrs.getBufferSize();
		
		FileScreenshot[] shots = mScreenshots;
		if (shots != null && shots.length > 0) { 
			for (FileScreenshot shot : shots) { 
				if (shot != null) 
					size += shot.getBufferSize();
			}
		}
		
		FileMetaTag[] tags = mMetaTags;
		if (tags != null && tags.length > 0) { 
			for (FileMetaTag tag : tags) { 
				if (tag != null) { 
					size += tag.getTagName().getLength() 
						  + tag.getTagValue().getLength();
				}
			}
		}
		
		FileMetaInfo[] infos = mMetaInfos;
		if (infos != null && infos.length > 0) { 
			for (FileMetaInfo info : infos) { 
				if (info != null) { 
					size += info.getName().getLength() 
						  + info.getValue().getLength();
				}
			}
		}
		
		return size;
	}
	
	public boolean hasFileData() { 
		return mFileData != null && mFileData.getLength() == getAttrs().getLength();
	}
	
	@Override
	public synchronized void write(DataOutput out) throws IOException {
		out.writeInt(VERSION);
		
		mFileKey.write(out);
		mAttrs.write(out);
		
		out.writeInt(mMetaTags != null ? mMetaTags.length : 0);
		if (mMetaTags != null) { 
			for (FileMetaTag tag : mMetaTags) { 
				tag.write(out);
			}
		}
		
		out.writeInt(mMetaInfos != null ? mMetaInfos.length : 0);
		if (mMetaInfos != null) { 
			for (FileMetaInfo info : mMetaInfos) { 
				info.write(out);
			}
		}
		
		out.writeInt(mScreenshots != null ? mScreenshots.length : 0);
		if (mScreenshots != null) { 
			for (FileScreenshot data : mScreenshots) { 
				data.write(out);
			}
		}
		
		mFileData.write(out);
	}

	@Override
	public synchronized void readFields(DataInput in) throws IOException {
		int version = in.readInt();
		if (version != VERSION) 
			throw new IOException("Version:" + version + " != " + VERSION);
		
		mFileKey = Text.read(in);
		mAttrs = FileAttrs.read(in);
		
		int tagCount = in.readInt();
		if (tagCount > 0) { 
			FileMetaTag[] tags = new FileMetaTag[tagCount];
			for (int i=0; i < tagCount; i++) { 
				tags[i] = FileMetaTag.read(in);
			}
			mMetaTags = tags;
		} else
			mMetaTags = new FileMetaTag[0];
		
		int infoCount = in.readInt();
		if (infoCount > 0) { 
			FileMetaInfo[] infos = new FileMetaInfo[infoCount];
			for (int i=0; i < infoCount; i++) { 
				infos[i] = FileMetaInfo.read(in);
			}
			mMetaInfos = infos;
		} else
			mMetaInfos = new FileMetaInfo[0];
		
		int shotCount = in.readInt();
		if (shotCount > 0) { 
			FileScreenshot[] shots = new FileScreenshot[shotCount];
			for (int i=0; i < shotCount; i++) { 
				shots[i] = FileScreenshot.read(in);
			}
			mScreenshots = shots;
		} else 
			mScreenshots = new FileScreenshot[0];
		
		BytesWritable data = new BytesWritable();
		data.readFields(in);
		mFileData = data;
	}
	
	public static FileData read(DataInput in) throws IOException { 
		FileData data = new FileData();
		data.readFields(in);
		return data;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + getFileKey() 
				+ ",attrs=" + getAttrs() + "}";
	}
	
}
