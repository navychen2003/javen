package org.javenstudio.hornet.store.fst;

public final class CompiledNode implements BuilderNode {
	
    private int mNode = 0;
    
    @Override
    public boolean isCompiled() {
      return true;
    }
    
    public final int getNode() { return mNode; }
    final void setNode(int node) { mNode = node; }
    
}
