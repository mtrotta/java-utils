package org.matteo.utils.clean;

import java.util.*;

public class DateUtility {

    private static final WorkingCalendar workingCalendar = new TargetCalendar();

    private static Calendar getCalendar(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

    public static boolean isLastWorkingDayOfYear(Date date) throws CalendarException {
        return date.equals(getLastWorkingDayOfYear(date));
    }

    public static Date getLastWorkingDayOfYear(Date date) throws CalendarException {
        Calendar calendar = getCalendar(date);
        return getLastWorkingDay(calendar, Calendar.DAY_OF_YEAR);
    }

    public static Date getLastWorkingDayOfYear(Date date, int shift) throws CalendarException {
        Calendar calendar = getCalendar(date);
        calendar.add(Calendar.YEAR, -shift);
        return getLastWorkingDay(calendar, Calendar.DAY_OF_YEAR);
    }

    public static boolean isLastWorkingDayOfQuarter(Date date) throws CalendarException {
        Set<Date> quarters = getQuarters(date);
        return quarters.contains(date);
    }

    private static Set<Date> getQuarters(Date date) throws CalendarException {
        return getMonthFractions(date, Calendar.MARCH, Calendar.JUNE, Calendar.SEPTEMBER, Calendar.DECEMBER);
    }

    public static Date getLastWorkingDayOfQuarter(Date date, int shift) throws CalendarException {
        Calendar calendar = getCalendar(date);
        int ctr = 0;
        Date quarter;
        Iterator<Date> iterator = null;
        do {
            if (iterator == null || !iterator.hasNext()) {
                iterator = getQuarters(calendar.getTime()).iterator();
                calendar.add(Calendar.YEAR, -1);
            }
            quarter = iterator.next();
            if (date.after(quarter)) {
                ctr++;
            }
        } while (ctr < shift);
        return quarter;
    }

    private static Set<Date> getMonthFractions(Date date, int... months) throws CalendarException {
        Calendar calendar = getCalendar(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Set<Date> quarters = new TreeSet<>(Collections.reverseOrder());
        for (int month : months) {
            calendar.set(Calendar.MONTH, month);
            quarters.add(getLastWorkingDayOfMonth(calendar));
        }
        return quarters;
    }

    private static Date getLastWorkingDayOfMonth(Calendar calendar) throws CalendarException {
        return getLastWorkingDay(calendar, Calendar.DAY_OF_MONTH);
    }

    public static Date getLastWorkingDayOfMonth(Date date, int shift) throws CalendarException {
        Calendar calendar = getCalendar(date);
        calendar.add(Calendar.MONTH, -shift);
        return getLastWorkingDay(calendar, Calendar.DAY_OF_MONTH);
    }

    public static boolean isLastWorkingDayOfMonth(Date date) throws CalendarException {
        Calendar calendar = getCalendar(date);
        return date.equals(getLastWorkingDayOfMonth(calendar));
    }

    public static Date getLastWorkingDayOfWeek(Calendar calendar) throws CalendarException {
        return getLastWorkingDay(calendar, Calendar.DAY_OF_WEEK);
    }

    public static Date getLastWorkingDayOfWeek(Date date, int max) throws CalendarException {
        int shift = isLastWorkingDayOfWeek(date) ? max - 1 : max;
        Calendar calendar = getCalendar(date);
        calendar.add(Calendar.WEEK_OF_MONTH, -shift);
        return getLastWorkingDay(calendar, Calendar.DAY_OF_WEEK);
    }

    public static boolean isLastWorkingDayOfWeek(Date date) throws CalendarException {
        Calendar calendar = getCalendar(date);
        return date.equals(getLastWorkingDayOfWeek(calendar));
    }

    private static Date getLastWorkingDay(Calendar calendar, int maximum) throws CalendarException {
        Calendar last = getCalendar(calendar.getTime());
        last.set(maximum, calendar.getActualMaximum(maximum));
        while (!workingCalendar.isWorkingDay(last.getTime())) {
            last.add(Calendar.DAY_OF_YEAR, -1);
        }
        return last.getTime();
    }

    public static Date getLastWorkingDay(Date date, int shift) throws CalendarException {
        Calendar calendar = getCalendar(date);
        int ctr = 0;
        do {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            if (workingCalendar.isWorkingDay(calendar.getTime())) {
                ctr++;
            }
        } while (ctr < shift);
        return calendar.getTime();
    }

    public static Date addWorkingDay(Date date, int rollOffDays) throws CalendarException {
        Calendar calendar = getCalendar(date);
        int ctr = 0;
        int shift = rollOffDays > 0 ? 1 : -1;
        while (Math.abs(ctr) < Math.abs(rollOffDays)) {
            calendar.add(Calendar.DAY_OF_YEAR, shift);
            if (workingCalendar.isWorkingDay(calendar.getTime())) {
                ctr += shift;
            }
        }
        return calendar.getTime();
    }

    public static Date getLastDay(Date date, int day, int shift) {
        Calendar calendar = getCalendar(date);
        int diff = day - calendar.get(Calendar.DAY_OF_WEEK);
        if (diff > 0) {
            diff -= 7;
        }
        calendar.add(Calendar.DAY_OF_MONTH, diff);
        calendar.add(Calendar.WEEK_OF_YEAR, -shift);
        return calendar.getTime();
    }

    public static boolean isDayOfWeek(Calendar calendar, int day) {
        return calendar.get(Calendar.DAY_OF_WEEK) == day;
    }

    public static boolean isDayOfWeek(Date date, int day) {
        return isDayOfWeek(getCalendar(date), day);
    }
}
