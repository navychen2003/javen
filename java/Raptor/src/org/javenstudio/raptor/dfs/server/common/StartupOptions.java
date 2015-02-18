package org.javenstudio.raptor.dfs.server.common;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.util.Strings; 


public abstract class StartupOptions {

  static {
    Strings.addStrings(StartupOptions.class.getPackage().getName()); 
  }

  private final List<String> mOptions = new ArrayList<String>();
  
  public final void parse(Configuration conf, String[] argv) { 
	if (argv == null) return;
	
	synchronized (mOptions) { 
	  for (int i=0; i < argv.length; i++) { 
		String arg = argv[i];
		mOptions.add(arg);
	  }
	}
  }
  
  public final boolean hasOption(String option) { 
	if (option == null || option.length() == 0)
	  return false;
	
	if (!option.startsWith("-"))
	  option = "-" + option;
	
	synchronized (mOptions) { 
      for (String opt : mOptions) { 
    	if (option.equals(opt)) return true;
      }
	}
	
	return false;
  }
  
  public static class NamenodeOptions extends StartupOptions {
    public static final String SINGLED = "singled";
    public static final String FORMAT = "format"; 
    public static final String REGULAR = "regular"; 
    public static final String UPGRADE = "upgrade"; 
    public static final String ROLLBACK = "rollback"; 
    public static final String FINALIZE = "finalize"; 
    public static final String IMPORT = "importCheckpoint"; 

    public static final String NODEADDRESS = "nodeAddress"; 
    public static final String NAMENODEADDRESS = "namenodeAddress"; 
    public static final String THREADCOUNT = "threadCount"; 
    public static final String HTTPADDRESS = "httpAddress"; 
    public static final String NAMEDIRS = "nameDirs";
    public static final String EDITDIRS = "editDirs";

    public NamenodeOptions() {
      //super(NameNode.class); 
    }

    public void addOptions() {
      //addBooleanOption(SINGLED, Strings.get("namenode.singled"));
      //addCommandOption(FORMAT, Strings.get("format.filesystem")); 
      //addCommandOption(REGULAR, Strings.get("regular.startup")); 
      //addCommandOption(UPGRADE, Strings.get("upgrade.filesystem")); 
      //addCommandOption(ROLLBACK, Strings.get("rollback.filesystem")); 
      //addCommandOption(FINALIZE, Strings.get("finalize.filesystem")); 
      //addCommandOption(IMPORT, Strings.get("import.checkpoint")); 

      //addArgumentOption(NODEADDRESS, "address:port", Strings.get("node.addressport")); 
      //addArgumentOption(NAMENODEADDRESS, "address:port", Strings.get("namenode.bindaddress")); 
      //addArgumentOption(THREADCOUNT, "num", Strings.get("namenode.threadcount")); 
      //addArgumentOption(HTTPADDRESS, "address:port", Strings.get("http.bindaddress")); 
      //addArgumentOption(NAMEDIRS, "dir,dir..", Strings.get("namenode.name.dirs"));
      //addArgumentOption(EDITDIRS, "dir,dir..", Strings.get("namenode.edit.dirs"));
    }
  }

  public static NamenodeOptions getNamenodeOptions() {
    NamenodeOptions options = new NamenodeOptions(); 
    return options; 
  }

  public static class DatanodeOptions /*extends ServiceOptions*/ {
    public static final String SINGLED = "singled";
    public static final String REGULAR = "regular";
    public static final String ROLLBACK = "rollback";

    public static final String NODEADDRESS = "nodeAddress";
    public static final String NAMENODEADDRESS = "namenodeAddress";
    public static final String DATANODEADDRESS = "datanodeAddress";
    public static final String IPCADDRESS = "ipcAddress";
    public static final String THREADCOUNT = "threadCount";
    public static final String HTTPADDRESS = "httpAddress";
    public static final String DATADIRS = "dataDirs";

    public DatanodeOptions() {
      //super(DataNode.class);
    }

    public void addOptions() {
      //addBooleanOption(SINGLED, Strings.get("datanode.singled"));
      //addCommandOption(REGULAR, Strings.get("regular.startup"));
      //addCommandOption(ROLLBACK, Strings.get("rollback.filesystem"));

      //addArgumentOption(NODEADDRESS, "address:port", Strings.get("node.addressport"));
      //addArgumentOption(NAMENODEADDRESS, "address:port", Strings.get("namenode.addressport"));
      //addArgumentOption(DATANODEADDRESS, "address:port", Strings.get("datanode.bindaddress"));
      //addArgumentOption(IPCADDRESS, "address:port", Strings.get("datanode.ipc.bindaddress"));
      //addArgumentOption(THREADCOUNT, "num", Strings.get("datanode.threadcount"));
      //addArgumentOption(HTTPADDRESS, "address:port", Strings.get("http.bindaddress"));
      //addArgumentOption(DATADIRS, "dir,dir..", Strings.get("datanode.data.dirs"));
    }
  }

  public static DatanodeOptions getDatanodeOptions() {
    DatanodeOptions options = new DatanodeOptions();
    return options; 
  }

}
