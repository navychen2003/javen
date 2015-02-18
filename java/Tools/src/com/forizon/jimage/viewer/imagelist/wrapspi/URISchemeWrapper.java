package com.forizon.jimage.viewer.imagelist.wrapspi;

import com.forizon.jimage.viewer.imagelist.ImageIdentity;
import java.net.URI;

public interface URISchemeWrapper {
    @SuppressWarnings("rawtypes")
	public ImageIdentity wrap(URI wrapee) throws WrapException;
}

