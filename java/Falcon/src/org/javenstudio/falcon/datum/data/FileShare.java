package org.javenstudio.falcon.datum.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;

public class FileShare implements Writable {
	
	private int mFlag;
	private long mTimeFrom;
	private long mTimeTo;
	private Text mAccessKey;
	private Text[] mUserKeys;
	
	public FileShare() {}
	
	public FileShare(int flag, long timeFrom, long timeTo, 
			Text accessKey, Text[] userKeys) { 
		//if (userKeys == null) throw new NullPointerException();
		mFlag = flag;
		mTimeFrom = timeFrom;
		mTimeTo = timeTo;
		mAccessKey = accessKey;
		mUserKeys = userKeys;
	}
	
	public int getFlag() { return mFlag; }
	public long getTimeFrom() { return mTimeFrom; }
	public long getTimeTo() { return mTimeTo; }
	
	public Text getAccessKey() { return mAccessKey; }
	public Text[] getUserKeys() { return mUserKeys; }

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(mFlag);
		out.writeLong(mTimeFrom);
		out.writeLong(mTimeTo);
		
		Text accessKey = mAccessKey;
		if (accessKey == null) accessKey = Text.EMPTY;
		accessKey.write(out);
		
		out.writeInt(mUserKeys != null ? mUserKeys.length : 0);
		if (mUserKeys != null) {
			for (int i=0; i < mUserKeys.length; i++) { 
				Text userKey = mUserKeys[i];
				if (userKey == null) userKey = Text.EMPTY;
				userKey.write(out);
			}
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		mFlag = in.readInt();
		mTimeFrom = in.readLong();
		mTimeTo = in.readLong();
		
		mAccessKey = Text.read(in);
		
		int size = in.readInt();
		Text[] userKeys = new Text[size > 0 ? size : 0];
		for (int i=0; size > 0 && i < size; i++) { 
			userKeys[i] = Text.read(in);
		}
		mUserKeys = userKeys;
	}
	
	public static FileShare read(DataInput in) throws IOException { 
		FileShare data = new FileShare();
		data.readFields(in);
		return data;
	}
	
}
