package org.javenstudio.android.data;

import java.util.ArrayList;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.IdentityCache;
import org.javenstudio.common.util.Logger;

public final class DataPath implements Comparable<DataPath> {
	private static final Logger LOG = Logger.getLogger(DataPath.class);
    private static DataPath sRoot = new DataPath(null, "ROOT");

    private final long mIdentity = ResourceHelper.getIdentity();
    
    private final DataPath mParent;
    private final String mSegment;
    
    private IdentityCache<String, DataPath> mChildren = null;
    private DataObject mObject = null;
    private String mPathString = null;

    private DataPath(DataPath parent, String segment) {
        mParent = parent;
        mSegment = segment;
    }

    public final long getIdentity() { return mIdentity; }
    
    public DataPath getChild(String segment) {
        synchronized (DataPath.class) {
            if (mChildren == null) {
                mChildren = new IdentityCache<String, DataPath>();
            } else {
            	DataPath p = mChildren.get(segment);
                if (p != null) return p;
            }

            DataPath p = new DataPath(this, segment);
            mChildren.put(segment, p);
            return p;
        }
    }

    public DataPath getParent() {
        synchronized (DataPath.class) {
            return mParent;
        }
    }

    public DataPath getChild(int segment) {
        return getChild(String.valueOf(segment));
    }

    public DataPath getChild(long segment) {
        return getChild(String.valueOf(segment));
    }

    // TODO: toString() should be more efficient, will fix it later
    @Override
    public final String toString() {
        synchronized (DataPath.class) {
        	if (mPathString == null) {
	            StringBuilder sb = new StringBuilder();
	            String[] segments = split();
	            for (int i = 0; i < segments.length; i++) {
	                sb.append("/");
	                sb.append(segments[i]);
	            }
	            mPathString = sb.toString();
        	}
        	return mPathString;
        }
    }

	@Override
	public int compareTo(DataPath another) {
		return toString().compareTo(another.toString());
	}
    
	@Override
	public boolean equals(Object o) { 
		if (o == this) return true;
		if (o == null || !(o instanceof DataPath))
			return false;
		
		DataPath other = (DataPath)o;
		return this.compareTo(other) == 0;
	}
	
    public boolean equalsIgnoreCase(String p) {
        String path = toString();
        return path.equalsIgnoreCase(p);
    }

    void setObject(DataObject object) {
        synchronized (DataPath.class) {
            //if (LOG.isDebugEnabled() && mObject != null) { 
            //	LOG.debug("Object: " + mObject.getIdentity() + " path=" + mObject.getPath() 
            //			+ " not null, new object: " + (object != null ? ("" + object.getIdentity() 
            //			+ " path=" + object.getPath()) : "null"));
            //}
            
            mObject = object;
        }
    }

    public DataObject getObject() {
        synchronized (DataPath.class) {
            return mObject;
        }
    }
    
    public static DataPath fromString(String s) {
    	if (s == null) return null;
        synchronized (DataPath.class) {
            String[] segments = split(s);
            DataPath current = sRoot;
            for (int i = 0; i < segments.length; i++) {
                current = current.getChild(segments[i]);
            }
            return current;
        }
    }

    public String[] split() {
        synchronized (DataPath.class) {
            int n = 0;
            for (DataPath p = this; p != sRoot; p = p.mParent) {
                n++;
            }
            String[] segments = new String[n];
            int i = n - 1;
            for (DataPath p = this; p != sRoot; p = p.mParent) {
                segments[i--] = p.mSegment;
            }
            return segments;
        }
    }

    public static String[] split(String s) {
    	if (s == null) s = "";
        int n = s.length();
        if (n == 0) return new String[0];
        
        int pos = s.indexOf("://");
        if (pos >= 0) { 
        	s = s.substring(pos+2); 
        	n = s != null ? s.length() : 0;
        	if (n == 0) return new String[0];
        }
        
        if (s.charAt(0) != '/') 
            throw new RuntimeException("malformed path:" + s);
        
        ArrayList<String> segments = new ArrayList<String>();
        int i = 1;
        
        while (i < n) {
            int brace = 0;
            int j;
            
            for (j = i; j < n; j++) {
                char c = s.charAt(j);
                if (c == '{') ++brace;
                else if (c == '}') --brace;
                else if (brace == 0 && c == '/') break;
            }
            
            if (brace != 0) 
                throw new RuntimeException("unbalanced brace in path:" + s);
            
            segments.add(s.substring(i, j));
            i = j + 1;
        }
        
        String[] result = new String[segments.size()];
        segments.toArray(result);
        
        return result;
    }

    // Splits a string to an array of strings.
    // For example, "{foo,bar,baz}" -> {"foo","bar","baz"}.
    public static String[] splitSequence(String s) {
        int n = s.length();
        if (s.charAt(0) != '{' || s.charAt(n-1) != '}') 
            throw new RuntimeException("bad sequence: " + s);
        
        ArrayList<String> segments = new ArrayList<String>();
        int i = 1;
        
        while (i < n - 1) {
            int brace = 0;
            int j;
            
            for (j = i; j < n - 1; j++) {
                char c = s.charAt(j);
                if (c == '{') ++brace;
                else if (c == '}') --brace;
                else if (brace == 0 && c == ',') break;
            }
            
            if (brace != 0) 
                throw new RuntimeException("unbalanced brace in path:" + s);
            
            segments.add(s.substring(i, j));
            i = j + 1;
        }
        
        String[] result = new String[segments.size()];
        segments.toArray(result);
        
        return result;
    }

    public String getPrefix() {
        if (this == sRoot) return "";
        return getPrefixPath().mSegment;
    }

    public DataPath getPrefixPath() {
        synchronized (DataPath.class) {
        	DataPath current = this;
            if (current == sRoot) 
                throw new IllegalStateException();
            
            while (current.mParent != sRoot) {
                current = current.mParent;
            }
            
            return current;
        }
    }

    public String getSuffix() {
        // We don't need lock because mSegment is final.
        return mSegment;
    }

    // Below are for testing/debugging only
    static void clearAll() {
        synchronized (DataPath.class) {
            sRoot = new DataPath(null, "");
        }
    }

    static void dumpAll() {
        dumpAll(sRoot, "", "");
    }

    static void dumpAll(DataPath p, String prefix1, String prefix2) {
        synchronized (DataPath.class) {
        	DataObject obj = p.getObject();
        	if (LOG.isDebugEnabled()) {
            	LOG.debug(prefix1 + p.mSegment + ":"
            			+ (obj == null ? "null" : obj.getClass().getSimpleName()));
        	}
            
            if (p.mChildren != null) {
                ArrayList<String> childrenKeys = p.mChildren.keys();
                int i = 0, n = childrenKeys.size();
                
                for (String key : childrenKeys) {
                    DataPath child = p.mChildren.get(key);
                    if (child == null) {
                        ++i;
                        continue;
                    }
                    
                    if (LOG.isDebugEnabled())
                    	LOG.debug(prefix2 + "|");
                    
                    if (++i < n) {
                        dumpAll(child, prefix2 + "+-- ", prefix2 + "|   ");
                    } else {
                        dumpAll(child, prefix2 + "+-- ", prefix2 + "    ");
                    }
                }
            }
        }
    }
    
}
