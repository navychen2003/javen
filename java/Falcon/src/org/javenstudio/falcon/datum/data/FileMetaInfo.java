package org.javenstudio.falcon.datum.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;

public class FileMetaInfo implements Writable {
	
	public static final String AUTHOR = "author";
	public static final String TITLE = "title";
	public static final String SUBTITLE = "subtitle";
	public static final String SUMMARY = "summary";
	public static final String ALBUM = "album";
	public static final String GENRE = "genre";
	public static final String YEAR = "year";
	public static final String TAGS = "tags";
	
	private Text mName;
	private Text mValue;
	
	public FileMetaInfo() {}
	
	public FileMetaInfo(Text name, Text val) { 
		if (name == null || val == null) throw new NullPointerException();
		mName = name;
		mValue = val;
	}
	
	public Text getName() { return mName; }
	public Text getValue() { return mValue; }

	@Override
	public void write(DataOutput out) throws IOException {
		mName.write(out);
		mValue.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		mName = Text.read(in);
		mValue = Text.read(in);
	}
	
	public static FileMetaInfo read(DataInput in) throws IOException { 
		FileMetaInfo data = new FileMetaInfo();
		data.readFields(in);
		return data;
	}
	
}
