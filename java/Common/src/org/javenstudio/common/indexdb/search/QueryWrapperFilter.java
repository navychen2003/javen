package org.javenstudio.common.indexdb.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.util.Bits;

/** 
 * Constrains search results to only match those which also match a provided
 * query.  
 *
 * <p> This could be used, for example, with a {@link TermRangeQuery} on a suitably
 * formatted date field to implement date filtering.  One could re-use a single
 * QueryFilter that matches, e.g., only documents modified within the last
 * week.  The QueryFilter and TermRangeQuery would only need to be reconstructed
 * once per day.
 */
public class QueryWrapperFilter extends Filter {
  private final IQuery query;

  /** Constructs a filter which only matches documents matching
   * <code>query</code>.
   */
  public QueryWrapperFilter(IQuery query) {
    if (query == null)
      throw new NullPointerException("Query may not be null");
    this.query = query;
  }
  
  /** returns the inner Query */
  public final IQuery getQuery() {
    return query;
  }

  @Override
  public DocIdSet getDocIdSet(final IAtomicReaderRef context, final Bits acceptDocs) throws IOException {
    // get a private context that is used to rewrite, createWeight and score eventually
    //final IAtomicReaderRef privateContext = (IAtomicReaderRef)context.getReader().getContext();
    //final Weight weight = new IndexSearcher(privateContext).createNormalizedWeight(query);
    return new DocIdSet() {
      @Override
      public IDocIdSetIterator iterator() throws IOException {
        return null; //weight.getScorer(privateContext, true, false, acceptDocs);
      }
      @Override
      public boolean isCacheable() { return false; }
    };
  }

  @Override
  public String toString() {
    return "QueryWrapperFilter(" + query + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof QueryWrapperFilter))
      return false;
    return this.query.equals(((QueryWrapperFilter)o).query);
  }

  @Override
  public int hashCode() {
    return query.hashCode() ^ 0x923F64B9;
  }
}
