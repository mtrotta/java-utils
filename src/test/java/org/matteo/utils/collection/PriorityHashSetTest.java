package org.matteo.utils.collection;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PriorityHashSetTest {

    @Test
    void testAdd() {
        Set<Mock> set = new PriorityHashSet<>();

        String id1 = "ID1";
        assertTrue(set.add(new Mock(id1, 10)));
        assertTrue(set.add(new Mock(id1, 9)));
        Mock bestId1 = new Mock(id1, 8);
        assertTrue(set.add(bestId1));
        assertFalse(set.add(new Mock(id1, 15)));

        String id2 = "ID2";
        assertTrue(set.add(new Mock(id2, 10)));
        assertFalse(set.add(new Mock(id2, 11)));
        Mock bestId2 = new Mock(id2, -1);
        assertTrue(set.add(bestId2));

        assertEquals(2, set.size());

        for (Mock mock : set) {
            if (id1.equals(mock.id)) {
                assertSame(mock, bestId1);
            } else if (id2.equals(mock.id)) {
                assertSame(mock, bestId2);
            }
        }
    }

    private static final Comparator<Mock> COMPARATOR = Comparator.comparing(Mock::getId).thenComparing(Mock::getPriority);

    private static class Mock implements Comparable<Mock> {
        private final String id;
        private final int priority;

        Mock(String id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        String getId() {
            return id;
        }

        int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(Mock o) {
            return COMPARATOR.compare(this, o);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Mock mock = (Mock) o;

            return id.equals(mock.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}