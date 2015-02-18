package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class FlushQueue {
	
	private final Queue<FlushTicket> mQueue = new LinkedList<FlushTicket>();
	// we track tickets separately since count must be present even before the ticket is
	// constructed ie. queue.size would not reflect it.
	private final AtomicInteger mTicketCount = new AtomicInteger();
	private final ReentrantLock mPurgeLock = new ReentrantLock();

	public void addDeletesAndPurge(SegmentWriter writer, DeleteQueue deleteQueue) 
			throws IOException {
		synchronized (this) {
			// first inc the ticket count - freeze opens
            // a window for #anyChanges to fail
			increaseTickets();
			
			boolean success = false;
			try {
				mQueue.add(new FlushTicket(deleteQueue.freezeGlobalBuffer(null), true));
				success = true;
			} finally {
				if (!success) 
					decreaseTickets();
			}
		}
		
		// don't hold the lock on the FlushQueue when forcing the purge - this blocks and deadlocks 
		// if we hold the lock.
		forcePurge(writer);
	}
  
	private void increaseTickets() {
		int numTickets = mTicketCount.incrementAndGet();
		assert numTickets > 0;
	}
  
	private void decreaseTickets() {
		int numTickets = mTicketCount.decrementAndGet();
		assert numTickets >= 0;
	}

	public synchronized FlushTicket addFlushTicket(DocumentWriter dwpt) {
		// Each flush is assigned a ticket in the order they acquire the ticketQueue
		// lock
		increaseTickets();
		
		boolean success = false;
		try {
			// prepare flush freezes the global deletes - do in synced block!
			final FlushTicket ticket = new FlushTicket(dwpt.prepareFlush());
			mQueue.add(ticket);
			success = true;
			
			return ticket;
			
		} finally {
			if (!success) 
				decreaseTickets();
		}
	}
  
	public synchronized void addSegment(FlushTicket ticket, FlushedSegment segment) {
		// the actual flush is done asynchronously and once done the FlushedSegment
		// is passed to the flush ticket
		ticket.setSegment(segment);
	}

	public synchronized void markTicketFailed(FlushTicket ticket) {
		// to free the queue we mark tickets as failed just to clean up the queue.
		ticket.setFailed();
	}

	public boolean hasTickets() {
		assert mTicketCount.get() >= 0 : "ticketCount should be >= 0 but was: " + mTicketCount.get();
		return mTicketCount.get() != 0;
	}

	private void innerPurge(SegmentWriter writer) throws IOException {
		assert mPurgeLock.isHeldByCurrentThread();
		
		while (true) {
			final FlushTicket head;
			final boolean canPublish;
			synchronized (this) {
				head = mQueue.peek();
				canPublish = head != null && head.canPublish(); // do this synced 
			}
			
			if (canPublish) {
				try {
					/**
					 * if we bock on publish -> lock IW -> lock BufferedDeletes we don't block
					 * concurrent segment flushes just because they want to append to the queue.
					 * the downside is that we need to force a purge on fullFlush since ther could
					 * be a ticket still in the queue. 
					 */
					head.publish(writer);
				} finally {
					synchronized (this) {
						// finally remove the publised ticket from the queue
						final FlushTicket poll = mQueue.poll();
						mTicketCount.decrementAndGet();
						assert poll == head;
					}
				}
			} else {
				break;
			}
		}
	}

	public void forcePurge(SegmentWriter writer) throws IOException {
		assert !Thread.holdsLock(this);
		
		mPurgeLock.lock();
		try {
			innerPurge(writer);
		} finally {
			mPurgeLock.unlock();
		}
	}

	public void tryPurge(SegmentWriter writer) throws IOException {
		assert !Thread.holdsLock(this);
		
		if (mPurgeLock.tryLock()) {
			try {
				innerPurge(writer);
			} finally {
				mPurgeLock.unlock();
			}
		}
	}

	public synchronized void clear() {
		mQueue.clear();
		mTicketCount.set(0);
	}
	
}
