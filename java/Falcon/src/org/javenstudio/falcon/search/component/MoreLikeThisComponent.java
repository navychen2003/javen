package org.javenstudio.falcon.search.component;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.hits.DocIterator;
import org.javenstudio.falcon.search.hits.DocList;
import org.javenstudio.falcon.search.hits.DocListAndSet;
import org.javenstudio.falcon.search.params.MoreLikeThisParams;
import org.javenstudio.falcon.search.schema.IndexSchema;

/**
 * TODO!
 * 
 * @since 1.3
 */
public class MoreLikeThisComponent extends SearchComponent {
	public static final String COMPONENT_NAME = "mlt";
  
	//public String getName() { return COMPONENT_NAME; }
	
	@Override
	public void prepare(ResponseBuilder rb) throws ErrorException {
		// do nothing
	}

	@Override
	public void process(ResponseBuilder rb) throws ErrorException {
		Params p = rb.getRequest().getParams();
		if (p.getBool( MoreLikeThisParams.MLT, false)) {
			Searcher searcher = rb.getRequest().getSearcher();
      
			NamedList<DocList> sim = getMoreLikeThese(rb, searcher,
					rb.getResults().getDocList(), rb.getFieldFlags());

			// TODO ???? add this directly to the response?
			rb.getResponse().add("moreLikeThis", sim);
		}
	}

	protected NamedList<DocList> getMoreLikeThese(ResponseBuilder rb, 
			Searcher searcher, DocList docs, int flags) throws ErrorException {
		Params p = rb.getRequest().getParams();
		IndexSchema schema = searcher.getSchema();
		
		MoreLikeThisHelper mltHelper = new MoreLikeThisHelper(p, searcher);
		NamedList<DocList> mlt = new NamedMap<DocList>();
		DocIterator iterator = docs.iterator();

		NamedMap<Object> dbg = null;
		if (rb.isDebug()) 
			dbg = new NamedMap<Object>();

		while (iterator.hasNext()) {
			int id = iterator.nextDoc();
			int rows = p.getInt(MoreLikeThisParams.DOC_COUNT, 5);
			
			DocListAndSet sim = mltHelper.getMoreLikeThis(id, 0, rows, null, null, flags);
			String name = schema.getPrintableUniqueKey(searcher.getDoc(id));
			mlt.add(name, sim.getDocList());
      
			if (dbg != null) {
				NamedMap<Object> docDbg = new NamedMap<Object>();
				docDbg.add("rawMLTQuery", mltHelper.getRawQuery().toString());
				docDbg.add("boostedMLTQuery", mltHelper.getBoostedQuery().toString());
				docDbg.add("realMLTQuery", mltHelper.getRealQuery().toString());
				
				NamedMap<Object> explains = new NamedMap<Object>();
				DocIterator mltIte = sim.getDocList().iterator();
				
				while (mltIte.hasNext()) {
					int mltid = mltIte.nextDoc();
					String key = schema.getPrintableUniqueKey(searcher.getDoc(mltid));
					explains.add(key, searcher.explain(mltHelper.getRealQuery(), mltid));
				}
				
				docDbg.add("explain", explains);
				dbg.add(name, docDbg);
			}
		}

		// add debug information
		if (dbg != null) 
			rb.addDebugInfo("moreLikeThis", dbg);
    
		return mlt;
	}
  
}
