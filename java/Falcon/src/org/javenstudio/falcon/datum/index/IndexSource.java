package org.javenstudio.falcon.datum.index;

import java.io.IOException;
import java.util.LinkedList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataJob;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.DataSource;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionRoot;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.job.JobContext;

public class IndexSource implements DataSource {
	private static final Logger LOG = Logger.getLogger(IndexSource.class);

	private final DataManager mManager;
	
	private final LinkedList<ILibrary> mLibraries = 
			new LinkedList<ILibrary>();
	
	private ILibrary mCurrentLibrary = null;
	
	public IndexSource(DataManager manager) { 
		if (manager == null) throw new NullPointerException();
		mManager = manager;
	}
	
	public DataManager getManager() { 
		return mManager;
	}
	
	@Override
	public IUser getUser() {
		return getManager().getUser();
	}
	
	public void addLibrary(ILibrary... libs) { 
		if (libs == null || libs.length == 0)
			return;
		
		synchronized (mLibraries) { 
			for (ILibrary lib : libs) { 
				if (lib == null) continue;
				if (lib.getManager() != getManager()) { 
					throw new IllegalArgumentException("Library: " + lib.getName() 
							+ " cannot be added with wrong manager");
				}
				
				if (lib == mCurrentLibrary) { 
					if (LOG.isDebugEnabled())
						LOG.debug("addLibrary: library: " + lib + " already indexing");
					continue;
				}
				
				boolean found = false;
				for (ILibrary library : mLibraries) { 
					if (library == lib) { 
						if (LOG.isDebugEnabled())
							LOG.debug("addLibrary: library: " + lib + " already existed");
						
						found = true; 
						break;
					}
				}
				
				if (!found) mLibraries.add(lib);
			}
		}
	}
	
	private ILibrary popLibrary() { 
		synchronized (mLibraries) { 
			if (mLibraries.size() > 0)
				return mLibraries.removeFirst();
			return null;
		}
	}
	
	@Override
	public String getMessage() {
		ILibrary library = mCurrentLibrary;
		if (library != null) {
			String lang = null;
			try { 
				lang = library.getManager().getUser().getPreference().getLanguage();
			} catch (Throwable e) { 
				lang = null;
			}
			String msg = "Indexing library \"%1$s\".";
			return String.format(Strings.get(lang, msg), library.getName());
		}
		return null;
	}

	@Override
	public void process(DataJob job, JobContext jc, Collector collector) 
			throws IOException, ErrorException {
		ILibrary library = null;
		try {
			while ((library = popLibrary()) != null) { 
				mCurrentLibrary = library;
				
				if (LOG.isDebugEnabled())
					LOG.debug("process: library: " + library + " indexing");
				
				long indexTime = library.getIndexedTime();
				long current = System.currentTimeMillis();
				
				library.getLock().lock(ILockable.Type.READ, null);
				try { 
					for (int i=0; i < library.getSectionCount(); i++) { 
						ISectionRoot root = library.getSectionAt(i);
						if (root == null) continue;
						
						root.getLock().lock(ILockable.Type.READ, null);
						try { 
							indexRoot(root, indexTime);
						} finally { 
							root.getLock().unlock(ILockable.Type.READ);
						}
					}
					
					library.onIndexed(current);
				} finally { 
					library.getLock().unlock(ILockable.Type.READ);
				}
			}
		} finally { 
			mCurrentLibrary = null;
			getManager().getIndexer().commit();
		}
	}

	private void indexRoot(final ISectionRoot root, final long indexTime) 
			throws IOException, ErrorException { 
		if (root == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("indexRoot: root: " + root + " indexing");
		
		root.listSection(new ISection.Collector() {
				@Override
				public void addSection(ISection section) throws ErrorException {
					if (section == null) return;
					if (section.isFolder() || section.getIndexedTime() < indexTime)
						return;
					
					indexFile(section);
				}
			});
	}
	
	private void indexFile(ISection section) throws ErrorException { 
		if (section == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("indexFile: file: " + section + " indexing");
		
		try {
			getManager().getIndexer().addDoc(new IndexDoc(section));
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	@Override
	public void close() {
	}

}
