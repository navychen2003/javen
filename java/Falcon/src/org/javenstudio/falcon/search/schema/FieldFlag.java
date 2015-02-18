package org.javenstudio.falcon.search.schema;

public enum FieldFlag {
	
	INDEXED('I', "Indexed"), 
	TOKENIZED('T', "Tokenized"), 
	STORED('S', "Stored"), 
	MULTI_VALUED('M', "Multivalued"),
	TERM_VECTOR_STORED('V', "TermVector Stored"), 
	TERM_VECTOR_OFFSET('o', "Store Offset With TermVector"),
	TERM_VECTOR_POSITION('p', "Store Position With TermVector"),
	OMIT_NORMS('O', "Omit Norms"), 
	OMIT_TF('F', "Omit Term Frequencies & Positions"), 
	OMIT_POSITIONS('P', "Omit Positions"),
	LAZY('L', "Lazy"), 
	BINARY('B', "Binary"), 
	SORT_MISSING_FIRST('f', "Sort Missing First"), 
	SORT_MISSING_LAST('l', "Sort Missing Last");

	private final char mAbbreviation;
	private final String mDisplayText;

	FieldFlag(char abbreviation, String display) {
		mAbbreviation = abbreviation;
		mDisplayText = display;
		mDisplayText.intern(); //QUESTION: Need we bother here?
	}

	public static FieldFlag getFlag(char abbrev){
		FieldFlag result = null;
		FieldFlag[] vals = FieldFlag.values();
		for (int i = 0; i < vals.length; i++) {
			if (vals[i].getAbbreviation() == abbrev){
				result = vals[i];
				break;
			}
		}
		return result;
	}

	public char getAbbreviation() {
		return mAbbreviation;
	}

	public String getDisplayText() {
		return mDisplayText;
	}
	
}
