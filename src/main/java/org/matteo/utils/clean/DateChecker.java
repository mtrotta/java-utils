package org.matteo.utils.clean;

import java.util.Date;

public interface DateChecker {
    boolean isDate(Date date) throws CalendarException;

    Date getMinimum(Date date, int maxElaborations) throws CalendarException;
}
