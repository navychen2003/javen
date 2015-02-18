package org.javenstudio.raptor.bigdb;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.javenstudio.raptor.ipc.RemoteException;

/**
 * An immutable class which contains a static method for handling
 * org.javenstudio.raptor.ipc.RemoteException exceptions.
 */
public class RemoteExceptionHandler {
  /* Not instantiable */
  private RemoteExceptionHandler() {super();}

  /**
   * Examine passed Throwable.  See if its carrying a RemoteException. If so,
   * run {@link #decodeRemoteException(RemoteException)} on it.  Otherwise,
   * pass back <code>t</code> unaltered.
   * @param t Throwable to examine.
   * @return Decoded RemoteException carried by <code>t</code> or
   * <code>t</code> unaltered.
   */
  public static Throwable checkThrowable(final Throwable t) {
    Throwable result = t;
    if (t instanceof RemoteException) {
      try {
        result =
          RemoteExceptionHandler.decodeRemoteException((RemoteException)t);
      } catch (Throwable tt) {
        result = tt;
      }
    }
    return result;
  }

  /**
   * Examine passed IOException.  See if its carrying a RemoteException. If so,
   * run {@link #decodeRemoteException(RemoteException)} on it.  Otherwise,
   * pass back <code>e</code> unaltered.
   * @param e Exception to examine.
   * @return Decoded RemoteException carried by <code>e</code> or
   * <code>e</code> unaltered.
   */
  public static IOException checkIOException(final IOException e) {
    Throwable t = checkThrowable(e);
    return t instanceof IOException? (IOException)t: new IOException(t);
  }

  /**
   * Converts org.javenstudio.raptor.ipc.RemoteException into original exception,
   * if possible. If the original exception is an Error or a RuntimeException,
   * throws the original exception.
   *
   * @param re original exception
   * @return decoded RemoteException if it is an instance of or a subclass of
   *         IOException, or the original RemoteException if it cannot be decoded.
   *
   * @throws IOException indicating a server error ocurred if the decoded
   *         exception is not an IOException. The decoded exception is set as
   *         the cause.
   */
  public static IOException decodeRemoteException(final RemoteException re)
  throws IOException {
    IOException i = re;

    try {
      Class<?> c = Class.forName(re.getClassName());

      Class<?>[] parameterTypes = { String.class };
      Constructor<?> ctor = c.getConstructor(parameterTypes);

      Object[] arguments = { re.getMessage() };
      Throwable t = (Throwable) ctor.newInstance(arguments);

      if (t instanceof IOException) {
        i = (IOException) t;

      } else {
        i = new IOException("server error");
        i.initCause(t);
        throw i;
      }

    } catch (ClassNotFoundException x) {
      // continue
    } catch (NoSuchMethodException x) {
      // continue
    } catch (IllegalAccessException x) {
      // continue
    } catch (InvocationTargetException x) {
      // continue
    } catch (InstantiationException x) {
      // continue
    }
    return i;
  }
}

