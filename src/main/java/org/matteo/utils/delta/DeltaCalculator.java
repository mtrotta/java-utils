package org.matteo.utils.delta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class DeltaCalculator {

    private DeltaCalculator() {
    }

    public static <T extends Delta<T>> List<T> delta(List<T> list1, List<T> list2) {

        list1.sort(Comparator.naturalOrder());
        list2.sort(Comparator.naturalOrder());

        List<T> list = new ArrayList<>();

        Iterator<T> iterator1 = list1.iterator();
        Iterator<T> iterator2 = list2.iterator();
        boolean read1 = iterator1.hasNext();
        boolean read2 = iterator2.hasNext();
        T t1 = null;
        T t2 = null;

        while (read1 || read2 || t1 != null || t2 != null) {
            if (read1) {
                t1 = iterator1.next();
            }
            if (read2) {
                t2 = iterator2.next();
            }

            Result<T> result = delta(t1, t2);

            DeltaType type = result.getDeltaType();
            T main = result.getMain();
            main.apply(type, result.getOther());

            switch (type) {
                case ADDITIONAL:
                    read1 = iterator1.hasNext();
                    read2 = false;
                    t1 = null;
                    break;
                case MISSING:
                    read1 = false;
                    read2 = iterator2.hasNext();
                    t2 = null;
                    break;
                case MATCH:
                    read1 = iterator1.hasNext();
                    read2 = iterator2.hasNext();
                    t1 = t2 = null;
            }

            list.add(main);
        }

        return list;
    }

    private static <T extends Delta<T>> Result<T> delta(T t1, T t2) {
        if (t1 != null && t2 != null) {
            int check = t1.compareTo(t2);
            if (check < 0) {
                return new Result<>(DeltaType.ADDITIONAL, t1);
            } else if (check > 0) {
                return new Result<>(DeltaType.MISSING, t2);
            } else {
                return new Result<>(t1, t2);
            }
        } else if (t1 != null) {
            return new Result<>(DeltaType.ADDITIONAL, t1);
        } else if (t2 != null) {
            return new Result<>(DeltaType.MISSING, t2);
        }
        throw new IllegalStateException("Can't be here, this is a bug!");
    }

    private static class Result<T extends Delta> {

        private final DeltaType deltaType;
        private final T main;
        private final T other;

        Result(DeltaType deltaType, T main) {
            this.deltaType = deltaType;
            this.main = main;
            this.other = null;
        }

        Result(T main, T other) {
            this.deltaType = DeltaType.MATCH;
            this.main = main;
            this.other = other;
        }

        DeltaType getDeltaType() {
            return deltaType;
        }

        T getMain() {
            return main;
        }

        T getOther() {
            return other;
        }
    }

}
