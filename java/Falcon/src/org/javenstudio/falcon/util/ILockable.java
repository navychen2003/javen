package org.javenstudio.falcon.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public interface ILockable {

	public static enum Type { 
		READ, WRITE
	}
	
	public static enum Check {
		ALL, TOP, PARENT, CURRENT
	}
	
	public static class Util { 
		public static String toString(Type type) { 
			if (type == null) return null;
			switch (type) { 
			case READ:
				return "ReadLock";
			case WRITE:
				return "WriteLock";
			default:
				return "Lock";
			}
		}
		
		public static String toString(Check check) { 
			if (check == null) return null;
			switch (check) { 
			case ALL:
				return "All";
			case TOP:
				return "Top";
			case PARENT:
				return "Parent";
			case CURRENT:
				return "Current";
			default:
				return "None";
			}
		}
	}
	
	public void lock(Type type, Check check) throws ErrorException;
	public void unlock(Type type) throws ErrorException;
	public boolean isLocked(Type type, boolean currentThread) throws ErrorException;
	
	public static abstract class Lock implements ILockable {
		private static final Logger LOG = Logger.getLogger(Lock.class);
		
		private final ReentrantReadWriteLock mLock =
				new ReentrantReadWriteLock();
		
		public abstract Lock getParentLock();
		public abstract String getName();
		
		@Override
		public final void lock(ILockable.Type type, 
				ILockable.Check check) throws ErrorException { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("lock: name=" + getName() 
						+ " type=" + ILockable.Util.toString(type) 
						+ " check=" + ILockable.Util.toString(check) 
						+ " writeLocked=" + mLock.isWriteLocked() 
						+ " writeLockedByCurrentThread=" + mLock.isWriteLockedByCurrentThread() 
						+ " writeHoldCount=" + mLock.getWriteHoldCount() 
						+ " readHoldCount=" + mLock.getReadHoldCount() 
						+ " readLockCount=" + mLock.getReadLockCount());
			}
			switch (type) { 
			case WRITE:
				if (check != null) {
					checkLocked(ILockable.Type.WRITE, check);
					checkLocked(ILockable.Type.READ, check);
				}
				mLock.writeLock().lock();
				return;
			case READ:
				if (check != null) {
					checkLocked(ILockable.Type.WRITE, check);
				}
				mLock.readLock().lock();
				return;
			default:
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Unsupported Lock type: " + ILockable.Util.toString(type));
			}
		}
		
		@Override
		public final void unlock(ILockable.Type type) 
				throws ErrorException {
			if (LOG.isDebugEnabled()) {
				LOG.debug("unlock: name=" + getName() 
						+ " type=" + ILockable.Util.toString(type) 
						+ " writeLocked=" + mLock.isWriteLocked() 
						+ " writeLockedByCurrentThread=" + mLock.isWriteLockedByCurrentThread() 
						+ " writeHoldCount=" + mLock.getWriteHoldCount() 
						+ " readHoldCount=" + mLock.getReadHoldCount() 
						+ " readLockCount=" + mLock.getReadLockCount());
			}
			switch (type) { 
			case WRITE:
				mLock.writeLock().unlock();
				return;
			case READ:
				mLock.readLock().unlock();
				return;
			default:
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Unsupported lock type: " + ILockable.Util.toString(type));
			}
		}
		
		@Override
		public final boolean isLocked(ILockable.Type type, 
				boolean currentThread) throws ErrorException {
			switch (type) { 
			case WRITE:
				return currentThread ? 
						mLock.isWriteLockedByCurrentThread() : 
						mLock.isWriteLocked();
			case READ:
				return currentThread ? mLock.getReadHoldCount() > 0 : 
						mLock.getReadLockCount() > 0;
			default:
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Unsupported lock type: " + ILockable.Util.toString(type));
			}
		}
		
		public void checkLocked(ILockable.Type type, 
				ILockable.Check check) throws ErrorException { 
			if (check == null) return;
			switch (check) {
			case PARENT: {
				Lock parentLock = getParentLock();
				if (parentLock != null) 
					parentLock.checkLocked(type, check);
				return;
			}
			case TOP: {
				Lock topLock = getTopLock();
				if (topLock != null) 
					topLock.checkLocked(type, check);
				else
					checkCurrent(type, check);
				return;
			}
			case CURRENT: {
				checkCurrent(type, check);
				return;
			}
			case ALL: {
				Lock parentLock = getParentLock();
				if (parentLock != null) 
					parentLock.checkLocked(type, check);
				
				checkCurrent(type, check);
				return;
			}
			default:
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Unsupported lock check: " + ILockable.Util.toString(check));
			}
		}
		
		protected Lock getTopLock() { 
			Lock lock = getParentLock();
			Lock topLock = lock;
			while (lock != null) { 
				topLock = lock;
				lock = lock.getParentLock();
			}
			return topLock;
		}
		
		protected void checkCurrent(ILockable.Type type, ILockable.Check check) 
				throws ErrorException { 
			if (isLocked(type, true) == false && isLocked(type, false)) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						getName() + " is locked by " + ILockable.Util.toString(type) + 
						", check=" + ILockable.Util.toString(check));
			}
		}
		
	}
	
}
