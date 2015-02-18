package org.javenstudio.common.indexdb.store;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.IInputSlicer;
import org.javenstudio.common.indexdb.Lock;
import org.javenstudio.common.indexdb.LockFactory;

/** 
 * A delegating Directory that records which files were
 *  written to and deleted. 
 */
public final class TrackingDirectoryWrapper extends Directory implements Closeable {

	private final IDirectory mOther;
	private final Set<String> mCreatedFileNames = 
			Collections.synchronizedSet(new HashSet<String>());

	public TrackingDirectoryWrapper(IDirectory other) {
		mOther = other;
	}

	public final IDirectory getDirectory() { 
		return mOther;
	}
	
	@Override
	public String[] listAll() throws IOException {
		return mOther.listAll();
	}

	@Override
	public boolean fileExists(String name) throws IOException {
		return mOther.fileExists(name);
	}

	@Override
	public void deleteFile(String name) throws IOException {
		mCreatedFileNames.remove(name);
		mOther.deleteFile(name);
	}

	@Override
	public long getFileLength(String name) throws IOException {
		return mOther.getFileLength(name);
	}

	@Override
	public IIndexOutput createOutput(IIndexContext context, String name) throws IOException {
		mCreatedFileNames.add(name);
		return mOther.createOutput(context, name);
	}

	@Override
	protected IndexOutput createIndexOutput(IIndexContext context, String name)
			throws IOException { 
		// do nothing
		return null;
	}
	
	@Override
	public void sync(Collection<String> names) throws IOException {
		mOther.sync(names);
	}

	@Override
	public IIndexInput openInput(IIndexContext context, String name) throws IOException {
		return mOther.openInput(context, name);
	}

	@Override
	protected IndexInput openIndexInput(IIndexContext context, String name) 
			throws IOException { 
		// do nothing
		return null;
	}
	
	@Override
	public Lock makeLock(String name) {
		return mOther.makeLock(name);
	}

	@Override
	public void clearLock(String name) throws IOException {
		mOther.clearLock(name);
	}

	@Override
	public void close() throws IOException {
		mOther.close();
	}

	@Override
	public void setLockFactory(LockFactory lockFactory) throws IOException {
		mOther.setLockFactory(lockFactory);
	}

	@Override
	public LockFactory getLockFactory() {
		return mOther.getLockFactory();
	}

	@Override
	public String getLockID() {
		return mOther.getLockID();
	}

	@Override
	public String toString() {
		return "TrackingDirectoryWrapper(" + mOther.toString() + ")";
	}

	@Override
	public long copy(IIndexContext context, IDirectory to, String src, String dest) throws IOException {
		mCreatedFileNames.add(dest);
		return mOther.copy(context, to, src, dest);
	}

	@Override
	public IInputSlicer createSlicer(final IIndexContext context, final String name) throws IOException {
		return mOther.createSlicer(context, name);
	}

	// maybe clone before returning.... all callers are
	// cloning anyway....
	public Set<String> getCreatedFiles() {
		return mCreatedFileNames;
	}
	
}
