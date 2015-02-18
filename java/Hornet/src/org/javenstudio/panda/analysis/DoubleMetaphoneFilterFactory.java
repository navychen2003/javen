package org.javenstudio.panda.analysis;

import java.util.Map;

import org.javenstudio.common.indexdb.ITokenStream;

/**
 * Factory for {@link DoubleMetaphoneFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_dblmtphn" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.DoubleMetaphoneFilterFactory" inject="true" maxCodeLength="4"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 */
public class DoubleMetaphoneFilterFactory extends TokenFilterFactory {

	/** parameter name: true if encoded tokens should be added as synonyms */
	public static final String INJECT = "inject"; 
	/** parameter name: restricts the length of the phonetic code */
	public static final String MAX_CODE_LENGTH = "maxCodeLength"; 
	/** default maxCodeLength if not specified */
	public static final int DEFAULT_MAX_CODE_LENGTH = 4;

	private int mMaxCodeLength = DEFAULT_MAX_CODE_LENGTH;
	private boolean mInject = true;

	/** Sole constructor. See {@link AnalysisFactory} for initialization lifecycle. */
	public DoubleMetaphoneFilterFactory() {}

	@Override
	public void init(Map<String, String> args) {
		super.init(args);

		mInject = getBoolean(INJECT, true);

		if (args.get(MAX_CODE_LENGTH) != null) 
			mMaxCodeLength = Integer.parseInt(args.get(MAX_CODE_LENGTH));
	}

	@Override
	public DoubleMetaphoneFilter create(ITokenStream input) {
		return new DoubleMetaphoneFilter(input, mMaxCodeLength, mInject);
	}
	
}
