package com.forizon.jimage.viewer.imagelist;

import java.util.EventObject;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author David
 */
@SuppressWarnings("rawtypes")
public class ImageIdentityListModelIterator
        implements ListIterator<ImageIdentity>
{
    int position;
    boolean looping;
    final ImageIdentityListModel wrapped;
    final EventListenerList listenerList;

    public ImageIdentityListModelIterator(ImageIdentityListModel wrapped) {
        this.wrapped = wrapped;
        this.wrapped.addListDataListener(new ListModelChangeListener());
        listenerList = new EventListenerList();
    }

    @Override
    public void add(ImageIdentity e) {
        throw new UnsupportedOperationException();
    }

    public void addIteratorListener(IteratorListener listener) {
        listenerList.add(IteratorListener.class, listener);
    }

    public void removeIteratorListener(IteratorListener listener) {
        listenerList.remove(IteratorListener.class, listener);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type.  The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * Implementation based on:
     * http://java.sun.com/javase/6/docs/api/javax/swing/event/EventListenerList.html
     */
    protected void firePositionChange(int oldValue, int newValue) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        // Unlike the sourced-code, there should always be at least one listener
        PositionChangeEvent event = new PositionChangeEvent(oldValue, newValue);
        for (int i = 1; i < listeners.length; i += 2) {
            ((IteratorListener)listeners[i]).positionChange(event);
        }
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type.  The event instance is lazily created using the
     * parameters passed into the fire method.
     *
     * Implementation based on:
     * http://java.sun.com/javase/6/docs/api/javax/swing/event/EventListenerList.html
     */
    protected void fireNextChange(ImageIdentity oldValue, ImageIdentity newValue) {
        if (oldValue != newValue) {
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            // Process the listeners last to first, notifying
            // those that are interested in this event
            // Unlike the sourced-code, there should always be at least one listener
            NextChangeEvent event = new NextChangeEvent(oldValue, newValue);
            for (int i = 1; i < listeners.length; i += 2) {
                ((IteratorListener)listeners[i]).nextChange(event);
            }
        }
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type.  The event instance is lazily created using the
     * parameters passed into the fire method.
     *
     * Implementation based on:
     * http://java.sun.com/javase/6/docs/api/javax/swing/event/EventListenerList.html
     */
    protected void firePreviousChange(ImageIdentity oldValue, ImageIdentity newValue) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        // Unlike the sourced-code, there should always be at least one listener
        PreviousChangeEvent event = new PreviousChangeEvent(oldValue, newValue);
        for (int i = 1; i < listeners.length; i += 2) {
            ((IteratorListener)listeners[i]).previousChange(event);
        }
    }

    public synchronized ImageIdentity peekPrevious() {
        try {
            return wrapped.get(previousIndex());
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    public synchronized ImageIdentity peekNext() {
        try {
            return wrapped.get(nextIndex());
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    public synchronized boolean hasNext(int increment) {
        int size = wrapped.size();
        return size > 0 && (looping || (position + increment < size));
    }

    public synchronized boolean hasNext () {
        return hasNext(1);
    }

    public synchronized boolean hasPrevious(int decrement) {
        return wrapped.size() > 0 && (looping || (position - decrement >= 0));
    }

    public synchronized boolean hasPrevious() {
        return hasPrevious(1);
    }

    public synchronized int nextIndex() {
        return (looping && position >= wrapped.size())? 0 : position;
    }

    public synchronized int previousIndex() {
        int result = position - 1;
        return (looping && result < 0)? wrapped.size() - 1 : result;
    }

    public synchronized ImageIdentity next()
        throws NoSuchElementException
    {
        int newPosition = position + 1;
        if (looping && newPosition >= wrapped.size()) {
            newPosition = 0;
        }
        setPosition(newPosition);
        try {
            return wrapped.get(newPosition);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    public synchronized ImageIdentity previous()
        throws NoSuchElementException
    {
        int newPosition = previousIndex();
        setPosition(newPosition);
        try {
            return wrapped.get(newPosition);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public synchronized void set(ImageIdentity e)
        throws NoSuchElementException
    {
        int index = wrapped.indexOf(e);
        if (index >= 0) {
            setPosition(index);
        } else {
            throw new NoSuchElementException();
        }
    }

    public synchronized void setPosition(int position)
        throws IndexOutOfBoundsException
    {
        if (this.position != position) {
            if (position >= 0 && position <= wrapped.getSize()) {
                int old = this.position;
                ImageIdentity oldPrevious, oldNext;
                try {
                    oldPrevious = peekPrevious();
                } catch (NoSuchElementException e) {
                    oldPrevious = null;
                }
                try {
                    oldNext = peekNext();
                } catch (NoSuchElementException e) {
                    oldNext = null;
                }
                this.position = position;
                firePositionChange(old, position);
                try {
                    firePreviousChange(oldPrevious, peekPrevious());
                } catch (NoSuchElementException e) {
                    firePreviousChange(oldPrevious, null);
                }
                try {
                    fireNextChange(oldNext, peekNext());
                } catch (NoSuchElementException e) {
                    fireNextChange(oldNext, null);
                }
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    public synchronized void setLooping(boolean looping) {
        ImageIdentity oldPrevious = (hasPrevious())? peekPrevious()
                                                   : (ImageIdentity)null;
        ImageIdentity oldNext = (hasNext())? peekNext()
                                           : (ImageIdentity)null;
        this.looping = looping;
        if (position == 0) {
            if (hasPrevious()) {
                firePreviousChange(oldPrevious, peekPrevious());
            } else {
                firePreviousChange(oldPrevious, (ImageIdentity)null);
            }
        }
        if (position + 1 == wrapped.size()) {
            if (hasNext(2)) {
                fireNextChange(oldNext, peekNext());
            } else {
                fireNextChange(oldNext, (ImageIdentity)null);
            }
        }
    }

    public synchronized boolean isLooping() {
        return looping;
    }

    public class PositionChangeEvent extends EventObject {
		private static final long serialVersionUID = 1L;
		final int oldValue, newValue;

        public PositionChangeEvent(int oldValue, int newValue) {
            super(ImageIdentityListModelIterator.this);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public int getNewValue() {
            return newValue;
        }

        public int getOldValue() {
            return oldValue;
        }
    }

    public class NextChangeEvent extends EventObject {
		private static final long serialVersionUID = 1L;
		final ImageIdentity oldValue, newValue;

        public NextChangeEvent(ImageIdentity oldValue, ImageIdentity newValue) {
            super(ImageIdentityListModelIterator.this);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public ImageIdentity getNewValue() {
            return newValue;
        }

        public ImageIdentity getOldValue() {
            return oldValue;
        }
    }

    public class PreviousChangeEvent extends EventObject {
		private static final long serialVersionUID = 1L;
		final ImageIdentity oldValue, newValue;

        public PreviousChangeEvent(ImageIdentity oldValue, ImageIdentity newValue) {
            super(ImageIdentityListModelIterator.this);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public ImageIdentity getNewValue() {
            return newValue;
        }

        public ImageIdentity getOldValue() {
            return oldValue;
        }
    }

    class ListModelChangeListener implements ListDataListener {
        public void contentsChanged(ListDataEvent e) {
            if (position > e.getIndex0() && e.getIndex1() <= position) {
                setPosition(e.getIndex0());
            } else if (position == e.getIndex0()) {
                fireNextChange((ImageIdentity)null, peekNext());
            } else if (e.getIndex1() + 1 == position) {
                firePreviousChange((ImageIdentity)null, peekNext());
            }
        }

        public void intervalAdded(ListDataEvent e) {
            if (position > e.getIndex0() && e.getIndex1() <= position) {
                setPosition(e.getIndex0());
            } else if (position == e.getIndex0()) {
                fireNextChange((ImageIdentity)null, peekNext());
            } else if (position > e.getIndex0()) {
                setPosition(position + ((e.getIndex1() - e.getIndex0()) + 1));
            }
        }

        public void intervalRemoved(ListDataEvent e) {
            if (position > e.getIndex0() && position <= e.getIndex1()) {
                setPosition(e.getIndex0());
            } else if (position == e.getIndex0()) {
                ImageIdentity next = (hasNext())? peekNext()
                                                : (ImageIdentity)null;
                fireNextChange((ImageIdentity)null, next);
            } else if (position > e.getIndex0()) {
                int newPosition = position - ((e.getIndex1() - e.getIndex0()) + 1);
                if (newPosition >= 0) {
                    setPosition(newPosition);
                } else {
                    setPosition(e.getIndex0());
                }
            }
        }
    }
}
