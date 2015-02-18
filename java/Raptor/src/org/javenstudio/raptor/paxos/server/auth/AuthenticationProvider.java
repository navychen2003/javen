package org.javenstudio.raptor.paxos.server.auth;

import org.javenstudio.raptor.paxos.PaxosException;
import org.javenstudio.raptor.paxos.server.ServerCnxn;

/**
 * This interface is implemented by authentication providers to add new kinds of
 * authentication schemes to Paxos.
 */
public interface AuthenticationProvider {
    /**
     * The String used to represent this provider. This will correspond to the
     * scheme field of an Id.
     * 
     * @return the scheme of this provider.
     */
    String getScheme();

    /**
     * This method is called when a client passes authentication data for this
     * scheme. The authData is directly from the authentication packet. The
     * implementor may attach new ids to the authInfo field of cnxn or may use
     * cnxn to send packets back to the client.
     * 
     * @param cnxn
     *                the cnxn that received the authentication information.
     * @param authData
     *                the authentication data received.
     * @return TODO
     */
    PaxosException.Code handleAuthentication(ServerCnxn cnxn, byte authData[]);

    /**
     * This method is called to see if the given id matches the given id
     * expression in the ACL. This allows schemes to use application specific
     * wild cards.
     * 
     * @param id
     *                the id to check.
     * @param aclExpr
     *                the expression to match ids against.
     * @return true if the id can be matched by the expression.
     */
    boolean matches(String id, String aclExpr);

    /**
     * This method is used to check if the authentication done by this provider
     * should be used to identify the creator of a node. Some ids such as hosts
     * and ip addresses are rather transient and in general don't really
     * identify a client even though sometimes they do.
     * 
     * @return true if this provider identifies creators.
     */
    boolean isAuthenticated();

    /**
     * Validates the syntax of an id.
     * 
     * @param id
     *                the id to validate.
     * @return true if id is well formed.
     */
    boolean isValid(String id);
}

