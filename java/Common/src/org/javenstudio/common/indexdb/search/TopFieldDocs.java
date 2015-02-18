package org.javenstudio.common.indexdb.search;

import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.ITopSortDocs;

/** 
 * Represents hits returned by {@link
 * IndexSearcher#search(Query,Filter,int,Sort)}.
 */
public class TopFieldDocs extends TopDocs implements ITopSortDocs {

	/** The fields which were used to sort results by. */
	private ISortField[] mFields;
        
	/** Creates one of these objects.
	 * @param totalHits  Total number of hits for the query.
	 * @param scoreDocs  The top hits for the query.
	 * @param fields     The sort criteria used to find the top hits.
	 * @param maxScore   The maximum score encountered.
	 */
	public TopFieldDocs (int totalHits, IScoreDoc[] scoreDocs, ISortField[] fields, float maxScore) {
		super (totalHits, scoreDocs, maxScore);
		mFields = fields;
	}
  
	@Override
	public ISortField[] getSortFields() { 
		return mFields;
	}
  
}