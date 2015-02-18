package org.javenstudio.falcon.datum.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.Text;
import org.javenstudio.raptor.io.Writable;

public class FileAttr implements Writable {
	private static final Logger LOG = Logger.getLogger(FileAttr.class);

	@SuppressWarnings("rawtypes")
	private static final Map<Byte,AttrInfo> sAttrMap = 
			new HashMap<Byte,AttrInfo>();
	
	protected static final class TextAttr extends AttrInfo<Text> { 
		protected TextAttr(byte id, String name) { 
			super(id, name, Text.class);
		}

		@Override
		protected void writeData(DataOutput out, 
				Object data) throws IOException { 
			if (data == null) throw new NullPointerException();
			Text text = (Text)data;
			text.write(out);
		}
		
		@Override
		protected Attr<Text> readData(DataInput in) 
				throws IOException { 
			Text data = Text.read(in);
			return (Attr<Text>)newAttr(data);
		}
		
		@Override
		protected Text cloneData(Text data) { 
			return data != null ? new Text(data) : null;
		}
		
		@Override
		protected String toString(Text data) { 
			return data != null ? ("\"" + data.toString() + "\"") : "null";
		}
		
		@Override
		protected int getBufferSize(Text data) { 
			return data != null ? data.getLength() : 0;
		}
	}
	
	protected static final class IntAttr extends AttrInfo<Integer> { 
		protected IntAttr(byte id, String name) { 
			super(id, name, Integer.class);
		}

		@Override
		protected void writeData(DataOutput out, 
				Object data) throws IOException { 
			if (data == null) throw new NullPointerException();
			Integer num = (Integer)data;
			out.writeInt(num.intValue());
		}
		
		@Override
		protected Attr<Integer> readData(DataInput in) 
				throws IOException { 
			Integer data = in.readInt();
			return (Attr<Integer>)newAttr(data);
		}
		
		@Override
		protected Integer cloneData(Integer data) { 
			return data != null ? new Integer(data.intValue()) : null;
		}
		
		@Override
		protected String toString(Integer data) { 
			return data != null ? data.toString() : "null";
		}
		
		@Override
		protected int getBufferSize(Integer data) { 
			return data != null ? 4 : 0;
		}
	}
	
	protected static final class LongAttr extends AttrInfo<Long> { 
		protected LongAttr(byte id, String name) { 
			super(id, name, Long.class);
		}

		@Override
		protected void writeData(DataOutput out, 
				Object data) throws IOException { 
			if (data == null) throw new NullPointerException();
			Long num = (Long)data;
			out.writeLong(num.longValue());
		}
		
		@Override
		protected Attr<Long> readData(DataInput in) 
				throws IOException { 
			Long data = in.readLong();
			return (Attr<Long>)newAttr(data);
		}
		
		@Override
		protected Long cloneData(Long data) { 
			return data != null ? new Long(data.longValue()) : null;
		}
		
		@Override
		protected String toString(Long data) { 
			return data != null ? data.toString() : "null";
		}
		
		@Override
		protected int getBufferSize(Long data) { 
			return data != null ? 4 : 0;
		}
	}
	
	protected static abstract class AttrInfo<T> { 
		private final byte mId;
		private final String mName;
		private final Class<T> mClass;
		
		private AttrInfo(byte id, String name, Class<T> clazz) { 
			if (name == null || clazz == null) throw new NullPointerException();
			mId = id;
			mName = name;
			mClass = clazz;
			
			synchronized (sAttrMap) { 
				Byte key = new Byte(id);
				if (sAttrMap.containsKey(key)) {
					throw new IllegalArgumentException("Attr:0x" 
							+ Integer.toHexString(id) + " (" + name + ") already existed");
				}
				sAttrMap.put(key, this);
			}
		}
		
		public final byte getId() { return mId; }
		public final String getName() { return mName; }
		public final Class<T> getClazz() { return mClass; }
		
		@Override
		public final int hashCode() { 
			return Integer.hashCode(mId);
		}
		
		@Override
		public final boolean equals(Object obj) { 
			if (obj == this) return true;
			if (obj == null || !(obj instanceof AttrInfo)) return false;
			
			@SuppressWarnings("rawtypes")
			AttrInfo other = (AttrInfo)obj;
			return this.mId == other.mId;
		}
		
		public final Attr<T> newAttr(T data) { 
			if (data == null) throw new NullPointerException();
			return new Attr<T>(this, data);
		}
		
		@SuppressWarnings("unchecked")
		public final Attr<T> cloneAttr(Attr<?> attr) { 
			if (attr == null) throw new NullPointerException();
			if (attr.getInfo() != this) throw new IllegalArgumentException("Attr with wrong id");
			return newAttr(cloneData((T)attr.getData()));
		}
		
		public final void write(DataOutput out, Attr<?> attr)
				throws IOException {
			if (attr == null) throw new NullPointerException();
			if (attr.getInfo() != this) throw new IllegalArgumentException("Attr with wrong id");
			out.writeByte(getId());
			writeData(out, attr.getData());
		}
		
		protected abstract void writeData(DataOutput out, 
				Object data) throws IOException;
		
		protected abstract Attr<T> readData(DataInput in) 
				throws IOException;
		
		protected abstract String toString(T data);
		protected abstract int getBufferSize(T data);
		protected abstract T cloneData(T data);
		
		public static Attr<?> readAttr(DataInput in) throws IOException { 
			byte id = in.readByte();
			synchronized (sAttrMap) {
				AttrInfo<?> info = sAttrMap.get(id);
				if (info == null) 
					throw new IOException("Unknown attr id: 0x" + Integer.toHexString(id));
				return info.readData(in);
			}
		}
	}
	
	protected static final class Attr<T> implements Cloneable { 
		private final AttrInfo<T> mInfo;
		private final T mData;
		
		private Attr(AttrInfo<T> info, T data) { 
			if (info == null || data == null) throw new NullPointerException();
			mInfo = info;
			mData = data;
		}
		
		public AttrInfo<T> getInfo() { return mInfo; }
		public T getData() { return mData; }
		
		public String getString() { 
			return getInfo().toString(mData);
		}
		
		private int getBufferSize() { 
			return getInfo().getBufferSize(mData) + 1;
		}
		
		@Override
		public Attr<?> clone() { 
			return getInfo().cloneAttr(this);
		}
		
		@Override
		public String toString() { 
			return "Attr{id=0x" + Integer.toHexString(getInfo().getId()) 
					+ ",name=" + getInfo().getName() 
					+ ",data=" + getString() + "}";
		}
	}
	
	private static Comparator<Attr<?>> sComp = new Comparator<Attr<?>>() {
			@Override
			public int compare(Attr<?> o1, Attr<?> o2) {
				int id1 = (int)o1.getInfo().getId();
				int id2 = (int)o2.getInfo().getId();
				return id1 > id2 ? 1 : (id1 < id2 ? -1 : 0);
			}
		};
	
	private Attr<?>[] mAttrs = null;
	
	public FileAttr() {}
	
	public synchronized final int getBufferSize() { 
		if (mAttrs == null || mAttrs.length == 0) 
			return 0;
		
		int size = 0;
		
		for (int i=0; i < mAttrs.length; i++) { 
			Attr<?> a = mAttrs[i];
			size += a.getBufferSize();
		}
		
		return size;
	}
	
	public synchronized final void copyFrom(FileAttr from) { 
		if (from == null) return;
		
		synchronized (from) { 
			if (from.mAttrs == null || from.mAttrs.length == 0)
				return;
			
			for (int i=0; i < from.mAttrs.length; i++) { 
				Attr<?> a = from.mAttrs[i];
				setAttr(a.clone());
			}
		}
	}
	
	private synchronized final void setAttr(Attr<?> attr) { 
		if (attr == null) return;
		
		if (mAttrs == null) { 
			mAttrs = new Attr<?>[1];
			mAttrs[0] = attr;
			return;
		}
		
		for (int i=0; i < mAttrs.length; i++) { 
			Attr<?> a = mAttrs[i];
			if (a.getInfo().getId() == attr.getInfo().getId()) { 
				mAttrs[i] = attr;
				return;
			}
		}
		
		Attr<?>[] attrs = new Attr<?>[mAttrs.length+1];
		System.arraycopy(mAttrs, 0, attrs, 0, mAttrs.length);
		
		attrs[attrs.length-1] = attr;
		Arrays.sort(attrs, sComp);
		
		mAttrs = attrs;
	}
	
	private synchronized final Attr<?> removeAttr(AttrInfo<?> info) { 
		if (info == null || mAttrs == null || mAttrs.length == 0) 
			return null;
		
		ArrayList<Attr<?>> list = new ArrayList<Attr<?>>();
		Attr<?> removed = null;
		
		for (int i=0; i < mAttrs.length; i++) { 
			Attr<?> a = mAttrs[i];
			if (a.getInfo().getId() == info.getId()) { 
				removed = a;
				continue;
			} else { 
				list.add(a);
			}
		}
		
		if (removed != null) { 
			mAttrs = list.toArray(new Attr<?>[list.size()]);
			return removed;
		}
		
		return null;
	}
	
	private synchronized final Attr<?> getAttr(AttrInfo<?> info) { 
		if (info == null || mAttrs == null || mAttrs.length == 0) 
			return null;
		
		Attr<?>[] attrs = mAttrs;
		
		int low = 0;
		int high = attrs.length -1;
		int id = (int)info.getId();
		
		while (low <= high) { 
			int mid = (low + high) / 2;
			
			//if (LOG.isDebugEnabled()) {
			//	LOG.debug("getAttr: low=" + low + " mid=" + mid 
			//			+ " high=" + high + " length=" + attrs.length);
			//}
			
			Attr<?> attr = attrs[mid];
			int id2 = (int)attr.getInfo().getId();
			
			if (id2 == id) {
				//if (LOG.isDebugEnabled())
				//	LOG.debug("getAttr: found attrs[" + mid + "]=" + attr);
				
				return attr;
			
			} else if (id2 > id) { 
				high = mid - 1;
				
			} else { 
				low = mid + 1;
			}
		}
		
		for (int i=0; i < attrs.length; i++) { 
			Attr<?> a = attrs[i];
			
			if (a.getInfo().getId() == info.getId()) { 
				if (LOG.isDebugEnabled())
					LOG.debug("getAttr: WARNING attrs[" + i + "/" + attrs.length + "]=" + a);
				
				return a;
			}
		}
		
		return null;
	}

	@Override
	public synchronized final void write(DataOutput out) throws IOException {
		out.writeInt(mAttrs != null ? mAttrs.length : 0);
		for (int i=0; i < mAttrs.length; i++) { 
			Attr<?> attr = mAttrs[i];
			attr.getInfo().write(out, attr);
		}
	}

	@Override
	public synchronized final void readFields(DataInput in) throws IOException {
		Attr<?>[] attrs = null;
		int size = in.readInt();
		if (size > 0) { 
			attrs = new Attr<?>[size];
			for (int i=0; i < size; i++) { 
				Attr<?> attr = AttrInfo.readAttr(in);
				if (attr == null) throw new NullPointerException();
				attrs[i] = attr;
			}
		}
		mAttrs = attrs;
	}
	
	protected final String getString(TextAttr info) { 
		Text data = getText(info);
		return data != null ? data.toString() : null; 
	}
	
	protected final void setString(TextAttr info, String val) { 
		setText(info, val);
	}
	
	@SuppressWarnings("unchecked")
	protected final Text getText(TextAttr info) { 
		Attr<Text> attr = (Attr<Text>)getAttr(info);
		Text data = attr != null ? attr.getData() : null;
		return data != null ? data : new Text(); 
	}
	
	protected final void setText(TextAttr info, String val) { 
		if (val == null) { 
			removeAttr(info); 
		} else {
			Attr<Text> attr = info.newAttr(new Text(val));
			setAttr(attr);
		}
	}
	
	protected final void setText(TextAttr info, Text val) { 
		if (val == null) { 
			removeAttr(info); 
		} else {
			Attr<Text> attr = info.newAttr(new Text(val));
			setAttr(attr);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected final int getInt(IntAttr info, int def) { 
		Attr<Integer> attr = (Attr<Integer>)getAttr(info);
		Integer data = attr != null ? attr.getData() : null;
		return data != null ? data.intValue() : def;
	}
	
	protected final void setInt(IntAttr info, int val) { 
		Attr<Integer> attr = info.newAttr(val);
		setAttr(attr);
	}
	
	@SuppressWarnings("unchecked")
	protected final long getLong(LongAttr info, long def) { 
		Attr<Long> attr = (Attr<Long>)getAttr(info);
		Long data = attr != null ? attr.getData() : null;
		return data != null ? data.longValue() : def;
	}
	
	protected final void setLong(LongAttr info, long val) { 
		Attr<Long> attr = info.newAttr(val);
		setAttr(attr);
	}
	
	protected static String toStrings(Attr<?>[] attrs) { 
		StringBuilder sbuf = new StringBuilder();
		for (int i=0; attrs != null && i < attrs.length; i++) { 
			Attr<?> attr = attrs[i];
			if (sbuf.length() > 0) sbuf.append(',');
			if (attr == null) { 
				sbuf.append("null");
				continue;
			} else { 
				sbuf.append(attr.getInfo().getName());
				sbuf.append("(0x" + toHexString(attr.getInfo().getId()) + ")");
				sbuf.append('=');
				sbuf.append(attr.getString());
			}
		}
		return sbuf.toString();
	}
	
	protected static String toHexString(int num) { 
		String hex = Integer.toHexString(num);
		if (hex == null || hex.length() == 0) hex = "00";
		else if (hex.length() == 1) hex = "0" + hex;
		return hex;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{" + toStrings(mAttrs) + "}";
	}
	
}
