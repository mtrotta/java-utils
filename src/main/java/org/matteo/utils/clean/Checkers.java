package org.matteo.utils.clean;

import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 24/09/12
 */
public enum Checkers {

    YEARLY(new DateChecker() {
        @Override
        public boolean isDate(Date date) throws CalendarException {
            return DateUtility.isLastWorkingDayOfYear(date);
        }

        @Override
        public Date getMinimum(Date date, int maxElaborations) throws CalendarException {
            return DateUtility.getLastWorkingDayOfYear(date, maxElaborations);
        }
    }),

    QUARTERLY(new DateChecker() {
        @Override
        public boolean isDate(Date date) throws CalendarException {
            return DateUtility.isLastWorkingDayOfQuarter(date);
        }

        @Override
        public Date getMinimum(Date date, int maxElaborations) throws CalendarException {
            return DateUtility.getLastWorkingDayOfQuarter(date, maxElaborations);
        }
    }),

    MONTHLY(new DateChecker() {
        @Override
        public boolean isDate(Date date) throws CalendarException {
            return DateUtility.isLastWorkingDayOfMonth(date);
        }

        @Override
        public Date getMinimum(Date date, int maxElaborations) throws CalendarException {
            return DateUtility.getLastWorkingDayOfMonth(date, maxElaborations);
        }
    }),

    WEEKLY(new DateChecker() {
        @Override
        public boolean isDate(Date date) throws CalendarException {
            return DateUtility.isLastWorkingDayOfWeek(date);
        }

        @Override
        public Date getMinimum(Date date, int maxElaborations) throws CalendarException {
            return DateUtility.getLastWorkingDayOfWeek(date, maxElaborations);
        }
    }),

    THURSDAYS(new DateChecker() {
        @Override
        public boolean isDate(Date date) {
            return DateUtility.isDayOfWeek(date, Calendar.THURSDAY);
        }

        @Override
        public Date getMinimum(Date date, int maxElaborations) throws CalendarException {
            return DateUtility.getLastDay(date, Calendar.THURSDAY, maxElaborations);
        }
    }),

    DAILY(new DateChecker() {
        @Override
        public boolean isDate(Date date) {
            return true;
        }

        @Override
        public Date getMinimum(Date date, int maxElaborations) throws CalendarException {
            return DateUtility.getLastWorkingDay(date, maxElaborations);
        }
    });

    private final DateChecker checker;

    Checkers(DateChecker checker) {
        this.checker = checker;
    }

    public CheckerConfiguration with(int maxElaborations) {
        return new CheckerConfiguration(checker, this.ordinal() , maxElaborations);
    }
}
