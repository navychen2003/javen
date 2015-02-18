package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.ISectionSet;
import org.javenstudio.falcon.datum.SectionQuery;
import org.javenstudio.raptor.io.Text;

public final class SqRootOptimize {
	private static final Logger LOG = Logger.getLogger(SqRootOptimize.class);

	private static class CountCollector implements SqRootNames.Collector { 
		private String mRootKey = null;
		
		private int mFolderCount = 0;
		private int mFileCount = 0;
		private long mFileLength = 0;
		
		@Override
		public void add(Text key, NameData data) throws ErrorException {
			if (key != null && data != null) { 
				String fileKey = key.toString();
				
				if (mRootKey != null && fileKey.equals(mRootKey)) 
					return;
				
				if (data.isDirectory()) { 
					mFolderCount ++;
				} else { 
					mFileCount ++;
					mFileLength += data.getAttrs().getLength();
				}
			}
		}
	}
	
	static void saveCounts(SqRoot root, SqRootNames names, 
			FileStorer storer) throws IOException, ErrorException { 
		CountCollector counts = new CountCollector();
		counts.mRootKey = root.getPathKey();
		
		names.getNameDatas(counts);
		
		int folderCount = counts.mFolderCount;
		int fileCount = counts.mFileCount;
		long fileLength = counts.mFileLength;
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("saveCounts: root=" + root + " folderCount=" + folderCount 
					+ " fileCount=" + fileCount + " fileLength=" + fileLength);
		}
		
		root.setModifiedTime(System.currentTimeMillis());
		root.setTotalFolderCount(folderCount);
		root.setTotalFileCount(fileCount);
		root.setTotalFileLength(fileLength);
		
		root.getLibrary().getManager().saveLibraryList();
	}
	
	static int optimizeFiles(SqRoot root, SqRootNames names, 
			FileStorer storer) throws IOException, ErrorException { 
		long current = System.currentTimeMillis();
		if (current - root.getOptimizedTime() < 10 * 60 * 1000)
			return 0;
		
		final ArrayList<NameData> files = new ArrayList<NameData>();
		
		names.getNameDatas(new SqRootNames.Collector() {
				@Override
				public void add(Text key, NameData data) throws ErrorException {
					if (key != null && data != null) { 
						if (!data.isDirectory() && data.getAttrs().getFileIndex() == 0 && 
							data.getAttrs().getLength() < SqFileSource.MAPFILE_MAXLEN) { 
							files.add(data);
						}
					}
				}
			});
		
		if (LOG.isDebugEnabled())
			LOG.debug("optimizeFiles: root=" + root + " fileCount=" + files.size());
		
		if (files.size() < 10) return 0;
		
		final FileLoader loader = root.newLoader();
		final Set<String> removes = new HashSet<String>();
		int count = 0;
		
		for (NameData nameData : files) { 
			Text key = nameData.getKey();
			if (key == null) continue;
			
			int fileIndex = nameData.getAttrs().getFileIndex();
			String pathKey = key.toString();
			FileData fileData = loader.loadFileData(fileIndex, pathKey);
			
			if (fileData != null && fileData.hasFileData()) { 
				storer.storeFile(key, fileData);
				nameData.getAttrs().setFileIndex(fileData.getAttrs().getFileIndex());
				removes.add(pathKey);
				count ++;
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("optimizeFiles: add file: key=" + key + " attrs="
							+ fileData.getAttrs() + " to fileMap");
				}
			}
		}
		
		if (count > 0) { 
			storer.closeFiles();
			root.setOptimizeTime(current);
			
			if (LOG.isInfoEnabled()) 
				LOG.info("optimizeFiles: removes.size=" + removes.size());
			
			for (String fileKey : removes) { 
				storer.removeFsFile(fileKey);
			}
		}
		
		loader.close();
		
		return count;
	}
	
	static int doListItems(final SqRoot root, 
			final SqSectionDir dir, final Collection<SqSection> items, 
			boolean includeSub) throws IOException, ErrorException { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("doListItems: root=" + root + " folder=" + dir 
					+ " includeSub=" + includeSub);
		}
		
		ISectionSet set = dir.getSubSections(new SectionQuery(0, 0));
		int count = 0;
		
		if (set != null && set.getSectionCount() > 0) { 
			for (int i=0; i < set.getSectionCount(); i++) { 
				SqSection section = (SqSection)set.getSectionAt(i);
				if (section == null) continue;
				
				if (section instanceof SqRoot) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Item: " + section.getName() + " is root");
					
				} else if (section instanceof SqSectionDir) {
					items.add(section);
					count += 1;
					
					if (includeSub) {
						count += doListItems(root, (SqSectionDir)section, 
								items, includeSub);
					}
					
				} else if (section instanceof SqRootFile) {
					SqRootFile file = (SqRootFile)section;
					items.add(file);
					count += 1;
				}
			}
		}
		
		return count;
	}
	
}
