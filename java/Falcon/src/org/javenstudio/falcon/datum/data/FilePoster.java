package org.javenstudio.falcon.datum.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;

public class FilePoster implements Writable {
	
	public static final int FLAG_NONE = 0;
	public static final int FLAG_SCREENSHOT = 1;
	
	private int mFlag;
	private Text[] mPosters;
	private Text[] mBackgrounds;
	
	public FilePoster() {}
	
	public FilePoster(int flag, Text[] pts, Text[] bgs) { 
		//if (pts == null) throw new NullPointerException();
		mFlag = flag;
		mPosters = pts;
		mBackgrounds = bgs;
	}
	
	public int getFlag() { return mFlag; }
	public void setFlag(int flag) { mFlag = flag; }
	
	public Text[] getPosters() { return mPosters; }
	public void setPosters(Text[] vals) { mPosters = vals; }
	
	public Text[] getBackgrounds() { return mBackgrounds; }
	public void setBackgrounds(Text[] vals) { mBackgrounds = vals; }

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(mFlag);
		writeArr(out, mPosters);
		writeArr(out, mBackgrounds);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		mFlag = in.readInt();
		mPosters = readArr(in);
		mBackgrounds = readArr(in);
	}
	
	static void writeArr(DataOutput out, Text[] values) throws IOException { 
		out.writeInt(values != null ? values.length : 0);
		if (values != null) {
			for (int i=0; i < values.length; i++) { 
				Text key = values[i];
				if (key == null) key = Text.EMPTY;
				key.write(out);
			}
		}
	}
	
	static Text[] readArr(DataInput in) throws IOException {
		int size = in.readInt();
		Text[] keys = new Text[size > 0 ? size : 0];
		for (int i=0; size > 0 && i < size; i++) { 
			keys[i] = Text.read(in);
		}
		return keys;
	}
	
	public static FilePoster read(DataInput in) throws IOException { 
		FilePoster data = new FilePoster();
		data.readFields(in);
		return data;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{flag=" + mFlag 
				+ ",posters={" + toString(mPosters) + "}" 
				+ ",backgrounds={" + toString(mBackgrounds) + "}}";
	}
	
	private static String toString(Text[] keys) { 
		StringBuilder sbuf = new StringBuilder();
		if (keys != null) { 
			for (Text key : keys) { 
				if (key == null) continue;
				if (sbuf.length() > 0) sbuf.append(',');
				sbuf.append(key.toString());
			}
		}
		return sbuf.toString();
	}
	
}
