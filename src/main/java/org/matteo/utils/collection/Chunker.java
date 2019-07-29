package org.matteo.utils.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class Chunker {

    public static <T> void process(Iterable<T> iterable, Function<T, Object> keyExtractor, Consumer<Collection<T>> onChunk) {
        final Iterator<T> iterator = iterable.iterator();
        Object previousKey = null;
        Collection<T> chunk = new ArrayList<>();
        while (iterator.hasNext()) {
            final T item = iterator.next();
            final Object key = keyExtractor.apply(item);
            if (previousKey == null || previousKey.equals(key)) {
                chunk.add(item);
            } else {
                onChunk.accept(chunk);
                chunk = new ArrayList<>();
                chunk.add(item);
            }
            previousKey = key;
        }
        if (!chunk.isEmpty()) {
            onChunk.accept(chunk);
        }
    }
}
