package org.matteo.utils.clean;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 21/09/12
 */
public interface Deletable<T> {

    Date getDate();

    T getObject();

}
