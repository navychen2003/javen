package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.codec.IFieldInfosFormat;

/**
 * Codec API for writing {@link FieldInfos}.
 */
public abstract class FieldInfosWriter implements IFieldInfosFormat.Writer {

	public abstract void writeFieldInfos(IFieldInfos infos) 
			throws IOException;
	
}
