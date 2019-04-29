package org.matteo.utils.delta;

public interface Delta<T> extends Comparable<T> {

    void apply(DeltaType type, T other);

}