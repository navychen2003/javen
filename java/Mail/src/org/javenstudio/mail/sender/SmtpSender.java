package org.javenstudio.mail.sender;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URISyntaxException;
import javax.net.ssl.SSLException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.Address;
import org.javenstudio.mail.AuthenticationFailedException;
import org.javenstudio.mail.CertificateValidationException;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.content.MessageData;
import org.javenstudio.mail.transport.EOLConvertingOutputStream;
import org.javenstudio.mail.transport.MailTransport;
import org.javenstudio.mail.transport.Transport;
import org.javenstudio.mime.Base64;

/**
 * This class handles all of the protocol-level aspects of sending messages via SMTP.
 */
public abstract class SmtpSender extends Sender {
	private static Logger LOG = Logger.getLogger(SmtpSender.class); 
	
    protected Transport mTransport;
    protected String mUsername;
    protected String mPassword;

    /**
     * Must implements static named constructor.
     */
    public static Sender newInstance(String uri) throws MessagingException {
    	throw new MessagingException("SmtpSender.newInstance: Unknown scheme in " + uri);
    }

    /**
     * Allowed formats for the Uri:
     * smtp://user:password@server:port
     * smtp+tls+://user:password@server:port
     * smtp+tls+trustallcerts://user:password@server:port
     * smtp+ssl+://user:password@server:port
     * smtp+ssl+trustallcerts://user:password@server:port
     *
     * @param uriString the Uri containing information to configure this sender
     */
    protected SmtpSender(String uriString) throws MessagingException {
    	if (LOG.isDebugEnabled()) 
    		LOG.debug("new SmtpSender with "+uriString); 
    	
        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException use) {
            throw new MessagingException("Invalid SmtpTransport URI", use);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.startsWith("smtp")) {
            throw new MessagingException("Unsupported protocol");
        }
        // defaults, which can be changed by security modifiers
        int connectionSecurity = Transport.CONNECTION_SECURITY_NONE;
        int defaultPort = 587;
        // check for security modifiers and apply changes
        if (scheme.contains("+ssl")) {
            connectionSecurity = Transport.CONNECTION_SECURITY_SSL;
            defaultPort = 465;
        } else if (scheme.contains("+tls")) {
            connectionSecurity = Transport.CONNECTION_SECURITY_TLS;
        }
        boolean trustCertificates = scheme.contains("+trustallcerts");

        mTransport = new MailTransport("SMTP");
        mTransport.setUri(uri, defaultPort);
        mTransport.setSecurity(connectionSecurity, trustCertificates);

        String[] userInfoParts = mTransport.getUserInfoParts();
        if (userInfoParts != null) {
            mUsername = userInfoParts[0];
            if (userInfoParts.length > 1) {
                mPassword = userInfoParts[1];
            }
        }
    }

    /**
     * For testing only.  Injects a different transport.  The transport should already be set
     * up and ready to use.  Do not use for real code.
     * @param testTransport The Transport to inject and use for all future communication.
     */
    protected void setTransport(Transport testTransport) {
        mTransport = testTransport;
    }

    @Override
    public void open() throws MessagingException {
        try {
            mTransport.open();

            // Eat the banner
            executeSimpleCommand(null);

            String localHost = "localhost";
            try {
                InetAddress localAddress = mTransport.getSocket().getLocalAddress();
                String ipAddr = localAddress.getHostAddress();
                localHost = localAddress.getHostName();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Detected hostname: " + localHost + " (" + ipAddr + ")");
                    LOG.debug("Old method: " + InetAddress.getLocalHost());
                }
                if (localHost.equals(ipAddr) || localHost.contains("_")) {
                    // We don't have a FQDN or the hostname contains invalid characters, so use IP address.
                    if (localAddress instanceof Inet6Address) {
                        localHost = "[IPV6:" + ipAddr + "]";
                    }
                    else {
                        localHost = "[" + ipAddr + "]";
                    }
                }
                if (LOG.isDebugEnabled()) {
                	LOG.debug("Using " + localHost + " for EHLO");
                }
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                	LOG.debug("Unable to look up localhost");
                }
            }

            String result = executeSimpleCommand("EHLO " + localHost);

            /*
             * TODO may need to add code to fall back to HELO I switched it from
             * using HELO on non STARTTLS connections because of AOL's mail
             * server. It won't let you use AUTH without EHLO.
             * We should really be paying more attention to the capabilities
             * and only attempting auth if it's available, and warning the user
             * if not.
             */
            if (mTransport.canTryTlsSecurity()) {
                if (result.contains("-STARTTLS") || result.contains(" STARTTLS")) {
                    executeSimpleCommand("STARTTLS");
                    mTransport.reopenTls();
                    /**
                     * Now resend the EHLO. Required by RFC2487 Sec. 5.2, and more specifically,
                     * Exim.
                     */
                    result = executeSimpleCommand("EHLO " + localHost);
                } else {
                    if (LOG.isDebugEnabled()) {
                    	LOG.debug("TLS not supported but required");
                    }
                    throw new MessagingException("TLS_REQUIRED");
                }
            }

            /*
             * result contains the results of the EHLO in concatenated form
             */
            boolean authLoginSupported = result.matches(".*AUTH.*LOGIN.*$");
            boolean authPlainSupported = result.matches(".*AUTH.*PLAIN.*$");

            if (mUsername != null && mUsername.length() > 0 && mPassword != null
                    && mPassword.length() > 0) {
                if (authPlainSupported) {
                    saslAuthPlain(mUsername, mPassword);
                }
                else if (authLoginSupported) {
                    saslAuthLogin(mUsername, mPassword);
                }
                else {
                    if (LOG.isDebugEnabled()) {
                    	LOG.debug("No valid authentication mechanism found.");
                    }
                    throw new MessagingException("AUTH_REQUIRED");
                }
            }
        } catch (SSLException e) {
            if (LOG.isDebugEnabled()) {
            	LOG.debug(e.toString(), e);
            }
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (IOException ioe) {
            if (LOG.isDebugEnabled()) {
            	LOG.debug(ioe.toString(), ioe);
            }
            throw new MessagingException(ioe.toString(), ioe);
        }
    }

    @Override
    public void sendMessage(MessageData message) throws MessagingException {
    	if (message == null) return;
    	//if (message == null) {
        //    throw new MessagingException("Trying to send non-existent message id="
        //            + Long.toString(messageId));
        //}
    	
        close();
        open();

        Address from = Address.unpackFirst(message.getFrom());
        Address[] to = Address.unpack(message.getTo());
        Address[] cc = Address.unpack(message.getCc());
        Address[] bcc = Address.unpack(message.getBcc());

        try {
            executeSimpleCommand("MAIL FROM: " + "<" + from.getAddress() + ">");
            for (Address address : to) {
                executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
            }
            for (Address address : cc) {
                executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
            }
            for (Address address : bcc) {
                executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
            }
            executeSimpleCommand("DATA");
            // TODO byte stuffing
            writeSmtpData(new EOLConvertingOutputStream(mTransport.getOutputStream()), 
            		message, true, false);
            executeSimpleCommand("\r\n.");
        } catch (IOException ioe) {
            throw new MessagingException("Unable to send message", ioe);
        }
    }

    /**
     * Write the entire message to an output stream.  This method provides buffering, so it is
     * not necessary to pass in a buffered output stream here.
     *
     * @param context system context for accessing the provider
     * @param messageId the message to write out
     * @param out the output stream to write the message to
     * @param appendQuotedText whether or not to append quoted text if this is a reply/forward
     *
     * TODO alternative parts (e.g. text+html) are not supported here.
     */
    protected abstract void writeSmtpData(OutputStream out, MessageData message, 
            boolean appendQuotedText, boolean sendBcc) throws IOException, MessagingException; 
    
    /**
     * Close the protocol (and the transport below it).
     *
     * MUST NOT return any exceptions.
     */
    @Override
    public void close() {
        mTransport.close();
    }

    /**
     * Send a single command and wait for a single response.  Handles responses that continue
     * onto multiple lines.  Throws MessagingException if response code is 4xx or 5xx.  All traffic
     * is logged (if debug logging is enabled) so do not use this function for user ID or password.
     *
     * @param command The command string to send to the server.
     * @return Returns the response string from the server.
     */
    protected String executeSimpleCommand(String command) throws IOException, MessagingException {
        return executeSensitiveCommand(command, null);
    }

    /**
     * Send a single command and wait for a single response.  Handles responses that continue
     * onto multiple lines.  Throws MessagingException if response code is 4xx or 5xx.
     *
     * @param command The command string to send to the server.
     * @param sensitiveReplacement If the command includes sensitive data (e.g. authentication)
     * please pass a replacement string here (for logging).
     * @return Returns the response string from the server.
     */
    protected String executeSensitiveCommand(String command, String sensitiveReplacement)
            throws IOException, MessagingException {
        if (command != null) {
            mTransport.writeLine(command, sensitiveReplacement);
        }

        String line = mTransport.readLine();

        String result = line;

        while (line.length() >= 4 && line.charAt(3) == '-') {
            line = mTransport.readLine();
            result += line.substring(3);
        }

        if (result.length() > 0) {
            char c = result.charAt(0);
            if ((c == '4') || (c == '5')) {
                throw new MessagingException(result);
            }
        }

        return result;
    }

//    C: AUTH LOGIN
//    S: 334 VXNlcm5hbWU6
//    C: d2VsZG9u
//    S: 334 UGFzc3dvcmQ6
//    C: dzNsZDBu
//    S: 235 2.0.0 OK Authenticated
//
//    Lines 2-5 of the conversation contain base64-encoded information. The same conversation, with base64 strings decoded, reads:
//
//
//    C: AUTH LOGIN
//    S: 334 Username:
//    C: weldon
//    S: 334 Password:
//    C: w3ld0n
//    S: 235 2.0.0 OK Authenticated

    protected void saslAuthLogin(String username, String password) 
    		throws MessagingException, AuthenticationFailedException, IOException {
        try {
            executeSimpleCommand("AUTH LOGIN");
            executeSensitiveCommand(
                    Base64.encodeToString(username.getBytes(), Base64.NO_WRAP),
                    "/username redacted/");
            executeSensitiveCommand(
                    Base64.encodeToString(password.getBytes(), Base64.NO_WRAP),
                    "/password redacted/");
        }
        catch (MessagingException me) {
            if (me.getMessage().length() > 1 && me.getMessage().charAt(1) == '3') {
                throw new AuthenticationFailedException(me.getMessage(), me.getCause());
            }
            throw me;
        }
    }

    protected void saslAuthPlain(String username, String password) throws MessagingException,
            AuthenticationFailedException, IOException {
        byte[] data = ("\000" + username + "\000" + password).getBytes();
        data = Base64.encode(data, Base64.NO_WRAP);
        try {
            executeSensitiveCommand("AUTH PLAIN " + new String(data), "AUTH PLAIN /redacted/");
        }
        catch (MessagingException me) {
            if (me.getMessage().length() > 1 && me.getMessage().charAt(1) == '3') {
                throw new AuthenticationFailedException(me.getMessage(), me.getCause());
            }
            throw me;
        }
    }
}
