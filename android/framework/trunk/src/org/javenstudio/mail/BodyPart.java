package org.javenstudio.mail;

public abstract class BodyPart implements Part {
	
    protected Multipart mParent;

    public Multipart getParent() {
        return mParent;
    }
    
}