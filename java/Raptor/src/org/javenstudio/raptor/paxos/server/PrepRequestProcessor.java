package org.javenstudio.raptor.paxos.server;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.PaxosException;
import org.javenstudio.raptor.paxos.PaxosDefs;
import org.javenstudio.raptor.paxos.PaxosException.Code;
import org.javenstudio.raptor.paxos.PaxosDefs.OpCode;
import org.javenstudio.raptor.paxos.common.PathUtils;
import org.javenstudio.raptor.paxos.data.ACL;
import org.javenstudio.raptor.paxos.data.Id;
import org.javenstudio.raptor.paxos.data.StatPersisted;
import org.javenstudio.raptor.paxos.proto.CreateRequest;
import org.javenstudio.raptor.paxos.proto.DeleteRequest;
import org.javenstudio.raptor.paxos.proto.SetACLRequest;
import org.javenstudio.raptor.paxos.proto.SetDataRequest;
import org.javenstudio.raptor.paxos.server.PaxosServer.ChangeRecord;
import org.javenstudio.raptor.paxos.server.auth.AuthenticationProvider;
import org.javenstudio.raptor.paxos.server.auth.ProviderRegistry;
import org.javenstudio.raptor.paxos.txn.CreateSessionTxn;
import org.javenstudio.raptor.paxos.txn.CreateTxn;
import org.javenstudio.raptor.paxos.txn.DeleteTxn;
import org.javenstudio.raptor.paxos.txn.ErrorTxn;
import org.javenstudio.raptor.paxos.txn.SetACLTxn;
import org.javenstudio.raptor.paxos.txn.SetDataTxn;
import org.javenstudio.raptor.paxos.txn.TxnHeader;

/**
 * This request processor is generally at the start of a RequestProcessor
 * change. It sets up any transactions associated with requests that change the
 * state of the system. It counts on PaxosServer to update
 * outstandingRequests, so that it can take into account transactions that are
 * in the queue to be applied when generating a transaction.
 */
public class PrepRequestProcessor extends Thread implements RequestProcessor {
    private static final Logger LOG = Logger.getLogger(PrepRequestProcessor.class);

    static boolean skipACL;
    static {
        skipACL = System.getProperty("paxos.skipACL", "no").equals("yes");
        if (skipACL) {
            LOG.info("paxos.skipACL==\"yes\", ACL checks will be skipped");
        }
    }

    /**
     * this is only for testing purposes.
     * should never be useed otherwise
     */
    private static boolean failCreate = false;

    LinkedBlockingQueue<Request> submittedRequests = new LinkedBlockingQueue<Request>();
    RequestProcessor nextProcessor;
    PaxosServer zks;

    public PrepRequestProcessor(PaxosServer zks,
            RequestProcessor nextProcessor) {
        super("ProcessThread:" + zks.getClientPort());
        this.nextProcessor = nextProcessor;
        this.zks = zks;
    }

    /**
     * method for tests to set failCreate
     * @param b
     */
    public static void setFailCreate(boolean b) {
        failCreate = b;
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                Request request = submittedRequests.take();
                long traceMask = PaxosTrace.CLIENT_REQUEST_TRACE_MASK;
                if (request.type == OpCode.ping) {
                    traceMask = PaxosTrace.CLIENT_PING_TRACE_MASK;
                }
                if (LOG.isTraceEnabled()) {
                    PaxosTrace.logRequest(LOG, traceMask, 'P', request, "");
                }
                if (Request.requestOfDeath == request) {
                    break;
                }
                pRequest(request);
            }
        } catch (InterruptedException e) {
            LOG.error("Unexpected interruption", e);
        }
        LOG.info("PrepRequestProcessor exited loop!");
    }

    ChangeRecord getRecordForPath(String path) throws PaxosException.NoNodeException {
        ChangeRecord lastChange = null;
        synchronized (zks.outstandingChanges) {
            lastChange = zks.outstandingChangesForPath.get(path);
            /*
            for (int i = 0; i < zks.outstandingChanges.size(); i++) {
                ChangeRecord c = zks.outstandingChanges.get(i);
                if (c.path.equals(path)) {
                    lastChange = c;
                }
            }
            */
            if (lastChange == null) {
                DataNode n = zks.getPaxosDatabase().getNode(path);
                if (n != null) {
                    Long acl;
                    Set<String> children;
                    synchronized(n) {
                        acl = n.acl;
                        children = n.getChildren();
                    }
                    lastChange = new ChangeRecord(-1, path, n.stat,
                        children != null ? children.size() : 0,
                            zks.getPaxosDatabase().convertLong(acl));
                }
            }
        }
        if (lastChange == null || lastChange.stat == null) {
            throw new PaxosException.NoNodeException(path);
        }
        return lastChange;
    }

    void addChangeRecord(ChangeRecord c) {
        synchronized (zks.outstandingChanges) {
            zks.outstandingChanges.add(c);
            zks.outstandingChangesForPath.put(c.path, c);
        }
    }

    static void checkACL(PaxosServer zks, List<ACL> acl, int perm,
            List<Id> ids) throws PaxosException.NoAuthException {
        if (skipACL) {
            return;
        }
        if (acl == null || acl.size() == 0) {
            return;
        }
        for (ACL a : acl) {
            Id id = a.getId();
            if ((a.getPerms() & perm) != 0) {
                if (id.getScheme().equals("world")
                        && id.getId().equals("anyone")) {
                    return;
                }
                AuthenticationProvider ap = ProviderRegistry.getProvider(id
                        .getScheme());
                if (ap != null) {
                    for (Id authId : ids) {
                        if (authId.getScheme().equals("super")) {
                            return;
                        }
                        if (authId.getScheme().equals(id.getScheme())
                                && ap.matches(authId.getId(), id.getId())) {
                            return;
                        }
                    }
                }
            }
        }
        throw new PaxosException.NoAuthException();
    }

    /**
     * This method will be called inside the ProcessRequestThread, which is a
     * singleton, so there will be a single thread calling this code.
     *
     * @param request
     */
    protected void pRequest(Request request) {
        // LOG.info("Prep>>> cxid = " + request.cxid + " type = " +
        // request.type + " id = 0x" + Long.toHexString(request.sessionId));
        TxnHeader txnHeader = null;
        Writable txn = null;
        try {
            switch (request.type) {
            case OpCode.create:
                txnHeader = new TxnHeader(request.sessionId, request.cxid, zks
                        .getNextZxid(), zks.getTime(), OpCode.create);
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                CreateRequest createRequest = new CreateRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        createRequest);
                String path = createRequest.getPath();
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || path.indexOf('\0') != -1 || failCreate) {
                    LOG.info("Invalid path " + path + " with session 0x" +
                            Long.toHexString(request.sessionId));
                    throw new PaxosException.BadArgumentsException(path);
                }
                if (!fixupACL(request.authInfo, createRequest.getAcl())) {
                    throw new PaxosException.InvalidACLException(path);
                }
                String parentPath = path.substring(0, lastSlash);
                ChangeRecord parentRecord = getRecordForPath(parentPath);

                checkACL(zks, parentRecord.acl, PaxosDefs.Perms.CREATE,
                        request.authInfo);
                int parentCVersion = parentRecord.stat.getCversion();
                CreateMode createMode =
                    CreateMode.fromFlag(createRequest.getFlags());
                if (createMode.isSequential()) {
                    path = path + String.format("%010d", parentCVersion);
                }
                try {
                    PathUtils.validatePath(path);
                } catch(IllegalArgumentException ie) {
                    LOG.info("Invalid path " + path + " with session 0x" +
                            Long.toHexString(request.sessionId));
                    throw new PaxosException.BadArgumentsException(path);
                }
                try {
                    if (getRecordForPath(path) != null) {
                        throw new PaxosException.NodeExistsException(path);
                    }
                } catch (PaxosException.NoNodeException e) {
                    // ignore this one
                }
                boolean ephemeralParent = parentRecord.stat.getEphemeralOwner() != 0;
                if (ephemeralParent) {
                    throw new PaxosException.NoChildrenForEphemeralsException(path);
                }
                txn = new CreateTxn(path, createRequest.getData(),
                        createRequest.getAcl(),
                        createMode.isEphemeral());
                StatPersisted s = new StatPersisted();
                if (createMode.isEphemeral()) {
                    s.setEphemeralOwner(request.sessionId);
                }
                parentRecord = parentRecord.duplicate(txnHeader.getZxid());
                parentRecord.childCount++;
                parentRecord.stat
                        .setCversion(parentRecord.stat.getCversion() + 1);
                addChangeRecord(parentRecord);
                addChangeRecord(new ChangeRecord(txnHeader.getZxid(), path, s,
                        0, createRequest.getAcl()));

                break;
            case OpCode.delete:
                txnHeader = new TxnHeader(request.sessionId, request.cxid, zks
                        .getNextZxid(), zks.getTime(), OpCode.delete);
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                DeleteRequest deleteRequest = new DeleteRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        deleteRequest);
                path = deleteRequest.getPath();
                lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || path.indexOf('\0') != -1
                        || zks.getPaxosDatabase().isSpecialPath(path)) {
                    throw new PaxosException.BadArgumentsException(path);
                }
                parentPath = path.substring(0, lastSlash);
                parentRecord = getRecordForPath(parentPath);
                ChangeRecord nodeRecord = getRecordForPath(path);
                checkACL(zks, parentRecord.acl, PaxosDefs.Perms.DELETE,
                        request.authInfo);
                int version = deleteRequest.getVersion();
                if (version != -1 && nodeRecord.stat.getVersion() != version) {
                    throw new PaxosException.BadVersionException(path);
                }
                if (nodeRecord.childCount > 0) {
                    throw new PaxosException.NotEmptyException(path);
                }
                txn = new DeleteTxn(path);
                parentRecord = parentRecord.duplicate(txnHeader.getZxid());
                parentRecord.childCount--;
                parentRecord.stat
                        .setCversion(parentRecord.stat.getCversion() + 1);
                addChangeRecord(parentRecord);
                addChangeRecord(new ChangeRecord(txnHeader.getZxid(), path,
                        null, -1, null));
                break;
            case OpCode.setData:
                txnHeader = new TxnHeader(request.sessionId, request.cxid, zks
                        .getNextZxid(), zks.getTime(), OpCode.setData);
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                SetDataRequest setDataRequest = new SetDataRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        setDataRequest);
                path = setDataRequest.getPath();
                nodeRecord = getRecordForPath(path);
                checkACL(zks, nodeRecord.acl, PaxosDefs.Perms.WRITE,
                        request.authInfo);
                version = setDataRequest.getVersion();
                int currentVersion = nodeRecord.stat.getVersion();
                if (version != -1 && version != currentVersion) {
                    throw new PaxosException.BadVersionException(path);
                }
                version = currentVersion + 1;
                txn = new SetDataTxn(path, setDataRequest.getData(), version);
                nodeRecord = nodeRecord.duplicate(txnHeader.getZxid());
                nodeRecord.stat.setVersion(version);
                addChangeRecord(nodeRecord);
                break;
            case OpCode.setACL:
                txnHeader = new TxnHeader(request.sessionId, request.cxid, zks
                        .getNextZxid(), zks.getTime(), OpCode.setACL);
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                SetACLRequest setAclRequest = new SetACLRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        setAclRequest);
                path = setAclRequest.getPath();
                if (!fixupACL(request.authInfo, setAclRequest.getAcl())) {
                    throw new PaxosException.InvalidACLException(path);
                }
                nodeRecord = getRecordForPath(path);
                checkACL(zks, nodeRecord.acl, PaxosDefs.Perms.ADMIN,
                        request.authInfo);
                version = setAclRequest.getVersion();
                currentVersion = nodeRecord.stat.getAversion();
                if (version != -1 && version != currentVersion) {
                    throw new PaxosException.BadVersionException(path);
                }
                version = currentVersion + 1;
                txn = new SetACLTxn(path, setAclRequest.getAcl(), version);
                nodeRecord = nodeRecord.duplicate(txnHeader.getZxid());
                nodeRecord.stat.setAversion(version);
                addChangeRecord(nodeRecord);
                break;
            case OpCode.createSession:
                txnHeader = new TxnHeader(request.sessionId, request.cxid, zks
                        .getNextZxid(), zks.getTime(), OpCode.createSession);
                request.request.rewind();
                int to = request.request.getInt();
                txn = new CreateSessionTxn(to);
                request.request.rewind();
                zks.sessionTracker.addSession(request.sessionId, to);
                zks.setOwner(request.sessionId, request.getOwner());
                break;
            case OpCode.closeSession:
                txnHeader = new TxnHeader(request.sessionId, request.cxid, zks
                        .getNextZxid(), zks.getTime(), OpCode.closeSession);
                // We don't want to do this check since the session expiration thread
                // queues up this operation without being the session owner.
                // this request is the last of the session so it should be ok
                //zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                HashSet<String> es = zks.getPaxosDatabase()
                        .getEphemerals(request.sessionId);
                synchronized (zks.outstandingChanges) {
                    for (ChangeRecord c : zks.outstandingChanges) {
                        if (c.stat == null) {
                            // Doing a delete
                            es.remove(c.path);
                        } else if (c.stat.getEphemeralOwner() == request.sessionId) {
                            es.add(c.path);
                        }
                    }
                    for (String path2Delete : es) {
                        addChangeRecord(new ChangeRecord(txnHeader.getZxid(),
                                path2Delete, null, 0, null));
                    }
                }
                LOG.info("Processed session termination for sessionid: 0x"
                        + Long.toHexString(request.sessionId));
                break;
            case OpCode.sync:
            case OpCode.exists:
            case OpCode.getData:
            case OpCode.getACL:
            case OpCode.getChildren:
            case OpCode.getChildren2:
            case OpCode.ping:
            case OpCode.setWatches:
                zks.sessionTracker.checkSession(request.sessionId,
                        request.getOwner());
                break;
            }
        } catch (PaxosException e) {
            if (txnHeader != null) {
                txnHeader.setType(OpCode.error);
                txn = new ErrorTxn(e.code().intValue());
            }
            LOG.info("Got user-level PaxosException when processing "
                    + request.toString()
                    + " Error Path:" + e.getPath()
                    + " Error:" + e.getMessage());
            request.setException(e);
        } catch (Exception e) {
            // log at error level as we are returning a marshalling
            // error to the user
            LOG.error("Failed to process " + request, e);

            StringBuilder sb = new StringBuilder();
            ByteBuffer bb = request.request;
            if(bb != null){
                bb.rewind();
                while (bb.hasRemaining()) {
                    sb.append(Integer.toHexString(bb.get() & 0xff));
                }
            } else {
                sb.append("request buffer is null");
            }

            LOG.error("Dumping request buffer: 0x" + sb.toString());
            if (txnHeader != null) {
                txnHeader.setType(OpCode.error);
                txn = new ErrorTxn(Code.MARSHALLINGERROR.intValue());
            }
        }
        request.hdr = txnHeader;
        request.txn = txn;
        request.zxid = zks.getZxid();
        nextProcessor.processRequest(request);
    }

    /**
     * This method checks out the acl making sure it isn't null or empty,
     * it has valid schemes and ids, and expanding any relative ids that
     * depend on the requestor's authentication information.
     *
     * @param authInfo list of ACL IDs associated with the client connection
     * @param acl list of ACLs being assigned to the node (create or setACL operation)
     * @return
     */
    private boolean fixupACL(List<Id> authInfo, List<ACL> acl) {
        if (skipACL) {
            return true;
        }
        if (acl == null || acl.size() == 0) {
            return false;
        }
        Iterator<ACL> it = acl.iterator();
        LinkedList<ACL> toAdd = null;
        while (it.hasNext()) {
            ACL a = it.next();
            Id id = a.getId();
            if (id.getScheme().equals("world") && id.getId().equals("anyone")) {
                // wide open
            } else if (id.getScheme().equals("auth")) {
                // This is the "auth" id, so we have to expand it to the
                // authenticated ids of the requestor
                it.remove();
                if (toAdd == null) {
                    toAdd = new LinkedList<ACL>();
                }
                boolean authIdValid = false;
                for (Id cid : authInfo) {
                    AuthenticationProvider ap =
                        ProviderRegistry.getProvider(cid.getScheme());
                    if (ap == null) {
                        LOG.error("Missing AuthenticationProvider for "
                                + cid.getScheme());
                    } else if (ap.isAuthenticated()) {
                        authIdValid = true;
                        toAdd.add(new ACL(a.getPerms(), cid));
                    }
                }
                if (!authIdValid) {
                    return false;
                }
            } else {
                AuthenticationProvider ap = ProviderRegistry.getProvider(id
                        .getScheme());
                if (ap == null) {
                    return false;
                }
                if (!ap.isValid(id.getId())) {
                    return false;
                }
            }
        }
        if (toAdd != null) {
            for (ACL a : toAdd) {
                acl.add(a);
            }
        }
        return acl.size() > 0;
    }

    public void processRequest(Request request) {
        // request.addRQRec(">prep="+zks.outstandingChanges.size());
        submittedRequests.add(request);
    }

    public void shutdown() {
        submittedRequests.clear();
        submittedRequests.add(Request.requestOfDeath);
        nextProcessor.shutdown();
    }
}

