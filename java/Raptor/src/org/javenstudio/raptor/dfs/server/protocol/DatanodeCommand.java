package org.javenstudio.raptor.dfs.server.protocol;

import java.io.*;

import org.javenstudio.raptor.io.*;

public abstract class DatanodeCommand implements Writable {
  static class Register extends DatanodeCommand {
    private Register() {super(DatanodeProtocol.DNA_REGISTER);}
    public void readFields(DataInput in) {}
    public void write(DataOutput out) {}
  }

  static class Finalize extends DatanodeCommand {
    private Finalize() {super(DatanodeProtocol.DNA_FINALIZE);}
    public void readFields(DataInput in) {}
    public void write(DataOutput out) {}
  }

  static {                                      // register a ctor
    WritableFactories.setFactory(Register.class,
        new WritableFactory() {
          public Writable newInstance() {return new Register();}
        });
    WritableFactories.setFactory(Finalize.class,
        new WritableFactory() {
          public Writable newInstance() {return new Finalize();}
        });
  }

  public static final DatanodeCommand REGISTER = new Register();
  public static final DatanodeCommand FINALIZE = new Finalize();

  private int action;
  
  public DatanodeCommand() {
    this(DatanodeProtocol.DNA_UNKNOWN);
  }
  
  DatanodeCommand(int action) {
    this.action = action;
  }

  public int getAction() {
    return this.action;
  }
  
  ///////////////////////////////////////////
  // Writable
  ///////////////////////////////////////////
  public void write(DataOutput out) throws IOException {
    out.writeInt(this.action);
  }
  
  public void readFields(DataInput in) throws IOException {
    this.action = in.readInt();
  }
}
