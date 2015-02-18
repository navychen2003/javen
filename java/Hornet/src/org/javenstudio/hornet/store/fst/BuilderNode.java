package org.javenstudio.hornet.store.fst;

// NOTE: not many instances of Node or CompiledNode are in
// memory while the FST is being built; it's only the
// current "frontier":
public interface BuilderNode {

	boolean isCompiled();
	
}
