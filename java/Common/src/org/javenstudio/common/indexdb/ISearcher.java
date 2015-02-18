package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.search.Weight;

public interface ISearcher {

	/** Returns the context of IndexInput for this searcher */
	public IIndexContext getContext();
	
	/** Expert: Set the Similarity implementation used by this Searcher. */
	public void setSimilarity(ISimilarity similarity);
	
	public ISimilarity getSimilarity();
	
	/** Return the {@link IndexReader} this searches. */
	public IIndexReader getIndexReader();
	
	/**
	 * Returns this searchers the top-level {@link IIndexReaderRef}.
	 * @see IndexReader#getTopReaderContext() 
	 * sugar for #getReader().getTopReaderContext() 
	 */
	public IIndexReaderRef getTopReaderContext();
	
	/** Sugar for <code>.getIndexReader().document(docID)</code> */
	public IDocument getDocument(int docID) 
			throws CorruptIndexException, IOException;
	
	/** Sugar for <code>.getIndexReader().document(docID, fieldsToLoad)</code> */
	public IDocument getDocument(int docID, Set<String> fieldsToLoad) 
			throws CorruptIndexException, IOException;
	
	/** Sugar for <code>.getIndexReader().document(docID, fieldVisitor)</code> */
	public void document(int docID, IFieldVisitor fieldVisitor) 
			throws CorruptIndexException, IOException;
	
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code>.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopDocs search(IQuery query, int n) throws IOException;
	
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code>, applying <code>filter</code> if non-null.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopDocs search(IQuery query, IFilter filter, 
			int n) throws IOException;
	
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code> where all results are after a previous 
	 * result (<code>after</code>).
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, 
			int n) throws IOException;
	
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code>, applying <code>filter</code> if non-null,
	 * where all results are after a previous result (<code>after</code>).
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, 
			IFilter filter, int n) throws IOException;
	
	/** 
	 * Lower-level search API.
	 *
	 * <p>{@link Collector#collect(int)} is called for every matching document.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public void search(IQuery query, ICollector results) throws IOException;
	
	/** 
	 * Lower-level search API.
	 *
	 * <p>{@link Collector#collect(int)} is called for every matching
	 * document.
	 *
	 * @param query to match documents
	 * @param filter if non-null, used to permit documents to be collected.
	 * @param results to receive hits
	 * @throws BooleanQuery.TooManyClauses
	 */
	public void search(IQuery query, IFilter filter, 
			ICollector results) throws IOException;
	
	/** 
	 * Search implementation with arbitrary sorting.  Finds
	 * the top <code>n</code> hits for <code>query</code>, applying
	 * <code>filter</code> if non-null, and sorting the hits by the criteria in
	 * <code>sort</code>.
	 * 
	 * <p>NOTE: this does not compute scores by default; use
	 * {@link IndexSearcher#search(Query,Filter,int,Sort,boolean,boolean)} to
	 * control scoring.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopSortDocs search(IQuery query, IFilter filter, 
			int n, ISort sort) throws IOException;
	
	/** 
	 * Search implementation with arbitrary sorting, plus
	 * control over whether hit scores and max score
	 * should be computed.  Finds
	 * the top <code>n</code> hits for <code>query</code>, applying
	 * <code>filter</code> if non-null, and sorting the hits by the criteria in
	 * <code>sort</code>.  If <code>doDocScores</code> is <code>true</code>
	 * then the score of each hit will be computed and
	 * returned.  If <code>doMaxScore</code> is
	 * <code>true</code> then the maximum score over all
	 * collected hits will be computed.
	 * 
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopSortDocs search(IQuery query, IFilter filter, int n, ISort sort, 
			boolean doDocScores, boolean doMaxScore) throws IOException;
	
	/**
	 * Search implementation with arbitrary sorting and no filter.
	 * @param query The query to search for
	 * @param n Return only the top n results
	 * @param sort The {@link Sort} object
	 * @return The top docs, sorted according to the supplied {@link Sort} instance
	 * @throws IOException
	 */
	public ITopSortDocs search(IQuery query, int n, ISort sort) throws IOException;
	
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code>, applying <code>filter</code> if non-null,
	 * where all results are after a previous result (<code>after</code>).
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, IFilter filter, 
			int n, ISort sort) throws IOException;
	
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code> where all results are after a previous 
	 * result (<code>after</code>).
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, 
			int n, ISort sort) throws IOException;
	
	/** 
	 * Finds the top <code>n</code>
	 * hits for <code>query</code> where all results are after a previous 
	 * result (<code>after</code>), allowing control over
	 * whether hit scores and max score should be computed.
	 * <p>
	 * By passing the bottom result from a previous page as <code>after</code>,
	 * this method can be used for efficient 'deep-paging' across potentially
	 * large result sets.  If <code>doDocScores</code> is <code>true</code>
	 * then the score of each hit will be computed and
	 * returned.  If <code>doMaxScore</code> is
	 * <code>true</code> then the maximum score over all
	 * collected hits will be computed.
	 *
	 * @throws BooleanQuery.TooManyClauses
	 */
	public ITopDocs searchAfter(IScoreDoc after, IQuery query, IFilter filter, 
			int n, ISort sort, boolean doDocScores, boolean doMaxScore) 
					throws IOException;
	
	/** 
	 * Returns an Explanation that describes how <code>doc</code> scored against
	 * <code>query</code>.
	 *
	 * <p>This is intended to be used in developing Similarity implementations,
	 * and, for good performance, should not be displayed with every hit.
	 * Computing an explanation is as expensive as executing the query over the
	 * entire index.
	 */
	public IExplanation explain(IQuery query, int doc) throws IOException;
	
	/**
	 * Returns {@link TermStatistics} for a term.
	 * 
	 * This can be overridden for example, to return a term's statistics
	 * across a distributed collection.
	 */
	public ITermStatistics getTermStatistics(ITerm term, ITermContext context) 
			throws IOException;
	
	/**
	 * Returns {@link CollectionStatistics} for a field.
	 * 
	 * This can be overridden for example, to return a field's statistics
	 * across a distributed collection.
	 */
	public ICollectionStatistics getCollectionStatistics(String field) 
			throws IOException;
	
	/**
	 * Creates a normalized weight for a top-level {@link Query}.
	 * The query is rewritten by this method and {@link Query#createWeight} called,
	 * afterwards the {@link Weight} is normalized. The returned {@code Weight}
	 * can then directly be used to get a {@link Scorer}.
	 */
	public IWeight createNormalizedWeight(IQuery query) throws IOException;
	
}
