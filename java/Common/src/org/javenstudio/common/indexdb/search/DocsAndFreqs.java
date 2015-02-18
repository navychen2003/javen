package org.javenstudio.common.indexdb.search;

import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IExactSimilarityScorer;

public class DocsAndFreqs {

    public final IDocsEnum docsAndFreqs;
    public final IDocsEnum docs;
    public final int docFreq;
    public final IExactSimilarityScorer docScorer;
    public int doc = -1;

    public DocsAndFreqs(IDocsEnum docsAndFreqs, IDocsEnum docs, int docFreq, IExactSimilarityScorer docScorer) {
      this.docsAndFreqs = docsAndFreqs;
      this.docs = docs;
      this.docFreq = docFreq;
      this.docScorer = docScorer;
    }
	
}
