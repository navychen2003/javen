package org.javenstudio.falcon.user.auth;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserData;
import org.javenstudio.raptor.io.Text;

class UserBuffer extends Buffer implements IUserData {

	private Map<Text,Text> mAttrs = null;
	
	private int mFlag;
	private int mType;
	private Text mName;
	private Text mKey;
	private Text mHostKey;
	private byte[] mPassword;
	
	public UserBuffer() {}
	
	public UserBuffer(String name, String key, String hostkey, String pwd, 
			int flag, int type) throws ErrorException { 
		if (name == null || key == null || hostkey == null || pwd == null) 
			throw new NullPointerException();
		mFlag = flag;
		mType = type;
		mName = new Text(name);
		mKey = new Text(key);
		mHostKey = new Text(hostkey);
		mPassword = AuthHelper.encodePwd(pwd);
	}
	
	public int getFlag() { return mFlag; }
	public void setFlag(int flag) { mFlag = flag; }
	
	public int getType() { return mType; }
	public void setType(int type) { mType = type; }
	
	public Text getName() { return mName; }
	public Text getKey() { return mKey; }
	
	public byte[] getPassword() { return mPassword; }
	
	public void setPassword(String pwd) throws ErrorException { 
		if (pwd != null) mPassword = AuthHelper.encodePwd(pwd);
	}
	
	public String getNameAsString() { return mName != null ? mName.toString() : null; }
	public String getKeyAsString() { return mKey != null ? mKey.toString() : null; }
	
	public String getUserName() { return getNameAsString(); }
	public String getUserKey() { return getKeyAsString(); }
	
	public int getUserFlag() { return getFlag(); }
	//public void setUserFlag(int flag) { setFlag(flag); }
	
	public int getUserType() { return getType(); }
	
	public Text getHostKeyAsText() { return mHostKey; }
	public String getHostKey() { return mHostKey != null ? mHostKey.toString() : null; }
	public void setHostKey(String txt) { mHostKey = new Text(txt != null ? txt : ""); }
	
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
		return getClass().getSimpleName() + "{key=" + getKey() 
				+ ",name=" + getName() + ",hostkey=" + getHostKey() + ",flag=" + getFlag() 
				+ ",type=" + getType() + "}";
	}
	
	@Override
	public synchronized void write(DataOutput out) throws IOException {
		out.writeInt(mFlag);
		out.writeInt(mType);
		out.writeInt(mPassword != null ? mPassword.length : 0);
		out.write(mPassword);
		mName.write(out);
		mKey.write(out);
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
		mType = in.readInt();
		mPassword = new byte[in.readInt()];
		in.readFully(mPassword);
		mName = Text.read(in);
		mKey = Text.read(in);
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
	
	public static UserBuffer read(DataInput in) throws IOException { 
		UserBuffer data = new UserBuffer();
		data.readFields(in);
		return data;
	}
	
	public static UserBuffer decode(byte[] buffer, int offset, 
			int length) throws ErrorException { 
		UserBuffer data = new UserBuffer();
		readBuffer(data, buffer, offset, length);
		return data;
	}
	
}
