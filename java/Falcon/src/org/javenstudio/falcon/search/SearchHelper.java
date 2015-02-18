package org.javenstudio.falcon.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.IndexContext;
import org.javenstudio.common.indexdb.index.IndexParams;
import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.hornet.index.AdvancedIndexContext;
import org.javenstudio.hornet.index.AdvancedIndexParams;
import org.javenstudio.hornet.index.segment.DirectoryReader;
import org.javenstudio.hornet.search.AdvancedIndexSearcher;

public class SearchHelper {

	public static IndexContext getIndexContext() { 
		return AdvancedIndexContext.getOrCreate();
	}
	
	public static IDirectoryReader openDirectory(IDirectory directory, 
			IIndexFormat format) throws IOException { 
		return DirectoryReader.open(directory, format);
	}
	
	public static IndexParams createParams(IAnalyzer analyzer, IIndexContext context) { 
		return new AdvancedIndexParams(analyzer, context);
	}
	
	public static IndexSearcher createSearcher(IIndexReader reader) { 
		return new AdvancedIndexSearcher(reader);
	}
	
	/**
	 * Returns the indexdir as given in index.properties. 
	 * If index.properties exists in dataDir and
	 * there is a property <i>index</i> available and it points to 
	 * a valid directory
	 * in dataDir that is returned Else dataDir/index is returned. 
	 * Only called for creating new indexSearchers
	 * and indexwriters. Use the getIndexDir() method to know 
	 * the active index directory
	 *
	 * @return the indexdir as given in index.properties
	 */
	public static String getNewIndexDir(ISearchCore core) {
		String result = core.getDataDir() + "index/";
		File propsFile = new File(core.getDataDir() + "index.properties");
		
		if (propsFile.exists()) {
			Properties p = new Properties();
			InputStream is = null;
			try {
				is = new FileInputStream(propsFile);
				p.load(is);
				
			} catch (IOException e) {
				// no operation
			} finally {
				IOUtils.closeQuietly(is);
			}
			
			String s = p.getProperty("index");
			if (s != null && s.trim().length() > 0) {
				File tmp = new File(core.getDataDir() + s);
				if (tmp.exists() && tmp.isDirectory())
					result = core.getDataDir() + s;
			}
		}
		
		return result;
	}
	
}
