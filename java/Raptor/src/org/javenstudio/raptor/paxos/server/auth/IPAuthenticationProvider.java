package org.javenstudio.raptor.paxos.server.auth;

import org.javenstudio.raptor.paxos.data.Id;
import org.javenstudio.raptor.paxos.server.ServerCnxn;
import org.javenstudio.raptor.paxos.PaxosException;

public class IPAuthenticationProvider implements AuthenticationProvider {

    public String getScheme() {
        return "ip";
    }

    public PaxosException.Code
        handleAuthentication(ServerCnxn cnxn, byte[] authData)
    {
        String id = cnxn.getRemoteAddress().getAddress().getHostAddress();
        cnxn.getAuthInfo().add(new Id(getScheme(), id));
        return PaxosException.Code.OK;
    }

    // This is a bit weird but we need to return the address and the number of
    // bytes (to distinguish between IPv4 and IPv6
    private byte[] addr2Bytes(String addr) {
        byte b[] = v4addr2Bytes(addr);
        // TODO Write the v6addr2Bytes
        return b;
    }

    private byte[] v4addr2Bytes(String addr) {
        String parts[] = addr.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte b[] = new byte[4];
        for (int i = 0; i < 4; i++) {
            try {
                int v = Integer.parseInt(parts[i]);
                if (v >= 0 && v <= 255) {
                    b[i] = (byte) v;
                } else {
                    return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return b;
    }

    private void mask(byte b[], int bits) {
        int start = bits / 8;
        int startMask = (1 << (8 - (bits % 8))) - 1;
        startMask = ~startMask;
        while (start < b.length) {
            b[start] &= startMask;
            startMask = 0;
            start++;
        }
    }

    public boolean matches(String id, String aclExpr) {
        String parts[] = aclExpr.split("/", 2);
        byte aclAddr[] = addr2Bytes(parts[0]);
        if (aclAddr == null) {
            return false;
        }
        int bits = aclAddr.length * 8;
        if (parts.length == 2) {
            try {
                bits = Integer.parseInt(parts[1]);
                if (bits < 0 || bits > aclAddr.length * 8) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        mask(aclAddr, bits);
        byte remoteAddr[] = addr2Bytes(id);
        if (remoteAddr == null) {
            return false;
        }
        mask(remoteAddr, bits);
        for (int i = 0; i < remoteAddr.length; i++) {
            if (remoteAddr[i] != aclAddr[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isAuthenticated() {
        return false;
    }

    public boolean isValid(String id) {
        return addr2Bytes(id) != null;
    }
}

