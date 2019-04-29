package org.matteo.utils.clean;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 18/12/12
 */
public interface Eraser<T> {

    void erase(T t) throws Exception;

    Collection<Deletable<T>> getDeletables();

}
