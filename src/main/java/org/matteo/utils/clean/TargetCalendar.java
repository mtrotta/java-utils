package org.matteo.utils.clean;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/12/12
 */
public class TargetCalendar implements WorkingCalendar {

    private static final int[][] holidays = {
        new int[] {1,1},   // New Year Day
        new int[] {5,1},   // Labor Day
        new int[] {12,25}, // Christmas
        new int[] {12,26}, // Family Day
    };

    @Override
    public boolean isWorkingDay(Date date) throws CalendarException {
        return isWorkingDay(getCalendar(date));
    }

    private static boolean isWorkingDay(Calendar calendar) throws CalendarException {
        Set<Calendar> targetCalendar = getTargetCalendar(calendar.get(Calendar.YEAR));
        if (targetCalendar == null) {
            throw new CalendarException("Unable to find a suitable calendar");
        }
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.SATURDAY && !targetCalendar.contains(calendar);
    }

    private static Set<Calendar> getTargetCalendar(int... years) {
        Set<Calendar> targetCalendar = new TreeSet<>();

        for(int year : years) {

            for (int[] holiday : holidays) {
                GregorianCalendar calendar = new GregorianCalendar();
                calendar.set(year, holiday[0], holiday[1]);
                targetCalendar.add(calendar);
            }

            Date easter = findEaster(year);

            GregorianCalendar goodFriday = new GregorianCalendar();
            goodFriday.setTime(easter);
            goodFriday.add(Calendar.DAY_OF_YEAR, -2);

            Calendar easterMonday = new GregorianCalendar();
            easterMonday.setTime(easter);
            easterMonday.add(Calendar.DAY_OF_YEAR, 1);

            targetCalendar.add(goodFriday);
            targetCalendar.add(easterMonday);
        }

        return targetCalendar.isEmpty() ? null : targetCalendar;
    }

    @SuppressWarnings("unused")
    private static boolean isEaster(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        int year = calendar.get(Calendar.YEAR);
        int dateYMD = year * 10000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DAY_OF_MONTH);
        Date easter = findEaster(year);
        calendar.setTime(easter);
        int easterYMD = year * 10000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DAY_OF_MONTH);
        return (easterYMD == dateYMD);
    }

    private static Date findEaster(int year) {
        if((year < 1573) || (year > 2499)) {
            throw new IllegalArgumentException("invalid year for easter: " + year);
        }

        int a = year % 19;
        int b = year % 4;
        int c = year % 7;

        int m = 0;
        int n = 0;

        if((year >= 1900) && (year <= 2099)) {
            m = 24;
            n = 5;
        } else if((year >= 2100) && (year <= 2199)) {
            m = 24;
            n = 6;
        } else if((year >= 1583) && (year <= 1699)) {
            m = 22;
            n = 2;
        } else if((year >= 1700) && (year <= 1799)) {
            m = 23;
            n = 3;
        } else if((year >= 1800) && (year <= 1899)) {
            m = 23;
            n = 4;
        } else if((year >= 2200) && (year <= 2299)) {
            m = 25;
            n = 0;
        } else if((year >= 2300) && (year <= 2399)) {
            m = 26;
            n = 1;
        } else if((year >= 2400)) {
            m = 25;
            n = 1;
        }

        int d = (19 * a + m) % 30;
        int e = (2 * b + 4 * c + 6 * d + n) % 7;

        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, year);

        if(d + e < 10) {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, Calendar.MARCH);
            calendar.set(Calendar.DAY_OF_MONTH, d + e + 22);
        } else {
            calendar.set(Calendar.MONTH, Calendar.APRIL);
            int day = d + e - 9;
            if(26 == day) {
                day = 19;
            } else if((25 == day) && (28 == d) && (e == 6) && (a > 10)) {
                day = 18;
            }
            calendar.set(Calendar.DAY_OF_MONTH, day);
        }

        return calendar.getTime();
    }

    private static Calendar getCalendar(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

}
