package org.javenstudio.cocoka.database;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.util.StringUtils;

public abstract class AbstractContent {

	private final boolean mUpdateable; 
	
	protected AbstractContent(boolean updateable) { 
		mUpdateable = updateable; 
	}
	
	public final boolean isUpdateable() { 
		return mUpdateable; 
	}
	
	protected void checkUpdateable() { 
		if (!mUpdateable) throw new DBException("This entity is not updateable"); 
	}
	
	protected String loadStreamAsString(SQLiteEntityDB.TEntity entity, String streamField) { 
		return loadStreamAsString(entity, streamField, "UTF-8"); 
	}
	
	protected String loadStreamAsString(SQLiteEntityDB.TEntity entity, String streamField, String encoding) { 
		if (entity == null || streamField == null) 
			return null; 
		
		InputStream is = null; 
		try { 
			is = entity.getAsStream(streamField); 
			if (is != null) { 
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding)); 
				StringBuilder sbuf = new StringBuilder(); 
				String line = null; 
				while ((line = reader.readLine()) != null) { 
					sbuf.append(line); 
					sbuf.append('\n'); 
				}
				return sbuf.toString(); 
			}
		} catch (Exception e) { 
			throw new DBException("stream field get error: "+e); 
		} finally { 
			try { if (is != null) is.close(); } 
			catch (Exception ex) {}
		}
		
		return null; 
	}
	
	protected void setStreamField(SQLiteEntityDB.TEntity entity, String streamField, String data) { 
		if (entity == null || streamField == null || data == null) 
			return; 
		
		try { 
			entity.setAsStream(streamField, 
					new ByteArrayInputStream(data.getBytes("UTF-8"))); 
		} catch (Exception e) { 
			throw new DBException("stream field set error: " +e); 
		}
	}
	
	protected boolean isFlagSet(Integer num, int flag) { 
		return (toInt(num) & flag) != 0;
	}
	
	protected int setFlag(Integer num, int flag, boolean set) { 
		final int oldFlags = toInt(num); 
    	final int mask = ~(oldFlags & flag); 
    	final int newFlags = set ? (oldFlags | flag) : (oldFlags & mask); 
    	return newFlags;
	}
	
	protected int toInt(Integer num) { 
		return num != null ? num.intValue() : 0; 
	}
	
	protected long toLong(Long num) { 
		return num != null ? num.longValue() : 0; 
	}
	
	protected String trim(String str) { 
		if (str == null) str = ""; 
		return StringUtils.trim(str); 
	}
	
}
