package org.javenstudio.raptor.paxos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.AsyncCallback.DataCallback;
import org.javenstudio.raptor.paxos.PaxosDefs.Ids;
import org.javenstudio.raptor.paxos.data.ACL;
import org.javenstudio.raptor.paxos.data.Id;
import org.javenstudio.raptor.paxos.data.Stat;
import org.javenstudio.raptor.paxos.server.CreateMode;
import org.javenstudio.raptor.paxos.server.ServerConfig;

/**
 * The command line client to Paxos.
 *
 */
public class PaxosMain {
    private static final Logger LOG = Logger.getLogger(PaxosMain.class);
    
    protected static final Map<String,String> commandMap = new HashMap<String,String>( );

    protected MyCommandOptions cl = new MyCommandOptions();
    protected HashMap<Integer,String> history = new HashMap<Integer,String>( );
    protected int commandCount = 0;
    protected boolean printWatches = true;

    protected Paxos paxos;
    protected String host = "";

    public boolean getPrintWatches( ) {
        return printWatches;
    }

    static {
        commandMap.put("connect", "host:port");
        commandMap.put("close","");
        commandMap.put("create", "[-s] [-e] path data acl");
        commandMap.put("delete","path [version]");
        commandMap.put("set","path data [version]");
        commandMap.put("get","path [watch]");
        commandMap.put("ls","path [watch]");
        commandMap.put("ls2","path [watch]");
        commandMap.put("getAcl","path");
        commandMap.put("setAcl","path acl");
        commandMap.put("stat","path [watch]");
        commandMap.put("sync","path");
        commandMap.put("setquota","-n|-b val path");
        commandMap.put("listquota","path");
        commandMap.put("delquota","[-n|-b] path");
        commandMap.put("history","");
        commandMap.put("redo","cmdno");
        commandMap.put("printwatches", "on|off");
        commandMap.put("quit","");
        commandMap.put("addauth", "scheme auth");
    }

    static void usage() {
        System.err.println("Paxos -server host:port cmd args");
        for (String cmd : commandMap.keySet()) {
            System.err.println("\t"+cmd+ " " + commandMap.get(cmd));
        }
    }

    private class MyWatcher implements Watcher {
        public void process(WatchedEvent event) {
            if (getPrintWatches()) {
                PaxosMain.printMessage("WATCHER:: "+event.toString());
            }
        }
    }

    static private int getPermFromString(String permString) {
        int perm = 0;
        for (int i = 0; i < permString.length(); i++) {
            switch (permString.charAt(i)) {
            case 'r':
                perm |= PaxosDefs.Perms.READ;
                break;
            case 'w':
                perm |= PaxosDefs.Perms.WRITE;
                break;
            case 'c':
                perm |= PaxosDefs.Perms.CREATE;
                break;
            case 'd':
                perm |= PaxosDefs.Perms.DELETE;
                break;
            case 'a':
                perm |= PaxosDefs.Perms.ADMIN;
                break;
            default:
                System.err
                .println("Unknown perm type: " + permString.charAt(i));
            }
        }
        return perm;
    }

    private static void printStat(Stat stat) {
        System.err.println("cZxid = 0x" + Long.toHexString(stat.getCzxid()));
        System.err.println("ctime = " + new Date(stat.getCtime()).toString());
        System.err.println("mZxid = 0x" + Long.toHexString(stat.getMzxid()));
        System.err.println("mtime = " + new Date(stat.getMtime()).toString());
        System.err.println("pZxid = 0x" + Long.toHexString(stat.getPzxid()));
        System.err.println("cversion = " + stat.getCversion());
        System.err.println("dataVersion = " + stat.getVersion());
        System.err.println("aclVersion = " + stat.getAversion());
        System.err.println("ephemeralOwner = 0x"
        		+ Long.toHexString(stat.getEphemeralOwner()));
        System.err.println("dataLength = " + stat.getDataLength());
        System.err.println("numChildren = " + stat.getNumChildren());
    }

    /**
     * A storage class for both command line options and shell commands.
     *
     */
    static private class MyCommandOptions {

        private Map<String,String> options = new HashMap<String,String>();
        private List<String> cmdArgs = null;
        private String command = null;

        public MyCommandOptions() {
          options.put("server", "localhost:" + ServerConfig.CLIENT_PORT);
          options.put("timeout", "30000");
        }

        public String getOption(String opt) {
            return options.get(opt);
        }

        public String getCommand( ) {
            return command;
        }

        @SuppressWarnings("unused")
		public String getCmdArgument( int index ) {
            return cmdArgs.get(index);
        }

        @SuppressWarnings("unused")
		public int getNumArguments( ) {
            return cmdArgs.size();
        }

        public String[] getArgArray() {
            return cmdArgs.toArray(new String[0]);
        }

        /**
         * Parses a command line that may contain one or more flags
         * before an optional command string
         * @param args command line arguments
         * @return true if parsing succeeded, false otherwise.
         */
        public boolean parseOptions(String[] args) {
            List<String> argList = Arrays.asList(args);
            Iterator<String> it = argList.iterator();

            while (it.hasNext()) {
                String opt = it.next();
                try {
                    if (opt.equals("-server")) {
                        options.put("server", it.next());
                    } else if (opt.equals("-timeout")) {
                        options.put("timeout", it.next());
                    }
                } catch (NoSuchElementException e){
                    System.err.println("Error: no argument found for option " + opt);
                    return false;
                }

                if (!opt.startsWith("-")) {
                    command = opt;
                    cmdArgs = new ArrayList<String>( );
                    cmdArgs.add( command );
                    while (it.hasNext()) {
                        cmdArgs.add(it.next());
                    }
                    return true;
                }
            }
            return true;
        }

        /**
         * Breaks a string into command + arguments.
         * @param cmdstring string of form "cmd arg1 arg2..etc"
         * @return true if parsing succeeded.
         */
        public boolean parseCommand( String cmdstring ) {
            String[] args = cmdstring.split(" ");
            if (args.length == 0)
                return false;
            
            command = args[0];
            cmdArgs = Arrays.asList(args);
            return true;
        }
    }

    /**
     * Makes a list of possible completions, either for commands
     * or for paxos nodes if the token to complete begins with /
     *
     */
    protected void addToHistory(int i,String cmd) {
        history.put(i, cmd);
    }

    public static List<String> getCommands() {
        return new LinkedList<String>(commandMap.keySet());
    }

    protected String getPrompt() {       
        return "[paxos: " + host + "("+paxos.getState()+")" + " " + commandCount + "] ";
    }

    public static void printMessage(String msg) {
        System.out.println(msg);
    }

    protected void connectToPaxos(String newHost) throws InterruptedException, IOException {
        if (paxos != null && paxos.getState().isAlive()) 
            paxos.close();
        
        host = newHost;
        paxos = new Paxos(host,
                 Integer.parseInt(cl.getOption("timeout")),
                 new MyWatcher());
    }
    
    public static void doMain(String args[])
        throws PaxosException, IOException, InterruptedException {
        //Configuration conf = ConfigurationFactory.create(true); 
        PaxosMain main = new PaxosMain(args);
        main.run();
    }

    public PaxosMain(String args[]) throws IOException, InterruptedException {
        cl.parseOptions(args);
        System.out.println("Connecting to " + cl.getOption("server"));
        connectToPaxos(cl.getOption("server"));
        //paxos = new Paxos(cl.getOption("server"),
//                Integer.parseInt(cl.getOption("timeout")), new MyWatcher());
    }

    public PaxosMain(Paxos paxos) {
      this.paxos = paxos;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void run() throws PaxosException, IOException, InterruptedException {
        if (cl.getCommand() == null) {
            System.out.println("Welcome to Paxos!");

            boolean jlinemissing = true;
            // only use jline if it's in the classpath
            try {
            	if (!jlinemissing) {
	                Class consoleC = Class.forName("jline.ConsoleReader");
	                Class completorC =
	                    Class.forName("org.javenstudio.raptor.paxos.JLineZNodeCompletor");
	
	                System.out.println("JLine support is enabled");
	
	                Object console =
	                    consoleC.getConstructor().newInstance();
	
	                Object completor =
	                    completorC.getConstructor(Paxos.class).newInstance(paxos);
	                Method addCompletor = consoleC.getMethod("addCompletor",
	                        Class.forName("jline.Completor"));
	                addCompletor.invoke(console, completor);
	
	                String line;
	                Method readLine = consoleC.getMethod("readLine", String.class);
	                while ((line = (String)readLine.invoke(console, getPrompt())) != null) {
	                    executeLine(line);
	                }
            	}
            } catch (ClassNotFoundException e) {
            	if (LOG.isDebugEnabled())
            		LOG.debug("Unable to start jline: " + e, e);
                jlinemissing = true;
            } catch (NoSuchMethodException e) {
            	if (LOG.isDebugEnabled())
            		LOG.debug("Unable to start jline: " + e, e);
                jlinemissing = true;
            } catch (InvocationTargetException e) {
            	if (LOG.isDebugEnabled())
            		LOG.debug("Unable to start jline: " + e, e);
                jlinemissing = true;
            } catch (IllegalAccessException e) {
            	if (LOG.isDebugEnabled())
            		LOG.debug("Unable to start jline: " + e, e);
                jlinemissing = true;
            } catch (InstantiationException e) {
            	if (LOG.isDebugEnabled())
            		LOG.debug("Unable to start jline: " + e, e);
                jlinemissing = true;
            }

            if (jlinemissing) {
                System.out.println("JLine support is disabled");
                BufferedReader br =
                    new BufferedReader(new InputStreamReader(System.in));

                String line;
                while ((line = br.readLine()) != null) {
                    executeLine(line);
                }
            }
        }

        boolean watch = processCmd(cl);
        if (!watch) 
            System.exit(0);
    }

    public void executeLine(String line)
    		throws InterruptedException, IOException, PaxosException {
    	if (!line.equals("")) {
    		cl.parseCommand(line);
    		addToHistory(commandCount,line);
    		processCmd(cl);
    		commandCount++;
    	}
    }

    private static DataCallback dataCallback = new DataCallback() {

        public void processResult(int rc, String path, Object ctx, byte[] data,
                Stat stat) {
            System.out.println("rc = " + rc + " path = " + path + " data = "
                    + (data == null ? "null" : new String(data)) + " stat = ");
            printStat(stat);
        }

    };

    /**
     * trim the quota tree to recover unwanted tree elements
     * in the quota's tree
     * @param paxos the zookeeper client
     * @param path the path to start from and go up and see if their
     * is any unwanted parent in the path.
     * @return true if sucessful
     * @throws PaxosException
     * @throws IOException
     * @throws InterruptedException
     */
    private static boolean trimProcQuotas(Paxos paxos, String path)
        throws PaxosException, IOException, InterruptedException 
    {
        if (Quotas.quotaPaxos.equals(path)) 
            return true;
        
        List<String> children = paxos.getChildren(path, false);
        if (children.size() == 0) {
            paxos.delete(path, -1);
            String parent = path.substring(0, path.lastIndexOf('/'));
            return trimProcQuotas(paxos, parent);
        } else {
            return true;
        }
    }

    /**
     * this method deletes quota for a node.
     * @param paxos the zookeeper client
     * @param path the path to delete quota for
     * @param bytes true if number of bytes needs to
     * be unset
     * @param numNodes true if number of nodes needs
     * to be unset
     * @return true if quota deletion is successful
     * @throws PaxosException
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean delQuota(Paxos paxos, String path,
            boolean bytes, boolean numNodes)
        throws PaxosException, IOException, InterruptedException
    {
        String parentPath = Quotas.quotaPaxos + path;
        String quotaPath = Quotas.quotaPaxos + path + "/" + Quotas.limitNode;
        if (paxos.exists(quotaPath, false) == null) {
            System.out.println("Quota does not exist for " + path);
            return true;
        }
        byte[] data = null;
        try {
            data = paxos.getData(quotaPath, false, new Stat());
        } catch(PaxosException.NoNodeException ne) {
            System.err.println("quota does not exist for " + path);
            return true;
        }
        StatsTrack strack = new StatsTrack(new String(data));
        if (bytes && !numNodes) {
            strack.setBytes(-1L);
            paxos.setData(quotaPath, strack.toString().getBytes(), -1);
        } else if (!bytes && numNodes) {
            strack.setCount(-1);
            paxos.setData(quotaPath, strack.toString().getBytes(), -1);
        } else if (bytes && numNodes) {
            // delete till you can find a node with more than
            // one child
            List<String> children = paxos.getChildren(parentPath, false);
            /// delete the direct children first
            for (String child: children) {
                paxos.delete(parentPath + "/" + child, -1);
            }
            // cut the tree till their is more than one child
            trimProcQuotas(paxos, parentPath);
        }
        return true;
    }

    private static void checkIfParentQuota(Paxos paxos, String path)
        throws InterruptedException, PaxosException
    {
        final String[] splits = path.split("/");
        String quotaPath = Quotas.quotaPaxos;
        for (String str: splits) {
            if (str.length() == 0) {
                // this should only be for the beginning of the path
                // i.e. "/..." - split(path)[0] is empty string before first '/'
                continue;
            }
            quotaPath += "/" + str;
            List<String> children =  null;
            try {
                children = paxos.getChildren(quotaPath, false);
            } catch(PaxosException.NoNodeException ne) {
            	if (LOG.isDebugEnabled())
            		LOG.debug("child removed during quota check", ne);
                return;
            }
            if (children.size() == 0) {
                return;
            }
            for (String child: children) {
                if (Quotas.limitNode.equals(child)) {
                    throw new IllegalArgumentException(path + " has a parent "
                            + quotaPath + " which has a quota");
                }
            }
        }
    }

    /**
     * this method creates a quota node for the path
     * @param paxos the Paxos client
     * @param path the path for which quota needs to be created
     * @param bytes the limit of bytes on this path
     * @param numNodes the limit of number of nodes on this path
     * @return true if its successful and false if not.
     */
    public static boolean createQuota(Paxos paxos, String path,
            long bytes, int numNodes)
        throws PaxosException, IOException, InterruptedException
    {
        // check if the path exists. We cannot create
        // quota for a path that already exists in zookeeper
        // for now.
        Stat initStat = paxos.exists(path, false);
        if (initStat == null) {
            throw new IllegalArgumentException(path + " does not exist.");
        }
        // now check if their is already existing
        // parent or child that has quota

        String quotaPath = Quotas.quotaPaxos;
        // check for more than 2 children --
        // if zookeeper_stats and zookeeper_qutoas
        // are not the children then this path
        // is an ancestor of some path that
        // already has quota
        String realPath = Quotas.quotaPaxos + path;
        try {
            List<String> children = paxos.getChildren(realPath, false);
            for (String child: children) {
                if (!child.startsWith("zookeeper_")) {
                    throw new IllegalArgumentException(path + " has child " +
                            child + " which has a quota");
                }
            }
        } catch(PaxosException.NoNodeException ne) {
            // this is fine
        }

        //check for any parent that has been quota
        checkIfParentQuota(paxos, path);

        // this is valid node for quota
        // start creating all the parents
        if (paxos.exists(quotaPath, false) == null) {
            try {
                paxos.create(Quotas.procPaxos, null, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
                paxos.create(Quotas.quotaPaxos, null, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            } catch(PaxosException.NodeExistsException ne) {
                // do nothing
            }
        }

        // now create the direct children
        // and the stat and quota nodes
        String[] splits = path.split("/");
        StringBuilder sb = new StringBuilder();
        sb.append(quotaPath);
        for (int i=1; i < splits.length; i++) {
            sb.append("/" + splits[i]);
            quotaPath = sb.toString();
            try {
                paxos.create(quotaPath, null, Ids.OPEN_ACL_UNSAFE ,
                        CreateMode.PERSISTENT);
            } catch(PaxosException.NodeExistsException ne) {
                //do nothing
            }
        }
        String statPath = quotaPath + "/" + Quotas.statNode;
        quotaPath = quotaPath + "/" + Quotas.limitNode;
        StatsTrack strack = new StatsTrack(null);
        strack.setBytes(bytes);
        strack.setCount(numNodes);
        try {
            paxos.create(quotaPath, strack.toString().getBytes(),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            StatsTrack stats = new StatsTrack(null);
            stats.setBytes(0L);
            stats.setCount(0);
            paxos.create(statPath, stats.toString().getBytes(),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch(PaxosException.NodeExistsException ne) {
            byte[] data = paxos.getData(quotaPath, false , new Stat());
            StatsTrack strackC = new StatsTrack(new String(data));
            if (bytes != -1L) {
                strackC.setBytes(bytes);
            }
            if (numNodes != -1) {
                strackC.setCount(numNodes);
            }
            paxos.setData(quotaPath, strackC.toString().getBytes(), -1);
        }
        return true;
    }

    protected boolean processCmd(MyCommandOptions co)
        throws PaxosException, IOException, InterruptedException
    {
        try {
            return processCommand(co);
        } catch (IllegalArgumentException e) {
            System.err.println("Command failed: " + e);
        } catch (PaxosException.NoNodeException e) {
            System.err.println("Node does not exist: " + e.getPath());
        } catch (PaxosException.NoChildrenForEphemeralsException e) {
            System.err.println("Ephemerals cannot have children: "
                    + e.getPath());
        } catch (PaxosException.NodeExistsException e) {
            System.err.println("Node already exists: " + e.getPath());
        } catch (PaxosException.NotEmptyException e) {
            System.err.println("Node not empty: " + e.getPath());
        }
        return false;
    }

    protected boolean processCommand(MyCommandOptions co)
        throws PaxosException, IOException, InterruptedException
    {
        Stat stat = new Stat();
        String[] args = co.getArgArray();
        String cmd = co.getCommand();
        if (args.length < 1) {
            usage();
            return false;
        }
        if (!commandMap.containsKey(cmd)) {
            usage();
            return false;
        }
        
        boolean watch = args.length > 2;
        String path = null;
        List<ACL> acl = Ids.OPEN_ACL_UNSAFE;
        
        if (LOG.isDebugEnabled())
        	LOG.debug("Processing " + cmd);
        
        if (cmd.equals("quit")) {
            System.out.println("Quitting...");
            paxos.close();
            System.exit(0);
        } else if (cmd.equals("redo") && args.length >= 2) {
            Integer i = Integer.decode(args[1]);
            if (commandCount <= i){ // don't allow redoing this redo
                System.out.println("Command index out of range");
                return false;
            }
            cl.parseCommand(history.get(i));
            if (cl.getCommand().equals( "redo" )){
                System.out.println("No redoing redos");
                return false;
            }
            history.put(commandCount, history.get(i));
            processCmd( cl);
        } else if (cmd.equals("history")) {
            for (int i=commandCount - 10;i<=commandCount;++i) {
                if (i < 0) continue;
                System.out.println(i + " - " + history.get(i));
            }
        } else if (cmd.equals("printwatches")) {
            if (args.length == 1) {
                System.out.println("printwatches is " + (printWatches ? "on" : "off"));
            } else {
                printWatches = args[1].equals("on");
            }
        } else if (cmd.equals("connect")) {
            if (args.length >=2) {
                connectToPaxos(args[1]);
            } else {
                connectToPaxos(host);                
            }
        } 
        
        // Below commands all need a live connection
        if (paxos == null || !paxos.state.isAlive()) {
            System.out.println("Not connected");
            return false;
        }
        
        if (cmd.equals("create") && args.length >= 3) {
            int first = 0;
            CreateMode flags = CreateMode.PERSISTENT;
            if ((args[1].equals("-e") && args[2].equals("-s"))
                    || (args[1]).equals("-s") && (args[2].equals("-e"))) {
                first+=2;
                flags = CreateMode.EPHEMERAL_SEQUENTIAL;
            } else if (args[1].equals("-e")) {
                first++;
                flags = CreateMode.EPHEMERAL;
            } else if (args[1].equals("-s")) {
                first++;
                flags = CreateMode.PERSISTENT_SEQUENTIAL;
            }
            if (args.length == first + 4) {
                acl = parseACLs(args[first+3]);
            }
            path = args[first + 1];
            String newPath = paxos.create(path, args[first+2].getBytes(), acl,
                    flags);
            System.err.println("Created " + newPath);
        } else if (cmd.equals("delete") && args.length >= 2) {
            path = args[1];
            paxos.delete(path, watch ? Integer.parseInt(args[2]) : -1);
        } else if (cmd.equals("set") && args.length >= 3) {
            path = args[1];
            stat = paxos.setData(path, args[2].getBytes(),
                    args.length > 3 ? Integer.parseInt(args[3]) : -1);
            printStat(stat);
        } else if (cmd.equals("aget") && args.length >= 2) {
            path = args[1];
            paxos.getData(path, watch, dataCallback, path);
        } else if (cmd.equals("get") && args.length >= 2) {
            path = args[1];
            byte data[] = paxos.getData(path, watch, stat);
            data = (data == null)? "null".getBytes() : data;
            System.out.println(new String(data));
            printStat(stat);
        } else if (cmd.equals("ls") && args.length >= 2) {
            path = args[1];
            List<String> children = paxos.getChildren(path, watch);
            System.out.println(children);
        } else if (cmd.equals("ls2") && args.length >= 2) {
            path = args[1];
            List<String> children = paxos.getChildren(path, watch, stat);
            System.out.println(children);
            printStat(stat);
        } else if (cmd.equals("getAcl") && args.length >= 2) {
            path = args[1];
            acl = paxos.getACL(path, stat);
            for (ACL a : acl) {
                System.out.println(a.getId() + ": "
                        + getPermString(a.getPerms()));
            }
        } else if (cmd.equals("setAcl") && args.length >= 3) {
            path = args[1];
            stat = paxos.setACL(path, parseACLs(args[2]),
                    args.length > 4 ? Integer.parseInt(args[3]) : -1);
            printStat(stat);
        } else if (cmd.equals("stat") && args.length >= 2) {
            path = args[1];
            stat = paxos.exists(path, watch);
            printStat(stat);
        } else if (cmd.equals("listquota") && args.length >= 2) {
            path = args[1];
            String absolutePath = Quotas.quotaPaxos + path + "/" + Quotas.limitNode;
            byte[] data =  null;
            try {
                System.err.println("absolute path is " + absolutePath);
                data = paxos.getData(absolutePath, false, stat);
                StatsTrack st = new StatsTrack(new String(data));
                System.out.println("Output quota for " + path + " "
                        + st.toString());

                data = paxos.getData(Quotas.quotaPaxos + path + "/" +
                        Quotas.statNode, false, stat);
                System.out.println("Output stat for " + path + " " +
                        new StatsTrack(new String(data)).toString());
            } catch(PaxosException.NoNodeException ne) {
                System.err.println("quota for " + path + " does not exist.");
            }
        } else if (cmd.equals("setquota") && args.length >= 4) {
            String option = args[1];
            String val = args[2];
            path = args[3];
            System.err.println("Comment: the parts are " +
                               "option " + option +
                               " val " + val +
                               " path " + path);
            if ("-b".equals(option)) {
                // we are setting the bytes quota
                createQuota(paxos, path, Long.parseLong(val), -1);
            } else if ("-n".equals(option)) {
                // we are setting the num quota
                createQuota(paxos, path, -1L, Integer.parseInt(val));
            } else {
                usage();
            }

        } else if (cmd.equals("delquota") && args.length >= 2) {
            //if neither option -n or -b is specified, we delete
            // the quota node for thsi node.
            if (args.length == 3) {
                //this time we have an option
                String option = args[1];
                path = args[2];
                if ("-b".equals(option)) {
                    delQuota(paxos, path, true, false);
                } else if ("-n".equals(option)) {
                    delQuota(paxos, path, false, true);
                }
            } else if (args.length == 2) {
                path = args[1];
                // we dont have an option specified.
                // just delete whole quota node
                delQuota(paxos, path, true, true);
            } else if (cmd.equals("help")) {
                usage();
            }
        } else if (cmd.equals("close")) {
                paxos.close();            
        } else if (cmd.equals("addauth") && args.length >=2 ) {
            byte[] b = null;
            if (args.length >= 3)
                b = args[2].getBytes();

            paxos.addAuthInfo(args[1], b);
        } else {
            usage();
        }
        return watch;
    }

    private static String getPermString(int perms) {
        StringBuilder p = new StringBuilder();
        if ((perms & PaxosDefs.Perms.CREATE) != 0) {
            p.append('c');
        }
        if ((perms & PaxosDefs.Perms.DELETE) != 0) {
            p.append('d');
        }
        if ((perms & PaxosDefs.Perms.READ) != 0) {
            p.append('r');
        }
        if ((perms & PaxosDefs.Perms.WRITE) != 0) {
            p.append('w');
        }
        if ((perms & PaxosDefs.Perms.ADMIN) != 0) {
            p.append('a');
        }
        return p.toString();
    }

    private static List<ACL> parseACLs(String aclString) {
        List<ACL> acl;
        String acls[] = aclString.split(",");
        acl = new ArrayList<ACL>();
        for (String a : acls) {
            int firstColon = a.indexOf(':');
            int lastColon = a.lastIndexOf(':');
            if (firstColon == -1 || lastColon == -1 || firstColon == lastColon) {
                System.err.println(a + " does not have the form scheme:id:perm");
                continue;
            }
            ACL newAcl = new ACL();
            newAcl.setId(new Id(a.substring(0, firstColon), a.substring(
                    firstColon + 1, lastColon)));
            newAcl.setPerms(getPermFromString(a.substring(lastColon + 1)));
            acl.add(newAcl);
        }
        return acl;
    }
}

