package org.javenstudio.cocoka.worker.work;

public class WorkException extends Exception {

	private static final long serialVersionUID = 1L; 
	
	public WorkException() {} 
	
	public WorkException(String error) {
		super(error); 
	} 
	
}
