package org.matteo.utils.clean;

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
    public String toString() {
        return "CheckerConfiguration{" +
                "checker=" + checker +
                ", priority=" + priority +
                ", maxElaborations=" + maxElaborations +
                '}';
    }
}
