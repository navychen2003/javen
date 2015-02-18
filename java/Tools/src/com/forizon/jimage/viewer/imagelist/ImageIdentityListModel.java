package com.forizon.jimage.viewer.imagelist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.swing.AbstractListModel;

@SuppressWarnings("rawtypes")
public class ImageIdentityListModel
        extends AbstractListModel
        implements List<ImageIdentity>
{
	private static final long serialVersionUID = 1L;
	final List<ImageIdentity> list;

    public ImageIdentityListModel() {
        this.list = new ArrayList<ImageIdentity>(0);
    }

    public ImageIdentityListModel(List<ImageIdentity> list) {
        this.list = new ArrayList<ImageIdentity>(list);
    }

    public void replace(ImageIdentity element, Collection<? extends ImageIdentity> c)
        throws IndexOutOfBoundsException
    {
        int index = indexOf(element);
        if (index >= 0) {
            replace(index, c);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public void replace(int index, Collection<? extends ImageIdentity> c) {
        int size = c.size();
        if (index != list.size()) {
            // Do not modify the list in this case as it should cause a
            // IndexOutOfBounds exception
            list.addAll(index, c);
        }
        list.remove(index + size);
        fireContentsChanged(this, index, index + size);
    }

    public boolean add(ImageIdentity element) {
        boolean result = list.add(element);
        if (result) {
            int index = list.size() - 1;
            fireIntervalAdded(this, index, index);
        }
        return result;
    }

    public void add(int index, ImageIdentity element) {
        list.add(index, element);
        fireIntervalAdded(this, index, index);
    }

    public boolean addAll(Collection<? extends ImageIdentity> c) {
        int size = list.size();
        boolean result = list.addAll(c);
        if (result) {
            fireIntervalAdded(this, size, size + (c.size() - 1));
        }
        return result;
    }

    public boolean addAll(int index, Collection<? extends ImageIdentity> c) {
        boolean result = list.addAll(index, c);
        if (result) {
            fireIntervalAdded(this, index, index + c.size() - 1);
        }
        return result;
    }

    public void clear() {
        int size = list.size();
        if (size > 0) {
            list.clear();
            fireIntervalRemoved(this, 0, size - 1);
        }
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public ImageIdentity get(int index) {
        return list.get(index);
    }

    public ImageIdentity getElementAt(int index) {
        return list.get(index);
    }

    public int getSize() {
        return list.size();
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Iterator<ImageIdentity> iterator() {
        return list.iterator();
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator<ImageIdentity> listIterator() {
        return listIterator(0);
    }

    public ListIterator<ImageIdentity> listIterator(ImageIdentity element) {
        return listIterator(list.indexOf(element));
    }

    public ListIterator<ImageIdentity> listIterator(int index) {
        return list.listIterator(index);
    }

    public boolean remove(Object o) {
        int position = list.indexOf(o);
        boolean result = list.remove(o);
        if (result) {
            fireIntervalRemoved(this, position, position);
        }
        return result;
    }

    public ImageIdentity remove(int index)
            throws IndexOutOfBoundsException
    {
        ImageIdentity result = list.remove(index);
        fireIntervalRemoved(this, index, index);
        return result;
    }

    public boolean removeAll(Collection<?> c) {
        boolean result = list.removeAll(c);
        if (result) {
            fireContentsChanged(this, 0, c.size() - 1);
        }
        return result;
    }

    public boolean retainAll(Collection<?> c) {
        boolean result = list.retainAll(c);
        if (result) {
            fireContentsChanged(this, 0, c.size() - 1);
        }
        return result;
    }

    public ImageIdentity set(int index, ImageIdentity element)
            throws IndexOutOfBoundsException
    {
        ImageIdentity result = list.set(index, element);
        fireContentsChanged(this, index, index);
        return result;
    }

    public int size() {
        return list.size();
    }

    public List<ImageIdentity> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    public ImageIdentity[] toArray() {
        return (ImageIdentity[])list.toArray(new ImageIdentity[0]);
    }

    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }
}

