package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.util.StringUtils;

public abstract class FileIO {
	private static final Logger LOG = Logger.getLogger(FileIO.class);

	protected FileIO() { addItem(this); }
	
	public abstract SqRoot getRoot();
	
	public void close() throws ErrorException { 
		removeItem(this);
	}
	
	private static final ArrayList<FileIO> sItems = new ArrayList<FileIO>();
	
	private static void addItem(FileIO item) { 
		if (item == null) return;
		
		synchronized (sItems) { 
			boolean found = false;
			for (FileIO it : sItems) { 
				if (it == item) { 
					found = true; break;
				}
			}
			if (!found) { 
				sItems.add(item);
				
				if (LOG.isDebugEnabled())
					LOG.debug("addItem: item=" + item + " totalCount=" + sItems.size());
			}
		}
	}
	
	private static void removeItem(FileIO item) { 
		if (item == null) return;
		
		synchronized (sItems) { 
			boolean res = sItems.remove(item);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("removeItem: item=" + item + " res=" + res 
						+ " totalCount=" + sItems.size());
			}
		}
	}
	
	static int closeAll(SqRoot root) throws ErrorException { 
		int count = 0;
		
		synchronized (sItems) { 
			for (int i=0; i < sItems.size(); ) { 
				FileIO item = sItems.get(i);
				if (item != null && item.getRoot() == root) { 
					item.close();
					count ++;
					continue;
				}
				i ++;
			}
		}
		
		return count;
	}
	
	static void removeFiles(FileSystem fs, Path path) throws IOException {
		if (fs == null || path == null) return;
		
		FileStatus[] files = fs.listStatus(path);
		if (files != null) {
			for (FileStatus file : files) {
				if (file == null) continue;
				if (file.isDir()) {
					removeFiles(fs, file.getPath());
					
				} else {
					if (LOG.isInfoEnabled())
						LOG.info("removeFiles: delete file: " + path);
					
					fs.delete(file.getPath(), false);
				}
			}
		}
		
		if (LOG.isInfoEnabled())
			LOG.info("removeFiles: delete path: " + path);
		
		fs.delete(path, true);
	}
	
	static DigestInputStream openDigestInputStream(InputStream input) 
			throws IOException, ErrorException {
		try {
			return new DigestInputStream(input, MessageDigest.getInstance("MD5"));
		} catch (NoSuchAlgorithmException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	static String getDigestChecksum(DigestInputStream input) {
		MessageDigest digest = input.getMessageDigest();
		return digest.getAlgorithm() + ":" + StringUtils.byteToHexString(digest.digest());
	}
	
}
