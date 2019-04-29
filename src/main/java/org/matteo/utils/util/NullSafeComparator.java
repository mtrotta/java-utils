package org.matteo.utils.util;

import java.util.Comparator;

public class NullSafeComparator<T extends Comparable<T>> implements Comparator<T> {

    public int compare(T t1, T t2) {
        if (t1 != null && t2 != null) {
            return t1.compareTo(t2);
        } else if (t1 != null) {
            return 1;
        } else if (t2 != null) {
            return -1;
        }
        return 0;
    }

}
