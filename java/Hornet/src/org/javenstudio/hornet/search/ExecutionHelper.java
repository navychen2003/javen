package org.javenstudio.hornet.search;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

import org.javenstudio.common.indexdb.ThreadInterruptedException;

/**
 * A helper class that wraps a {@link CompletionService} and provides an
 * iterable interface to the completed {@link Callable} instances.
 * 
 * @param <T> the type of the {@link Callable} return value
 */
final class ExecutionHelper<T> implements Iterator<T>, Iterable<T> {
	
	private final CompletionService<T> mService;
	private int mNumTasks;

	ExecutionHelper(final Executor executor) {
		mService = new ExecutorCompletionService<T>(executor);
	}

	@Override
	public boolean hasNext() {
		return mNumTasks > 0;
	}

	public void submit(Callable<T> task) {
		mService.submit(task);
		++ mNumTasks;
	}

	@Override
	public T next() {
		if (!this.hasNext())
			throw new NoSuchElementException();
		try {
			return mService.take().get();
		} catch (InterruptedException e) {
			throw new ThreadInterruptedException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		} finally {
			-- mNumTasks;
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() {
		// use the shortcut here - this is only used in a private context
		return this;
	}
	
}
