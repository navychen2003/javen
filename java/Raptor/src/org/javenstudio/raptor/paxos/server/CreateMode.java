package org.javenstudio.raptor.paxos.server;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.PaxosException;

/***
 *  CreateMode value determines how the znode is created on Paxos.
 */
public enum CreateMode {
    
    /**
     * The znode will not be automatically deleted upon client's disconnect.
     */
    PERSISTENT (0, false, false),
    /**
    * The znode will not be automatically deleted upon client's disconnect,
    * and its name will be appended with a monotonically increasing number.
    */
    PERSISTENT_SEQUENTIAL (2, false, true),
    /**
     * The znode will be deleted upon the client's disconnect.
     */
    EPHEMERAL (1, true, false),
    /**
     * The znode will be deleted upon the client's disconnect, and its name
     * will be appended with a monotonically increasing number.
     */
    EPHEMERAL_SEQUENTIAL (3, true, true);

    private static final Logger LOG = Logger.getLogger(CreateMode.class);

    private boolean ephemeral;
    private boolean sequential;
    private int flag;

    CreateMode(int flag, boolean ephemeral, boolean sequential) {
        this.flag = flag;
        this.ephemeral = ephemeral;
        this.sequential = sequential;
    }

    public boolean isEphemeral() { 
        return ephemeral;
    }

    public boolean isSequential() { 
        return sequential;
    }

    public int toFlag() {
        return flag;
    }

    /**
     * Map an integer value to a CreateMode value
     */
    static public CreateMode fromFlag(int flag) throws PaxosException {
        switch(flag) {
        case 0: return CreateMode.PERSISTENT;

        case 1: return CreateMode.EPHEMERAL;

        case 2: return CreateMode.PERSISTENT_SEQUENTIAL;

        case 3: return CreateMode.EPHEMERAL_SEQUENTIAL ;

        default:
            LOG.error("Received an invalid flag value to convert to a CreateMode");
            throw new PaxosException.BadArgumentsException(); 
        }
    }
}

