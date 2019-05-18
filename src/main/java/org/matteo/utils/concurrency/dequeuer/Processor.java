package org.matteo.utils.concurrency.dequeuer;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 07/12/12
 */
public interface Processor<T> {

    void process(T t) throws Exception;

}
