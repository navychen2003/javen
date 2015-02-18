package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.util.Bits;

public interface IIndexContext {

	public IIndexFormat getIndexFormat();
	
	public ISegmentReader newSegmentReader(ISegmentCommitInfo si) 
			throws IOException;
	
	public ISegmentReader newSegmentReader(ISegmentReader reader, 
			Bits liveDocs, int numDocs) throws IOException;
	
	/** Returns true if is merge mode*/
	public boolean isMerge();
	
	/** Returns the buffer size for BufferedIndexInput/BufferedIndexOutput */
	public int getInputBufferSize();
	public int getOutputBufferSize();
	
	public int findReaderIndex(int n, List<IAtomicReaderRef> leaves);
	
	public String getCompoundFileName(String name);
	public String getCompoundEntriesFileName(String name);
	
	public String getFileNameFromGeneration(String base, String ext, long gen);
	public String getSegmentFileName(String segmentName, String ext);
	public String getSegmentFileName(String segmentName, String segmentSuffix, String ext);
	
	public String stripSegmentName(String filename);
	public String parseSegmentName(String filename);
	
	/**  This method may return null if the field does not exist.*/
	public ITerms getMultiFieldsTerms(IIndexReader r, String field) throws IOException;
	
	public ITermContext buildTermContext(IIndexReaderRef context, ITerm term, boolean cache)
			throws IOException;
	
	public IDocTermOrds createDocTermOrds(IAtomicReader reader, String field) throws IOException;
	
	public IDocTerms createDocTerms(IBytesReader bytes, IIntsReader docToOffset);
	
	public IDocTermsIndex createDocTermsIndex(IBytesReader bytes, IIntsReader termOrdToBytesOffset, 
    		IIntsReader docToTermOrd, int numOrd);
	
	public void writeCodecHeader(IDataOutput out, String codec, int version)
			throws IOException;
	
	public int checkCodecHeader(IDataInput in, String codec, int minVersion, int maxVersion)
			throws IOException;
	
	public int checkCodecHeaderNoMagic(IDataInput in, String codec, int minVersion, int maxVersion) 
			throws IOException;
	
}
