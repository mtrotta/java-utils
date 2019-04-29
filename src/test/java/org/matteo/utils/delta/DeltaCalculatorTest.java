package org.matteo.utils.delta;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 04/04/12
 * Time: 19.12
 */
class DeltaCalculatorTest {

    @Test
    void testEmpty() {
        List<FakeDelta> list = new ArrayList<>();
        List<FakeDelta> result = DeltaCalculator.delta(list, list);
        assertEquals(0, result.size());
    }

    @Test
    void testIdentity() {
        List<FakeDelta> list = new ArrayList<>();

        list.add(new FakeDelta("001", 1));
        list.add(new FakeDelta("002", 2));
        list.add(new FakeDelta("003", 3));

        List<FakeDelta> result = DeltaCalculator.delta(list, list);

        assertEquals(3, result.size());

        for (FakeDelta delta : result) {
            assertEquals(0.0, delta.value, 0.0);
            assertEquals(DeltaType.MATCH, delta.getOutcome());
        }
    }

    @Test
    void testSecondEmpty() {
        List<FakeDelta> list = new ArrayList<>();

        list.add(new FakeDelta("001", 0));
        list.add(new FakeDelta("002", 0));
        list.add(new FakeDelta("003", 0));

        List<FakeDelta> result1 = DeltaCalculator.delta(list, new ArrayList<>());

        assertEquals(3, result1.size());

        for (FakeDelta delta : result1) {
            assertEquals(0.0, delta.value, 0.0);
            assertEquals(DeltaType.ADDITIONAL, delta.getOutcome());
        }

        List<FakeDelta> result2 = DeltaCalculator.delta(new ArrayList<>(), list);

        assertEquals(3, result2.size());

        for (FakeDelta delta : result2) {
            assertEquals(0.0, delta.value, 0.0);
            assertEquals(DeltaType.MISSING, delta.getOutcome());
        }
    }

    @Test
    void testAll() {
        List<FakeDelta> list1 = new ArrayList<>();
        list1.add(new FakeDelta("001", 2));
        list1.add(new FakeDelta("003", 3));
        list1.add(new FakeDelta("004", 4));

        List<FakeDelta> list2 = new ArrayList<>();
        list2.add(new FakeDelta("001", 1));
        list2.add(new FakeDelta("002", 2));
        list2.add(new FakeDelta("003", 3));

        List<FakeDelta> result = DeltaCalculator.delta(list1, list2);

        assertEquals(4, result.size());

        FakeDelta test = result.get(0);
        assertEquals("001", test.getKey());
        assertEquals(1.0, test.value, 0.0);
        assertEquals(DeltaType.MATCH, test.getOutcome());

        test = result.get(1);
        assertEquals("002", test.getKey());
        assertEquals(2.0, test.value, 0.0);
        assertEquals(DeltaType.MISSING, test.getOutcome());

        test = result.get(2);
        assertEquals("003", test.getKey());
        assertEquals(0.0, test.value, 0.0);
        assertEquals(DeltaType.MATCH, test.getOutcome());

        test = result.get(3);
        assertEquals("004", test.getKey());
        assertEquals(4.0, test.value, 0.0);
        assertEquals(DeltaType.ADDITIONAL, test.getOutcome());
    }

    @Test
    void testDisjoint() {
        List<FakeDelta> list1 = new ArrayList<>();
        list1.add(new FakeDelta("001", 1));
        list1.add(new FakeDelta("002", 2));
        list1.add(new FakeDelta("003", 3));

        List<FakeDelta> list2 = new ArrayList<>();
        list2.add(new FakeDelta("004", 4));
        list2.add(new FakeDelta("005", 5));
        list2.add(new FakeDelta("006", 6));

        List<FakeDelta> result = DeltaCalculator.delta(list1, list2);

        assertEquals(6, result.size());

        FakeDelta test = result.get(0);
        assertEquals("001", test.getKey());
        assertEquals(1.0, test.value, 0.0);
        assertEquals(DeltaType.ADDITIONAL, test.getOutcome());

        test = result.get(1);
        assertEquals("002", test.getKey());
        assertEquals(2.0, test.value, 0.0);
        assertEquals(DeltaType.ADDITIONAL, test.getOutcome());

        test = result.get(2);
        assertEquals("003", test.getKey());
        assertEquals(3.0, test.value, 0.0);
        assertEquals(DeltaType.ADDITIONAL, test.getOutcome());

        test = result.get(3);
        assertEquals("004", test.getKey());
        assertEquals(4.0, test.value, 0.0);
        assertEquals(DeltaType.MISSING, test.getOutcome());

        test = result.get(4);
        assertEquals("005", test.getKey());
        assertEquals(5.0, test.value, 0.0);
        assertEquals(DeltaType.MISSING, test.getOutcome());

        test = result.get(5);
        assertEquals("006", test.getKey());
        assertEquals(6.0, test.value, 0.0);
        assertEquals(DeltaType.MISSING, test.getOutcome());

    }

    @Test
    void testUnsorted() {

        List<FakeDelta> list1 = new ArrayList<>();
        list1.add(new FakeDelta("001", 1));
        list1.add(new FakeDelta("002", 2));
        list1.add(new FakeDelta("003", 3));

        List<FakeDelta> list2 = new ArrayList<>();
        list2.add(new FakeDelta("003", 3));
        list2.add(new FakeDelta("002", 2));
        list2.add(new FakeDelta("001", 1));

        List<FakeDelta> result = DeltaCalculator.delta(list1, list2);

        assertEquals(3, result.size());

        for (FakeDelta delta : result) {
            assertEquals(0.0, delta.value, 0.0);
            assertEquals(DeltaType.MATCH, delta.getOutcome());
        }
    }

    private static class FakeDelta implements Delta<FakeDelta> {

        private final String key;
        private double value;
        private DeltaType outcome;

        private FakeDelta(String key, double value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void apply(DeltaType type, FakeDelta other) {
            this.outcome = type;
            if (other != null) {
                value -= other.value;
            }
        }

        @Override
        public int compareTo(FakeDelta o) {
            return key.compareTo(o.key);
        }

        String getKey() {
            return key;
        }

        DeltaType getOutcome() {
            return outcome;
        }
    }

}
