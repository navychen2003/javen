package org.javenstudio.panda.analysis.payloads;

import java.io.IOException;
import java.util.Map;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.util.Logger;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.util.ResourceLoader;
import org.javenstudio.panda.util.ResourceLoaderAware;

/**
 *
 * Factory for {@link DelimitedPayloadTokenFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_dlmtd" class="lightning.TextFieldType" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="lightning.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="lightning.DelimitedPayloadTokenFilterFactory" encoder="float" delimiter="|"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 */
public class DelimitedPayloadTokenFilterFactory extends TokenFilterFactory 
		implements ResourceLoaderAware {
	static final Logger LOG = Logger.getLogger(DelimitedPayloadTokenFilterFactory.class);
	
	public static final String ENCODER_ATTR = "encoder";
	public static final String DELIMITER_ATTR = "delimiter";

	private PayloadEncoder mEncoder;
	private char mDelimiter = '|';

	@Override
	public DelimitedPayloadTokenFilter create(ITokenStream input) {
		return new DelimitedPayloadTokenFilter(input, mDelimiter, mEncoder);
	}

	@Override
	public void init(Map<String, String> args) {
		super.init(args);
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		String encoderClass = getArgs().get(ENCODER_ATTR);
		if (encoderClass == null) {
			encoderClass = "float";
			
			if (LOG.isWarnEnabled()) {
				LOG.warn("Parameter " + ENCODER_ATTR + " is mandatory, use '" 
						+ encoderClass + "'");
			}
		}
		
		if (encoderClass.equals("float")) {
			mEncoder = new FloatEncoder();
			
		} else if (encoderClass.equals("integer")) {
			mEncoder = new IntegerEncoder();
			
		} else if (encoderClass.equals("identity")) {
			mEncoder = new IdentityEncoder();
			
		} else {
			try {
				mEncoder = loader.newInstance(encoderClass, PayloadEncoder.class);
			} catch (ClassNotFoundException ex) { 
				throw new IOException(ex);
			}
		}

		String delim = getArgs().get(DELIMITER_ATTR);
		if (delim != null) {
			if (delim.length() == 1) {
				mDelimiter = delim.charAt(0);
			} else{
				throw new IllegalArgumentException("Delimiter must be one character only");
			}
		}
	}
	
}
