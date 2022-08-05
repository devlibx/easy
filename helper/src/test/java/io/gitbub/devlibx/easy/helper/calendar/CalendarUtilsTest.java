package io.gitbub.devlibx.easy.helper.calendar;


import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CalendarUtilsTest {

    @Test
    public void testCalenderUtil() {
        DateTime out = CalendarUtils.createTime(CalendarUtils.DATETIME_FORMAT_V2, "2001.07.04 AD at 12:08:56 PDT");
        Assertions.assertEquals(1, out.getYearOfCentury());
        Assertions.assertEquals(7, out.getMonthOfYear());
        Assertions.assertEquals(4, out.getDayOfMonth());

        out = CalendarUtils.createTime(CalendarUtils.DATETIME_FORMAT_V1, "2001-07-04T12:08:56.235+0530");
        Assertions.assertEquals(1, out.getYearOfCentury());
        Assertions.assertEquals(7, out.getMonthOfYear());
        Assertions.assertEquals(4, out.getDayOfMonth());
    }
}