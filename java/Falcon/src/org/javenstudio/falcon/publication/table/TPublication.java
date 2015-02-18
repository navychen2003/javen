package org.javenstudio.falcon.publication.table;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.setting.ValueHelper;

final class TPublication extends ValueHelper implements IPublication {

	private final TPublicationService mService;
	private final String mId;
	
	private final Map<String,TNameValue<?>> mAttrs = new HashMap<String,TNameValue<?>>();
	private final Map<String,TNameValue<?>> mHeaders = new HashMap<String,TNameValue<?>>();
	private final Map<String,TNameValue<?>> mContents = new HashMap<String,TNameValue<?>>();
	
	public TPublication(TPublicationService service, String id) { 
		if (service == null || id == null) throw new NullPointerException();
		mService = service;
		mId = id;
	}
	
	public TPublicationService getService() { return mService; }
	public String getId() { return mId; }
	
	public int getAttrInt(String name) { return toInt(getAttr(name, Integer.class)); }
	public void setAttr(String name, int val) { setAttr(name, val, Integer.class); }
	
	public long getAttrLong(String name) { return toLong(getAttr(name, Long.class)); }
	public void setAttr(String name, long val) { setAttr(name, val, Long.class); }
	
	public float getAttrFloat(String name) { return toFloat(getAttr(name, Float.class)); }
	public void setAttr(String name, float val) { setAttr(name, val, Float.class); }
	
	public boolean getAttrBool(String name) { return toBool(getAttr(name, Boolean.class)); }
	public void setAttr(String name, boolean val) { setAttr(name, val, Boolean.class); }
	
	public byte[] getAttrBytes(String name) { return toBytes(getAttr(name, byte[].class)); }
	public void setAttr(String name, byte[] val) { setAttr(name, val, byte[].class); }
	
	public String getAttrString(String name) { return toString(getAttr(name, String.class)); }
	public void setAttr(String name, String val) { setAttr(name, val, String.class); }
	
	public TNameValue<?> getAttr(String name) {
		if (name != null) {
			synchronized (mAttrs) {
				TNameValue<?> nameVal = mAttrs.get(name);
				return nameVal;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getAttr(String name, Class<T> clazz) {
		if (name != null && clazz != null) {
			TNameValue<?> nameVal = getAttr(name);
			if (nameVal != null && nameVal.getValue() != null && 
				clazz.isAssignableFrom(nameVal.getValue().getClass())) {
				return (T)nameVal.getValue();
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> void setAttr(String name, T val, Class<T> clazz) {
		if (name != null) {
			synchronized (mAttrs) {
				T oldVal = null;
				TNameValue<?> old = mAttrs.get(name);
				if (old != null) oldVal = (T)old.getValue();
				if (val != null) {
					mAttrs.put(name, getService().newAttrValue(name, val, oldVal, clazz));
				} else {
					if (old != null) old.getType().checkRemove(getService(), oldVal);
					mAttrs.remove(name);
				}
			}
		}
	}
	
	public <T> void setAttr(TNameValue<T> val) {
		if (val != null) {
			synchronized (mAttrs) {
				final String name = val.getType().getName();
				TNameValue<?> old = mAttrs.get(name);
				getService().checkAttrValue(val, old);
				mAttrs.put(name, val);
			}
		}
	}
	
	public TNameValue<?>[] getAttrs() {
		synchronized (mAttrs) {
			return mAttrs.values().toArray(new TNameValue[mAttrs.size()]);
		}
	}
	
	public int getHeaderInt(String name) { return toInt(getHeader(name, Integer.class)); }
	public void setHeader(String name, int val) { setHeader(name, val, Integer.class); }
	
	public long getHeaderLong(String name) { return toLong(getHeader(name, Long.class)); }
	public void setHeader(String name, long val) { setHeader(name, val, Long.class); }
	
	public float getHeaderFloat(String name) { return toFloat(getHeader(name, Float.class)); }
	public void setHeader(String name, float val) { setHeader(name, val, Float.class); }
	
	public boolean getHeaderBool(String name) { return toBool(getHeader(name, Boolean.class)); }
	public void setHeader(String name, boolean val) { setHeader(name, val, Boolean.class); }
	
	public byte[] getHeaderBytes(String name) { return toBytes(getHeader(name, byte[].class)); }
	public void setHeader(String name, byte[] val) { setHeader(name, val, byte[].class); }
	
	public String getHeaderString(String name) { return toString(getHeader(name, String.class)); }
	public void setHeader(String name, String val) { setHeader(name, val, String.class); }
	
	public TNameValue<?> getHeader(String name) {
		if (name != null) {
			synchronized (mHeaders) {
				TNameValue<?> nameVal = mHeaders.get(name);
				return nameVal;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getHeader(String name, Class<T> clazz) {
		if (name != null && clazz != null) {
			TNameValue<?> nameVal = getHeader(name);
			if (nameVal != null && nameVal.getValue() != null && 
				clazz.isAssignableFrom(nameVal.getValue().getClass())) {
				return (T)nameVal.getValue();
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> void setHeader(String name, T val, Class<T> clazz) {
		if (name != null) {
			synchronized (mHeaders) {
				T oldVal = null;
				TNameValue<?> old = mHeaders.get(name);
				if (old != null) oldVal = (T)old.getValue();
				if (val != null) {
					mHeaders.put(name, getService().newHeaderValue(name, val, oldVal, clazz));
				} else {
					if (old != null) old.getType().checkRemove(getService(), oldVal);
					mHeaders.remove(name);
				}
			}
		}
	}
	
	public <T> void setHeader(TNameValue<T> val) {
		if (val != null) {
			synchronized (mHeaders) {
				final String name = val.getType().getName();
				TNameValue<?> old = mHeaders.get(name);
				getService().checkHeaderValue(val, old);
				mHeaders.put(name, val);
			}
		}
	}
	
	public TNameValue<?>[] getHeaders() {
		synchronized (mHeaders) {
			return mHeaders.values().toArray(new TNameValue[mHeaders.size()]);
		}
	}
	
	public int getContentInt(String name) { return toInt(getContent(name, Integer.class)); }
	public void setContent(String name, int val) { setContent(name, val, Integer.class); }
	
	public long getContentLong(String name) { return toLong(getContent(name, Long.class)); }
	public void setContent(String name, long val) { setContent(name, val, Long.class); }
	
	public float getContentFloat(String name) { return toFloat(getContent(name, Float.class)); }
	public void setContent(String name, float val) { setContent(name, val, Float.class); }
	
	public boolean getContentBool(String name) { return toBool(getContent(name, Boolean.class)); }
	public void setContent(String name, boolean val) { setContent(name, val, Boolean.class); }
	
	public byte[] getContentBytes(String name) { return toBytes(getContent(name, byte[].class)); }
	public void setContent(String name, byte[] val) { setContent(name, val, byte[].class); }
	
	public String getContentString(String name) { return toString(getContent(name, String.class)); }
	public void setContent(String name, String val) { setContent(name, val, String.class); }
	
	public TNameValue<?> getContent(String name) {
		if (name != null) {
			synchronized (mContents) {
				TNameValue<?> nameVal = mContents.get(name);
				return nameVal;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getContent(String name, Class<T> clazz) {
		if (name != null && clazz != null) {
			TNameValue<?> nameVal = getContent(name);
			if (nameVal != null && nameVal.getValue() != null && 
				clazz.isAssignableFrom(nameVal.getValue().getClass())) {
				return (T)nameVal.getValue();
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> void setContent(String name, T val, Class<T> clazz) {
		if (name != null) {
			synchronized (mContents) {
				T oldVal = null;
				TNameValue<?> old = mContents.get(name);
				if (old != null) oldVal = (T)old.getValue();
				if (val != null) {
					mContents.put(name, getService().newContentValue(name, val, oldVal, clazz));
				} else {
					if (old != null) old.getType().checkRemove(getService(), oldVal);
					mContents.remove(name);
				}
			}
		}
	}
	
	public <T> void setContent(TNameValue<T> val) {
		if (val != null) {
			synchronized (mContents) {
				final String name = val.getType().getName();
				TNameValue<?> old = mContents.get(name);
				getService().checkContentValue(val, old);
				mContents.put(name, val);
			}
		}
	}
	
	public TNameValue<?>[] getContents() {
		synchronized (mContents) {
			return mContents.values().toArray(new TNameValue[mContents.size()]);
		}
	}
	
	@Override
	public boolean equals(Object obj) { 
		if (obj == this) return true;
		if (obj == null || !(obj instanceof TPublication)) return false;
		
		TPublication other = (TPublication)obj;
		return this.getId().equals(other.getId());
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + mId 
				+ ",attrs=" + TNameValue.toString(getAttrs()) 
				+ ",headers=" + TNameValue.toString(getHeaders()) 
				+ ",contents=" + TNameValue.toString(getContents()) 
				+ "}";
	}
	
}
