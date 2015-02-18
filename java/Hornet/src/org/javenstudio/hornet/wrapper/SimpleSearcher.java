package org.javenstudio.hornet.wrapper;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.SimpleAnalyzer;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.IndexParams;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.util.Logger;
import org.javenstudio.hornet.index.AdvancedIndexParams;
import org.javenstudio.hornet.index.segment.DirectoryReader;
import org.javenstudio.hornet.search.AdvancedIndexSearcher;
import org.javenstudio.hornet.search.query.BooleanClause;
import org.javenstudio.hornet.search.query.BooleanQuery;
import org.javenstudio.hornet.search.query.MatchAllDocsQuery;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.hornet.store.local.LocalFS;

public class SimpleSearcher {
	private static final Logger LOG = Logger.getLogger(SimpleSearcher.class);

	private final IDirectory mDirectory;
	private final IIndexReader mIndexReader;
	private final IAnalyzer mAnalyzer;
	private final ISearcher mSearcher;
	
	public SimpleSearcher(File path) throws IOException { 
		this(path, null);
	}
	
	public SimpleSearcher(File path, IAnalyzer a) throws IOException { 
		mDirectory = LocalFS.open(path.getAbsoluteFile());
		
		IndexParams params = createIndexParams(a);
		IIndexFormat format = params.getContext().getIndexFormat();
		
		mAnalyzer = params.getAnalyzer();
		mIndexReader = DirectoryReader.open(mDirectory, format);
		
		mSearcher = new AdvancedIndexSearcher(mIndexReader);
	}
	
	protected IndexParams createIndexParams(IAnalyzer a) { 
		if (a == null) a = new SimpleAnalyzer();
		return new AdvancedIndexParams(a);
	}
	
	public SimpleTopDocs search(String field, String text) throws IOException { 
		return search(field, text, 100);
	}
	
	public SimpleTopDocs search(String field, String text, int topN) throws IOException { 
		BooleanQuery query = new BooleanQuery();
		
		if (text == null || text.length() == 0 || text.equals("*") || text.equals("*:*")) { 
			if (LOG.isDebugEnabled())
				LOG.debug("search: field=" + field + " text=" + text);
			
			Query q = new MatchAllDocsQuery();
			query.add(q, BooleanClause.Occur.SHOULD);
			
		} else {
			StringReader reader = new StringReader(text);
			
			if (LOG.isDebugEnabled())
				LOG.debug("search: field=" + field + " text=" + text + " reader=" + reader);
			
			ITokenStream stream = mAnalyzer.tokenStream(field, reader);
			IToken token = null;
			
			stream.reset();
			
			while ((token = stream.nextToken()) != null) { 
				token.fillBytesRef();
				BytesRef bytes = BytesRef.deepCopyOf(token.getBytesRef());
				
				if (LOG.isDebugEnabled())
					LOG.debug("search: add TermQuery: " + token);
				
				Query q = new TermQuery(new Term(field, bytes));
				query.add(q, BooleanClause.Occur.SHOULD);
			}
		}
		
		return new SimpleTopDocs(mSearcher.search(query, topN));
	}
	
	public SimpleDocument getDocument(int docID) throws CorruptIndexException, IOException { 
		IDocument doc = mSearcher.getDocument(docID);
		return doc != null ? new SimpleDocument(doc) : null;
	}
	
	public void close() throws IOException { 
		mIndexReader.close();
	}
	
}
