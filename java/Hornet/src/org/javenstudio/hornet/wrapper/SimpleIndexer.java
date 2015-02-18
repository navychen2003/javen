package org.javenstudio.hornet.wrapper;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IIndexWriter;
import org.javenstudio.common.indexdb.analysis.SimpleAnalyzer;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.index.AdvancedIndexParams;
import org.javenstudio.hornet.index.AdvancedIndexWriter;
import org.javenstudio.hornet.store.local.LocalFS;

public class SimpleIndexer {
	private static final Logger LOG = Logger.getLogger(SimpleIndexer.class);

	private final IDirectory mDirectory;
	private final IIndexWriter mIndexer;
	
	public SimpleIndexer(File path) throws IOException { 
		this(path, null);
	}
	
	public SimpleIndexer(File path, IAnalyzer a) throws IOException { 
		mDirectory = LocalFS.open(path.getAbsoluteFile());
		mIndexer = new AdvancedIndexWriter(mDirectory, createIndexParams(a));
	}
	
	protected AdvancedIndexParams createIndexParams(IAnalyzer a) { 
		if (a == null) a = new SimpleAnalyzer();
		return new AdvancedIndexParams(a);
	}
	
	public void addDocument(SimpleDocument doc) throws IOException { 
		if (doc == null) return;
		
		IDocument document = doc.getDocument();
		if (document == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("addDocument: " + document);
		
		mIndexer.addDocument(document);
	}
	
	public void close() throws IOException { 
		mIndexer.close();
	}
	
}
