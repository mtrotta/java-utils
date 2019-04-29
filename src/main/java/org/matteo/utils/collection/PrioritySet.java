package org.matteo.utils.collection;

import java.util.*;

public class PrioritySet<T extends Comparable<T>> implements Set<T> {

    private final Map<T, T> map = new HashMap<>();

    private final Comparator<T> comparator;

    public PrioritySet() {
        this.comparator = Comparator.naturalOrder();
    }

    public PrioritySet(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return map.values().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.values().toArray();
    }

    @Override
    public <A> A[] toArray(A[] a) {
        return map.values().toArray(a);
    }

    @Override
    public boolean add(T t) {
        T existing = map.get(t);
        if (existing == null || comparator.compare(t, existing) < 0) {
            map.put(t, t);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return map.keySet().containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        boolean changed = false;
        for (T t : collection) {
            changed |= add(t);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return map.keySet().retainAll(collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return map.keySet().removeAll(collection);
    }

    @Override
    public void clear() {
        map.clear();
    }

}
