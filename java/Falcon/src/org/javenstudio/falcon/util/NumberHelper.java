package org.javenstudio.falcon.util;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.util.NumberUtils;

public class NumberHelper {

	public static String SortableStr2int(BytesRef val) {
		// TODO: operate directly on BytesRef
		return NumberUtils.SortableStr2int(val.utf8ToString());
	}

	public static String SortableStr2long(BytesRef val) {
		// TODO: operate directly on BytesRef
		return NumberUtils.SortableStr2long(val.utf8ToString());
	}
	
	public static float SortableStr2float(BytesRef val) {
		// TODO: operate directly on BytesRef
		return NumberUtils.SortableStr2float(val.utf8ToString());
	}

	public static double SortableStr2double(BytesRef val) {
		// TODO: operate directly on BytesRef
		return NumberUtils.SortableStr2double(val.utf8ToString());
	}

	public static int SortableStr2int(BytesRef sval, int offset, int len) {
		// TODO: operate directly on BytesRef
		return NumberUtils.SortableStr2int(sval.utf8ToString(), offset, len);
	}

	public static long SortableStr2long(BytesRef sval, int offset, int len) {
		// TODO: operate directly on BytesRef
		return NumberUtils.SortableStr2long(sval.utf8ToString(), offset, len);
	}
	
}
