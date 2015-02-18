package org.javenstudio.falcon.datum.data;

import java.io.DataInput;
import java.io.IOException;

import org.javenstudio.raptor.io.Text;

public class FileAttrs extends FileAttr {

	static TextAttr NAME 		 = new TextAttr((byte)0x01, "Name");
	static TextAttr EXTENSION 	 = new TextAttr((byte)0x02, "Extension");
	static TextAttr PATH 		 = new TextAttr((byte)0x03, "Path");
	static TextAttr PARENTKEY 	 = new TextAttr((byte)0x04, "ParentKey");
	static TextAttr OWNER 	 	 = new TextAttr((byte)0x05, "Owner");
	static TextAttr CHECKSUM 	 = new TextAttr((byte)0x06, "Checksum");
	
	static LongAttr LENGTH 		 = new LongAttr((byte)0x21, "Length");
	static LongAttr DURATION  	 = new LongAttr((byte)0x22, "Duration");
	static LongAttr MARKTIME 	 = new LongAttr((byte)0x23, "MarkTime");
	static LongAttr CREATEDTIME  = new LongAttr((byte)0x24, "CreatedTime");
	static LongAttr MODIFIEDTIME = new LongAttr((byte)0x25, "ModifiedTime");
	static LongAttr INDEXEDTIME  = new LongAttr((byte)0x26, "IndexedTime");
	
	static IntAttr  FILEINDEX  	 = new IntAttr( (byte)0x30, "FileIndex");
	static IntAttr  MARKFLAG  	 = new IntAttr( (byte)0x31, "MarkFlag");
	static IntAttr  POSTERCOUNT  = new IntAttr( (byte)0x32, "PosterCount");
	static IntAttr  WIDTH   	 = new IntAttr( (byte)0x33, "Width");
	static IntAttr  HEIGHT  	 = new IntAttr( (byte)0x34, "Height");
	
	public FileAttrs() {}
	
	public static FileAttrs read(DataInput in) throws IOException { 
		FileAttrs data = new FileAttrs();
		data.readFields(in);
		return data;
	}
	
	public Text getName() { return getText(NAME); }
	public void setName(Text val) { setText(NAME, val); }
	public void setName(String val) { setText(NAME, val); }
	
	public Text getExtension() { return getText(EXTENSION); }
	public void setExtension(Text val) { setText(EXTENSION, val); }
	public void setExtension(String val) { setText(EXTENSION, val); }
	
	public Text getPath() { return getText(PATH); }
	public void setPath(Text val) { setText(PATH, val); }
	public void setPath(String val) { setText(PATH, val); }
	
	public Text getParentKey() { return getText(PARENTKEY); }
	public void setParentKey(Text val) { setText(PARENTKEY, val); }
	public void setParentKey(String val) { setText(PARENTKEY, val); }
	
	public Text getOwner() { return getText(OWNER); }
	public void setOwner(Text val) { setText(OWNER, val); }
	public void setOwner(String val) { setText(OWNER, val); }
	
	public Text getChecksum() { return getText(CHECKSUM); }
	public void setChecksum(Text val) { setText(CHECKSUM, val); }
	public void setChecksum(String val) { setText(CHECKSUM, val); }
	
	
	public long getLength() { return getLong(LENGTH, 0); }
	public void setLength(long val) { setLong(LENGTH, val); }
	
	public long getDuration() { return getLong(DURATION, 0); }
	public void setDuration(long val) { setLong(DURATION, val); }
	
	public long getMarkTime() { return getLong(MARKTIME, 0); }
	public void setMarkTime(long val) { setLong(MARKTIME, val); }
	
	public long getCreatedTime() { return getLong(CREATEDTIME, 0); }
	public void setCreatedTime(long val) { setLong(CREATEDTIME, val); }
	
	public long getModifiedTime() { return getLong(MODIFIEDTIME, 0); }
	public void setModifiedTime(long val) { setLong(MODIFIEDTIME, val); }
	
	public long getIndexedTime() { return getLong(INDEXEDTIME, 0); }
	public void setIndexedTime(long val) { setLong(INDEXEDTIME, val); }
	
	
	public int getFileIndex() { return getInt(FILEINDEX, 0); }
	public void setFileIndex(int val) { setInt(FILEINDEX, val); }
	
	public int getMarkFlag() { return getInt(MARKFLAG, 0); }
	public void setMarkFlag(int val) { setInt(MARKFLAG, val); }
	
	public int getPosterCount() { return getInt(POSTERCOUNT, 0); }
	public void setPosterCount(int val) { setInt(POSTERCOUNT, val); }
	
	public int getWidth() { return getInt(WIDTH, 0); }
	public void setWidth(int val) { setInt(WIDTH, val); }
	
	public int getHeight() { return getInt(HEIGHT, 0); }
	public void setHeight(int val) { setInt(HEIGHT, val); }
	
}
