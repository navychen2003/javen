package org.javenstudio.jfm.filesystems;


/**
 * FileSystem Exception. this exception is thrown whenever a filesystem 
 * related problem might occur (e.g. an i/o problem)
 *
 * @author sergiu
 */
public class FSException extends Exception {
  private static final long serialVersionUID = 1L;

  public FSException() {
    super();
  }

  public FSException(String message) {
    super(message);
  }

  public FSException(Throwable cause) {
    super(cause);
  }

  public FSException(String message, Throwable cause) {
    super(message, cause);
  }

}
