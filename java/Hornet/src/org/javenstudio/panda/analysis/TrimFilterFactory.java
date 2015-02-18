package org.javenstudio.panda.analysis;

import java.util.Map;

import org.javenstudio.common.indexdb.ITokenStream;

/**
 * Factory for {@link TrimFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_trm" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.NGramTokenizerFactory"/&gt;
 *     &lt;filter class="solr.TrimFilterFactory" updateOffsets="false"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 * @see TrimFilter
 */
public class TrimFilterFactory extends TokenFilterFactory {
  
	protected boolean mUpdateOffsets = false;
  
	@Override
	public void init(Map<String,String> args) {
		super.init(args);
    
		String v = args.get("updateOffsets");
		if (v != null) 
			mUpdateOffsets = Boolean.valueOf(v);
	}
  
	@Override
	public TrimFilter create(ITokenStream input) {
		return new TrimFilter(input, mUpdateOffsets);
	}
	
}
