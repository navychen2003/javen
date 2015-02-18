package org.jaudiotagger.audio.exceptions;

/**
 * Thrown if when trying to read box id the length doesn't make any sense
 */
@SuppressWarnings("serial")
public class InvalidBoxHeaderException extends RuntimeException
{
    public InvalidBoxHeaderException(String message)
    {
        super(message);
    }
}
