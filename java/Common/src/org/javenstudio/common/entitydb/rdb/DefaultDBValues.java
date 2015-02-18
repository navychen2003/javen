package org.javenstudio.common.entitydb.rdb;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultDBValues implements DBValues {

	private HashMap<String, Object> mValues;
	
	public DefaultDBValues() {
        mValues = new HashMap<String, Object>(8);
    }

    public DefaultDBValues(int size) {
        mValues = new HashMap<String, Object>(size, 1.0f);
    }
	
    public DefaultDBValues(DefaultDBValues from) {
        mValues = new HashMap<String, Object>(from.mValues);
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DefaultDBValues)) {
            return false;
        }
        return mValues.equals(((DefaultDBValues)object).mValues);
    }

    @Override
    public int hashCode() {
        return mValues.hashCode();
    }
    
    @Override
	public void put(String key, String value) {
        mValues.put(key, value);
    }
	
    @Override
	public void put(String key, Byte value) {
        mValues.put(key, value);
    }
	
    @Override
	public void put(String key, Short value) {
        mValues.put(key, value);
    }
	
    @Override
	public void put(String key, Integer value) {
        mValues.put(key, value);
    }
	
    @Override
	public void put(String key, Long value) {
        mValues.put(key, value);
    }
	
    @Override
	public void put(String key, Float value) {
        mValues.put(key, value);
    }
	
    @Override
	public void put(String key, Double value) {
        mValues.put(key, value);
    }
	
    @Override
	public void put(String key, Boolean value) {
        mValues.put(key, value);
    }
	
    @Override
	public void put(String key, byte[] value) {
        mValues.put(key, value);
    }
	
    @Override
	public void putNull(String key) {
        mValues.put(key, null);
    }
	
    @Override
	public int size() {
        return mValues.size();
    }
	
    @Override
	public void remove(String key) {
        mValues.remove(key);
    }
	
    @Override
	public void clear() {
        mValues.clear();
    }
	
    @Override
	public boolean containsKey(String key) {
        return mValues.containsKey(key);
    }
	
    @Override
	public String getAsString(String key) {
        Object value = mValues.get(key);
        return value != null ? value.toString() : null;
    }
	
    @Override
	public Long getAsLong(String key) {
        Object value = mValues.get(key);
        try {
            return value != null ? ((Number) value).longValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Long.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
	
    @Override
	public Integer getAsInteger(String key) {
        Object value = mValues.get(key);
        try {
            return value != null ? ((Number) value).intValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Integer.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
	
    @Override
	public Short getAsShort(String key) {
        Object value = mValues.get(key);
        try {
            return value != null ? ((Number) value).shortValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Short.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
	
    @Override
	public Byte getAsByte(String key) {
        Object value = mValues.get(key);
        try {
            return value != null ? ((Number) value).byteValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Byte.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
	
    @Override
	public Double getAsDouble(String key) {
        Object value = mValues.get(key);
        try {
            return value != null ? ((Number) value).doubleValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Double.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
	
    @Override
	public Float getAsFloat(String key) {
        Object value = mValues.get(key);
        try {
            return value != null ? ((Number) value).floatValue() : null;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                try {
                    return Float.valueOf(value.toString());
                } catch (NumberFormatException e2) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
	
    @Override
	public Boolean getAsBoolean(String key) {
        Object value = mValues.get(key);
        try {
            return (Boolean) value;
        } catch (ClassCastException e) {
            if (value instanceof CharSequence) {
                return Boolean.valueOf(value.toString());
            } else {
                return null;
            }
        }
    }
	
	@Override
	public byte[] getAsByteArray(String key) {
        Object value = mValues.get(key);
        if (value instanceof byte[]) {
            return (byte[]) value;
        } else {
            return null;
        }
    }
	
	@Override
	public Set<Map.Entry<String, Object>> valueSet() {
        return mValues.entrySet();
    }
	
	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String name : mValues.keySet()) {
            String value = getAsString(name);
            if (sb.length() > 0) sb.append(" ");
            sb.append(name + "=" + value);
        }
        return sb.toString();
    }
	
}
