package org.javenstudio.falcon.user.auth;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.INameData;
import org.javenstudio.raptor.io.Text;

class NameBuffer extends Buffer implements INameData {

	private Map<Text,Text> mAttrs = null;
	
	private int mFlag;
	private Text mName;
	private Text mValue;
	private Text mHostKey;
	
	public NameBuffer() {}
	
	public NameBuffer(String name, String value, String hostkey, int flag) { 
		if (name == null || value == null || hostkey == null) 
			throw new NullPointerException();
		mFlag = flag;
		mName = new Text(name);
		mValue = new Text(value);
		mHostKey = new Text(hostkey);
	}
	
	public int getFlag() { return mFlag; }
	public void setFlag(int flag) { mFlag = flag; }
	
	public Text getHostKeyAsText() { return mHostKey; }
	public String getHostKey() { return mHostKey != null ? mHostKey.toString() : null; }
	public void setHostKey(String txt) { mHostKey = new Text(txt != null ? txt : ""); }
	
	public Text getValue() { return mValue; }
	public String getValueAsString() { return mValue != null ? mValue.toString() : null; }
	public void setValue(String txt) { mValue = new Text(txt != null ? txt : ""); }
	
	public Text getName() { return mName; }
	public String getNameAsString() { return mName != null ? mName.toString() : null; }
	//public void setName(String txt) { mName = new Text(txt != null ? txt : ""); }
	
	public String getNameKey() { return getNameAsString(); }
	public String getNameValue() { return getValueAsString(); }
	public int getNameFlag() { return getFlag(); }
	
	public synchronized String[] getAttrNames() {
		if (mAttrs != null) {
			return mAttrs.keySet().toArray(new String[mAttrs.size()]);
		}
		return null;
	}
	
	public synchronized String getAttr(String name) {
		if (name != null && mAttrs != null) {
			Text val = mAttrs.get(new Text(name));
			if (val != null) return val.toString();
		}
		return null;
	}
	
	public synchronized void setAttr(String name, String value) {
		if (name == null || name.length() == 0)
			return;
		
		if (mAttrs == null) 
			mAttrs = new HashMap<Text,Text>();
		
		if (value != null && value.length() > 0)
			mAttrs.put(new Text(name), new Text(value));
		else
			mAttrs.remove(new Text(name));
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + getName() 
				+ ",value=" + getValue() + ",hostkey=" + getHostKey() 
				+ ",flag=" + getFlag() + "}";
	}
	
	@Override
	public synchronized void write(DataOutput out) throws IOException {
		out.writeInt(mFlag);
		mName.write(out);
		mValue.write(out);
		mHostKey.write(out);
		
		out.writeInt(mAttrs != null ? mAttrs.size() : 0);
		if (mAttrs != null) {
			for (Map.Entry<Text, Text> entry : mAttrs.entrySet()) {
				Text name = entry.getKey();
				Text value = entry.getValue();
				
				name.write(out);
				value.write(out);
			}
		}
	}

	@Override
	public synchronized void readFields(DataInput in) throws IOException {
		mFlag = in.readInt();
		mName = Text.read(in);
		mValue = Text.read(in);
		mHostKey = Text.read(in);
		
		int size = in.readInt();
		if (size > 0) {
			Map<Text,Text> map = new HashMap<Text,Text>();
			for (int i=0; i < size; i++) { 
				Text name = Text.read(in);
				Text value = Text.read(in);
				
				map.put(name, value);
			}
			mAttrs = map;
		} else {
			mAttrs = null;
		}
	}
	
	public static NameBuffer read(DataInput in) throws IOException { 
		NameBuffer data = new NameBuffer();
		data.readFields(in);
		return data;
	}
	
	public static NameBuffer decode(byte[] buffer, int offset, 
			int length) throws ErrorException { 
		NameBuffer data = new NameBuffer();
		readBuffer(data, buffer, offset, length);
		return data;
	}
	
}
