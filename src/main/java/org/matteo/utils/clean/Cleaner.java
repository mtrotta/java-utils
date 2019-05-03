package org.matteo.utils.clean;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 21/09/12
 */
public class Cleaner {

    private Cleaner() {
    }

    public static <T> List<T> clean(Eraser<T> eraser, Date today, List<CheckerConfiguration> checkers, boolean simulation) throws Exception {

        List<T> deleted = new ArrayList<>();

        Hierarchy ptr = null;

        checkers.sort(Comparator.naturalOrder());

        for (CheckerConfiguration checkerConfiguration : checkers) {
            Hierarchy hierarchy = new Hierarchy(checkerConfiguration.getChecker(), today, checkerConfiguration.getMaxElaborations());
            if (ptr != null) {
                ptr.append(hierarchy);
            }
            ptr = hierarchy;
        }

        if (ptr == null) {
            throw new IllegalArgumentException("Checkers enum is empty!");
        }

        Hierarchy root = ptr.getRoot();

        Collection<Deletable<T>> deletables = eraser.getDeletables();

        for (Deletable deletable : deletables) {
            root.add(deletable.getDate());
        }

        Set<Date> deletableDates = root.getDeletableDates();

        for (Deletable<T> deletable : deletables) {
            if (deletableDates.contains(deletable.getDate())) {
                T object = deletable.getObject();
                if (!simulation) {
                    eraser.erase(object);
                }
                deleted.add(object);
            }
        }

        return deleted;
    }

    private static final class Hierarchy {

        private final DateChecker checker;
        private final Date min;

        private Hierarchy parent;
        private Hierarchy child;

        private final TreeSet<Date> dates = new TreeSet<>(Collections.reverseOrder());

        Hierarchy(DateChecker checker, Date today, int maxElaborations) throws CalendarException {
            this.checker = checker;
            this.min = checker.getMinimum(today, maxElaborations);
        }

        void append(Hierarchy hierarchy) {
            this.child = hierarchy;
            hierarchy.parent = this;
        }

        public void add(Date date) throws CalendarException {
            Hierarchy hierarchy = this;
            while (hierarchy != null && !hierarchy.checker.isDate(date)) {
                hierarchy = hierarchy.child;
            }
            if (hierarchy != null) {
                hierarchy.dates.add(date);
            }
        }

        Set<Date> getDeletableDates() {
            return check(new TreeSet<>(Collections.reverseOrder()));
        }

        private Set<Date> check(Set<Date> deletableDates) {
            Date tail = dates.higher(min);
            if (tail != null) {
                deletableDates.addAll(dates.tailSet(tail));
            }
            if (child != null) {
                child.check(deletableDates);
            }
            return deletableDates;
        }

        boolean contains(Date date) {
            return dates.contains(date) || parentContains(date);
        }

        private boolean parentContains(Date date) {
            return parent != null && parent.contains(date);
        }

        Hierarchy getRoot() {
            Hierarchy hierarchy = this;
            while (hierarchy.parent != null) {
                hierarchy = hierarchy.parent;
            }
            return hierarchy;
        }
    }

}
