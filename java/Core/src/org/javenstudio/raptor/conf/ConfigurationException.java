package org.javenstudio.raptor.conf;


/**
 * A class to encapsulate configuration related exceptions.
 *
 */
public class ConfigurationException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
     * Constructs a ConfigurationException with no specified detail message.
     */
    public ConfigurationException() {
        super();
    }
    
    /**
     * Constructs a ConfigurationException with the specified detail message.
     * @param msg the detail message.
     */
    public ConfigurationException(String msg) {
        super(msg);
    }

}
