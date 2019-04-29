package org.matteo.utils.collection;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrioritySetTest {

    @Test
    void testAdd() {
        Set<Mock> prioritySet = new PrioritySet<>();

        String id1 = "ID1";
        assertTrue(prioritySet.add(new Mock(id1, 10)));
        assertTrue(prioritySet.add(new Mock(id1, 9)));
        Mock bestId1 = new Mock(id1, 8);
        assertTrue(prioritySet.add(bestId1));
        assertFalse(prioritySet.add(new Mock(id1, 15)));

        String id2 = "ID2";
        assertTrue(prioritySet.add(new Mock(id2, 10)));
        assertFalse(prioritySet.add(new Mock(id2, 11)));
        Mock bestId2 = new Mock(id2, -1);
        assertTrue(prioritySet.add(bestId2));

        assertEquals(2, prioritySet.size());

        for (Mock mock : prioritySet) {
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