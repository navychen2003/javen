package org.javenstudio.common.indexdb.index;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IPayloadProcessor;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Processes the given payload.
 * 
 */
public abstract class PayloadProcessor implements IPayloadProcessor {

	/** Process the incoming payload and stores the result in the given {@link BytesRef}. */
	public abstract void processPayload(BytesRef payload) throws IOException;

	/**
	 * Provides a {@link PayloadProcessorReader} to be used for a {@link Directory}.
	 * This allows using different {@link PayloadProcessorReader}s for different
	 * source {@link AtomicReader}, for e.g. to perform different processing of payloads of
	 * different directories.
	 * <p>
	 * <b>NOTE:</b> to avoid processing payloads of certain directories, you can
	 * return <code>null</code> in {@link #getReaderProcessor}.
	 * <p>
	 * <b>NOTE:</b> it is possible that the same {@link PayloadProcessorReader} will be
	 * requested for the same {@link Directory} concurrently. Therefore, to avoid
	 * concurrency issues you should return different instances for different
	 * threads. Usually, if your {@link PayloadProcessorReader} does not maintain state
	 * this is not a problem. The merge code ensures that the
	 * {@link PayloadProcessorReader} instance you return will be accessed by one
	 * thread to obtain the {@link PayloadProcessor}s for different terms.
	 * 
	 */
	public static abstract class Provider implements IPayloadProcessor.Provider {

		/**
		 * Returns a {@link PayloadProcessorReader} for the given {@link Directory},
		 * through which {@link PayloadProcessor}s can be obtained for each
		 * {@link Term}, or <code>null</code> if none should be used.
		 */
		public abstract IPayloadProcessor.Reader getProcessorReader(IAtomicReader reader) 
				throws IOException;

	}
	
	/**
	 * Returns a {@link PayloadProcessor} for a given {@link Term} which allows
	 * processing the payloads of different terms differently. If you intent to
	 * process all your payloads the same way, then you can ignore the given term.
	 * <p>
	 * <b>NOTE:</b> if you protect your {@link PayloadProcessorReader} from
	 * concurrency issues, then you shouldn't worry about any such issues when
	 * {@link PayloadProcessor}s are requested for different terms.
	 */
	public static abstract class Reader implements IPayloadProcessor.Reader {

		/** Returns a {@link PayloadProcessor} for the given term. */
		public abstract IPayloadProcessor getProcessor(String field, BytesRef text) 
				throws IOException;
	  
	}
	
}
