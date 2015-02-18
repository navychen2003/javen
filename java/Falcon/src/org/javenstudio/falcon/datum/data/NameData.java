package org.javenstudio.falcon.datum.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;

public class NameData implements Writable { 
	private static final int VERSION = 1;
	
	public static final int MARK_HIDE = 1;
	//public static final int OPTIMIZE_MAPPED = 1;
	
	public static interface Collector { 
		public void addNameData(Text key, NameData data) throws ErrorException;
	}
	
	private Text mKey;
	private FileAttrs mAttrs;
	private FileShare mFileShare;
	private FilePoster mFilePoster;
	private Text[] mFileKeys;
	
	public NameData() {}
	
	public NameData(Text key, FileAttrs attrs, Text[] fileKeys) { 
		if (key == null) throw new NullPointerException();
		mKey = key;
		mAttrs = new FileAttrs();
		mFileShare = null;
		mFilePoster = null;
		mFileKeys = fileKeys != null ? fileKeys : new Text[0];
		
		mAttrs.copyFrom(attrs);
	}
	
	public Text getKey() { return mKey; }
	public FileAttrs getAttrs() { return mAttrs; }
	
	public boolean isDirectory() { 
		return SectionHelper.isDirectoryKey(getKey().toString()); 
	}
	
	public Text[] getFileKeys() { return mFileKeys; }
	public void setFileKeys(Text[] keys) { 
		if (keys == null) keys = new Text[0];
		mFileKeys = keys; 
	}
	public int getFileCount() { 
		return mFileKeys != null ? mFileKeys.length : 0; 
	}
	
	public FileShare getFileShare() { return mFileShare; }
	public void setFileShare(FileShare data) { mFileShare = data; }
	
	public FilePoster getFilePoster() { return mFilePoster; }
	public void setFilePoster(FilePoster data) { mFilePoster = data; }
	
	public int getBufferSize() { 
		int size = mKey.getLength() + mAttrs.getBufferSize(); 
		
		Text[] keys = mFileKeys;
		if (keys != null) { 
			for (Text key : keys) { 
				size += key != null ? key.getLength() : 0;
			}
		}
		
		return size;
	}
	
	@Override
	public synchronized void write(DataOutput out) throws IOException {
		out.writeInt(VERSION);
		
		mKey.write(out);
		mAttrs.write(out);
		
		out.writeInt(mFileKeys != null ? mFileKeys.length : 0);
		if (mFileKeys != null) {
			for (int i=0; i < mFileKeys.length; i++) { 
				Text fileKey = mFileKeys[i];
				if (fileKey == null) fileKey = Text.EMPTY;
				fileKey.write(out);
			}
		}
		
		out.writeBoolean(mFileShare != null);
		if (mFileShare != null)
			mFileShare.write(out);
		
		out.writeBoolean(mFilePoster != null);
		if (mFilePoster != null)
			mFilePoster.write(out);
	}

	@Override
	public synchronized void readFields(DataInput in) throws IOException {
		int version = in.readInt();
		if (version != VERSION) 
			throw new IOException("Version:" + version + " != " + VERSION);
		
		mKey = Text.read(in);
		mAttrs = FileAttrs.read(in);
		
		int size = in.readInt();
		Text[] fileKeys = new Text[size > 0 ? size : 0];
		for (int i=0; size > 0 && i < size; i++) { 
			fileKeys[i] = Text.read(in);
		}
		mFileKeys = fileKeys;

		if (in.readBoolean()) 
			mFileShare = FileShare.read(in);
		else
			mFileShare = null;
		
		if (in.readBoolean()) 
			mFilePoster = FilePoster.read(in);
		else
			mFilePoster = null;
	}
	
	public static NameData read(DataInput in) throws IOException { 
		NameData data = new NameData();
		data.readFields(in);
		return data;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + getKey() 
				+ ",attrs=" + getAttrs() + "}";
	}
	
}
