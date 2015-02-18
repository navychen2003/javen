package org.javenstudio.cocoka.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class SimpleSocketFactory extends javax.net.SocketFactory implements SocketFactory {

    /**
     * The factory singleton.
     */
    private static final SimpleSocketFactory DEFAULT_FACTORY = new SimpleSocketFactory();

    private final HostNameResolver nameResolver;
    
    /**
     * Gets the singleton instance of this class.
     * @return the one and only plain socket factory
     */
    public static SimpleSocketFactory getSocketFactory() {
        return DEFAULT_FACTORY;
    }

    public SimpleSocketFactory(final HostNameResolver nameResolver) {
        super();
        this.nameResolver = nameResolver;
    }

    public SimpleSocketFactory() {
        this(null);
    }

    @Override 
    public Socket createSocket() {
        return new SimpleSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return new SimpleSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        return new SimpleSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return new SimpleSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
            int localPort) throws IOException {
        return new SimpleSocket(address, port, localAddress, localPort);
    }
    
    @Override 
    public Socket connectSocket(Socket sock, String host, int port, 
                                InetAddress localAddress, int localPort,
                                HttpParams params) throws IOException {

        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null.");
        }
    	
        int timeout = HttpConnectionParams.getConnectionTimeout(params);
        
        return connectSocket(sock, host, port, localAddress, localPort, timeout); 
    }
    
    public Socket connectSocket(Socket sock, String host, int port, 
            InetAddress localAddress, int localPort,
            int timeout) throws IOException {
    	
        if (host == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }

        if (sock == null)
            sock = createSocket();

        if ((localAddress != null) || (localPort > 0)) {

            // we need to bind explicitly
            if (localPort < 0)
                localPort = 0; // indicates "any"

            InetSocketAddress isa =
                new InetSocketAddress(localAddress, localPort);
            sock.bind(isa);
        }
        
        InetSocketAddress remoteAddress;
        if (this.nameResolver != null) {
            remoteAddress = new InetSocketAddress(this.nameResolver.resolve(host), port); 
        } else {
            remoteAddress = new InetSocketAddress(host, port);            
        }
        try {
            sock.connect(remoteAddress, timeout);
        } catch (SocketTimeoutException ex) {
            throw new ConnectTimeoutException("Connect to " + remoteAddress + " timed out");
        }
        return sock;

    } 

    /**
     * Checks whether a socket connection is secure.
     * This factory creates plain socket connections
     * which are not considered secure.
     *
     * @param sock      the connected socket
     *
     * @return  <code>false</code>
     *
     * @throws IllegalArgumentException if the argument is invalid
     */
    @Override 
    public final boolean isSecure(Socket sock)
        throws IllegalArgumentException {

        if (sock == null) {
            throw new IllegalArgumentException("Socket may not be null.");
        }
        // This class check assumes that createSocket() calls the constructor
        // directly. If it was using javax.net.SocketFactory, we couldn't make
        // an assumption about the socket class here.
        if (sock.getClass() != SimpleSocket.class) {
            throw new IllegalArgumentException("Socket not created by this factory.");
        }
        // This check is performed last since it calls a method implemented
        // by the argument object. getClass() is final in java.lang.Object.
        if (sock.isClosed()) {
            throw new IllegalArgumentException("Socket is closed.");
        }

        return false;

    } 

    /**
     * Compares this factory with an object.
     * There is only one instance of this class.
     *
     * @param obj       the object to compare with
     *
     * @return  if the argument is this object
     */
    @Override
    public boolean equals(Object obj) {
        return (obj == this);
    }

    /**
     * Obtains a hash code for this object.
     * All instances of this class have the same hash code.
     * There is only one instance of this class.
     */
    @Override
    public int hashCode() {
        return SimpleSocketFactory.class.hashCode();
    }
    
}
