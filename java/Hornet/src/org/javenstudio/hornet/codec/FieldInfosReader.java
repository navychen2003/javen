package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.codec.IFieldInfosFormat;

/**
 * Codec API for reading {@link FieldInfos}.
 */
public abstract class FieldInfosReader implements IFieldInfosFormat.Reader {

	public abstract IFieldInfos readFieldInfos() throws IOException;
	
}
