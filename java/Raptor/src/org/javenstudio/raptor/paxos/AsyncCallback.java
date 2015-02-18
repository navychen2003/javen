package org.javenstudio.raptor.paxos;

import java.util.List;

import org.javenstudio.raptor.paxos.data.ACL;
import org.javenstudio.raptor.paxos.data.Stat;

public interface AsyncCallback {
    interface StatCallback extends AsyncCallback {
        public void processResult(int rc, String path, Object ctx, Stat stat);
    }

    interface DataCallback extends AsyncCallback {
        public void processResult(int rc, String path, Object ctx, byte data[],
                Stat stat);
    }

    interface ACLCallback extends AsyncCallback {
        public void processResult(int rc, String path, Object ctx,
                List<ACL> acl, Stat stat);
    }

    interface ChildrenCallback extends AsyncCallback {
        public void processResult(int rc, String path, Object ctx,
                List<String> children);
    }

    interface Children2Callback extends AsyncCallback {
        public void processResult(int rc, String path, Object ctx,
                List<String> children, Stat stat);
    }

    interface StringCallback extends AsyncCallback {
        public void processResult(int rc, String path, Object ctx, String name);
    }

    interface VoidCallback extends AsyncCallback {
        public void processResult(int rc, String path, Object ctx);
    }
}

