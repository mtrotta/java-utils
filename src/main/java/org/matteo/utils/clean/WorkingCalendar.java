package org.matteo.utils.clean;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/12/12
 */
public interface WorkingCalendar {

    boolean isWorkingDay(Date date) throws CalendarException;

}
