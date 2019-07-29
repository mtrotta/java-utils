package org.matteo.utils.collection;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ChunkerTest {

    @Test
    void testEmpty() {
        Chunker.process(Collections.emptyList(), Function.identity(), c -> fail());
    }

    @Test
    void testOne() {
        List<Collection<Object>> chunks = new ArrayList<>();
        Chunker.process(Collections.singletonList("1"), Function.identity(), chunks::add);
        assertEquals(1, chunks.size());
    }

    @Test
    void testMany() {
        List<Collection<Object>> chunks = new ArrayList<>();
        Chunker.process(Arrays.asList("1", "2", "2", "3","3","3"), Function.identity(), chunks::add);
        assertEquals(3, chunks.size());
        assertEquals(1, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(3, chunks.get(2).size());
    }

    @Test
    void testLast() {
        List<Collection<Object>> chunks = new ArrayList<>();
        Chunker.process(Arrays.asList("1", "2", "2", "3"), Function.identity(), chunks::add);
        assertEquals(3, chunks.size());
        assertEquals(1, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

}