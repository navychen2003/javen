package org.javenstudio.falcon.datum.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;

public class FileMetaTag implements Writable {
	private Text mTagName;
	private Text mTagValue;
	
	public FileMetaTag() {}
	
	public FileMetaTag(Text name, Text val) { 
		if (name == null || val == null) throw new NullPointerException();
		mTagName = name;
		mTagValue = val;
	}
	
	public Text getTagName() { return mTagName; }
	public Text getTagValue() { return mTagValue; }

	@Override
	public void write(DataOutput out) throws IOException {
		mTagName.write(out);
		mTagValue.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		mTagName = Text.read(in);
		mTagValue = Text.read(in);
	}
	
	public static FileMetaTag read(DataInput in) throws IOException { 
		FileMetaTag data = new FileMetaTag();
		data.readFields(in);
		return data;
	}
	
}
