package org.javenstudio.falcon.datum.data;

import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.io.Text;

public final class SqRootPoster {
	private static final Logger LOG = Logger.getLogger(SqRootPoster.class);

	public static boolean setPoster(SqRoot root, 
			String[] posters) throws ErrorException { 
		if (root == null) throw new NullPointerException();
		return setPosterBackground(root, posters, root.getBackgrounds());
	}
	
	public static boolean setBackground(SqRoot root, 
			String[] backgrounds) throws ErrorException { 
		if (root == null) throw new NullPointerException();
		return setPosterBackground(root, root.getPosters(), backgrounds);
	}
	
	public static boolean setPosterBackground(SqRoot root, 
			String[] posters, String[] backgrounds) throws ErrorException { 
		if (root == null) throw new NullPointerException();
		
		final String key = root.getPathKey();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return doSavePoster(root, key, posters, backgrounds, false);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static boolean setPoster(SqRootDir dir, 
			String[] posters) throws ErrorException { 
		if (dir == null) throw new NullPointerException();
		return setPosterBackground(dir, posters, dir.getBackgrounds());
	}
	
	public static boolean setBackground(SqRootDir dir, 
			String[] backgrounds) throws ErrorException { 
		if (dir == null) throw new NullPointerException();
		return setPosterBackground(dir, dir.getPosters(), backgrounds);
	}
	
	public static boolean setPosterBackground(SqRootDir dir, 
			String[] posters, String[] backgrounds) throws ErrorException { 
		if (dir == null) throw new NullPointerException();
		
		final SqRoot root = dir.getRoot();
		final String key = dir.getPathKey();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return doSavePoster(root, key, posters, backgrounds, false);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	public static boolean setPoster(SqRootFile file, 
			String[] posters) throws ErrorException { 
		if (file == null) throw new NullPointerException();
		return setPosterBackground(file, posters, file.getBackgrounds());
	}
	
	public static boolean setBackground(SqRootFile file, 
			String[] backgrounds) throws ErrorException { 
		if (file == null) throw new NullPointerException();
		return setPosterBackground(file, file.getPosters(), backgrounds);
	}
	
	public static boolean setPosterBackground(SqRootFile file, 
			String[] posters, String[] backgrounds) throws ErrorException { 
		if (file == null) throw new NullPointerException();
		
		final SqRoot root = file.getRoot();
		final String key = file.getPathKey();
		
		//synchronized (root.getLock()) { 
			root.getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
			try { 
				return doSavePoster(root, key, posters, backgrounds, false);
			} finally { 
				root.getLock().unlock(ILockable.Type.WRITE);
			}
		//}
	}
	
	private static boolean doSavePoster(final SqRoot root, 
			final String key, final String[] posters, final String[] backgrounds, 
			final boolean append) throws ErrorException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("doSavePoster: key=" + key + " append=" + append);
		
		final SqRootNames names = new SqRootNames();
		
		ErrorException exception = null;
		boolean success = false;
		
		try { 
			final Text keyText = new Text(key);
			names.reloadNames(root);
			
			NameData data = names.getNameData(keyText);
			if (data == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"File key: " + key + " not found");
			}
			
			Text[] posterKeys = toTextArray(posters);
			Text[] backgroundKeys = toTextArray(backgrounds);
			
			names.setPoster(data, posterKeys, backgroundKeys, append);
			
			if (LOG.isDebugEnabled())
				LOG.debug("doSavePoster: key=" + key + " poster=" + data.getFilePoster());
			
			success = true;
		} catch (ErrorException ee) {
			if (exception == null) {
				exception = ee;
			}// else if (LOG.isErrorEnabled()) {
			//	LOG.error(ee.toString(), ee);
			//}
		} finally { 
			boolean updated = false;
			try {
				if (success) {
					FileStorer storer = root.newStorer();
					names.storeNames(storer);
					storer.close(); // flush
					updated = true;
				}
			//} catch (IOException ee) { 
			//	if (exception == null) {
			//		exception = new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ee);
			//	} else if (LOG.isErrorEnabled()) {
			//		LOG.error(ee.toString(), ee);
			//	}
			} catch (ErrorException ee) { 
				if (exception == null) {
					exception = ee;
				} else if (LOG.isErrorEnabled()) {
					LOG.error(ee.toString(), ee);
				}
			} finally { 
				if (updated) {
					try { 
						root.reset();
					} catch (ErrorException ee) { 
						if (exception == null) {
							exception = ee;
						} else if (LOG.isErrorEnabled()) {
							LOG.error(ee.toString(), ee);
						}
					}
				}
			}
		}
		
		if (exception != null)
			throw exception;
		
		return success;
	}
	
	private static Text[] toTextArray(final String[] values) { 
		ArrayList<Text> list = new ArrayList<Text>();
		if (values != null) { 
			for (String value : values) { 
				if (value != null && value.length() > 0)
					list.add(new Text(value));
			}
		}
		
		if (list.size() == 0) return null;
		return list.toArray(new Text[list.size()]);
	}
	
}
