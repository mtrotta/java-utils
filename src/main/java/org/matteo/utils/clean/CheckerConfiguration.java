package org.matteo.utils.clean;

import java.util.Objects;

public class CheckerConfiguration implements Comparable<CheckerConfiguration> {

    private final DateChecker checker;
    private final int priority;
    private final int maxElaborations;

    public CheckerConfiguration(DateChecker checker, int priority, int maxElaborations) {
        this.checker = checker;
        this.priority = priority;
        this.maxElaborations = maxElaborations;
    }

    public DateChecker getChecker() {
        return checker;
    }

    public int getMaxElaborations() {
        return maxElaborations;
    }

    @Override
    public int compareTo(CheckerConfiguration o) {
        return Integer.compare(priority, o.priority);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckerConfiguration that = (CheckerConfiguration) o;
        return priority == that.priority &&
                maxElaborations == that.maxElaborations &&
                checker.equals(that.checker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checker, priority, maxElaborations);
    }

    @Override
    public String toString() {
        return "CheckerConfiguration{" +
                "checker=" + checker +
                ", priority=" + priority +
                ", maxElaborations=" + maxElaborations +
                '}';
    }
}
