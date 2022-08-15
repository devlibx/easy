package io.gitbub.devlibx.easy.helper.calendar;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class CalendarUtils {

    /**
     * Example time = 2001-07-04T12:08:56.235+0530
     */
    public static final String DATETIME_FORMAT_V1 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DATETIME_FORMAT_V2 = "yyyy.MM.dd G 'at' HH:mm:ss z";
    public static final String DATETIME_FORMAT_10 = "dd/MM/yyyy HH:mm:ss";

    /**
     * @param inputTime time to be crated with "dd/MM/yyyy HH:mm:ss" format - e.g. "10/05/2022 12:11:11"
     */
    public static DateTime createTime(String format, String inputTime) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
        DateTime dt = formatter.parseDateTime(inputTime);
        return new DateTime(new DateTime(dt));
    }

    /**
     * @param inputTime time to be crated with "dd/MM/yyyy HH:mm:ss" format - e.g. "10/05/2022 12:11:11"
     */
    public static DateTime createTime(String inputTime) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(DATETIME_FORMAT_10);
        DateTime dt = formatter.parseDateTime(inputTime);
        return new DateTime(new DateTime(dt));
    }
}
