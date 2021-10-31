package ga.epicpix.zprol;

import java.util.ArrayList;
import java.util.ListIterator;

public class SeekIterator<T> implements ListIterator<T> {

    private T[] elements;
    private int index;

    public SeekIterator(ArrayList<T> elements) {
        this.elements = (T[]) elements.toArray(new Object[0]);
    }

    public SeekIterator(T[] elements) {
        this.elements = elements;
    }

    public boolean hasNext() {
        return index < elements.length;
    }

    public T next() {
        return elements[index++];
    }

    public T seek() {
        return elements[index];
    }

    public boolean hasPrevious() {
        return index > 0;
    }

    public T previous() {
        index -= 2;
        return elements[index];
    }
    public T back() {
        index--;
        return elements[index];
    }

    public int nextIndex() {
        return index;
    }

    public int previousIndex() {
        return index - 2;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove()");
    }

    public void set(T t) {
        throw new UnsupportedOperationException("set(T)");
    }

    public void add(T t) {
        throw new UnsupportedOperationException("add(T)");
    }

    public int currentIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
