package io.gitbub.devlibx.easy.helper.calendar;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class KeyGenerator {

    /**
     * Generate key for last N days. e.g. if you gave a date of from=2022-07-10, days=5; then it will return
     * <p>
     * ["7-10", "7-9", "7-8", "7-7", "7-7"]
     */
    public List<String> generateKeyForLastNDaysFrom(DateTime from, int days) {
        return generateKeyForLastNDaysFrom(from, days, new IKeyFunc() {
            @Override
            public String generate(DateTime time) {
                return IKeyFunc.super.generate(time);
            }
        });
    }

    /**
     * Generate key for last N days. e.g. if you gave a date of from=2022-07-10, days=5; then it will return
     * <p>
     * ["7-10", "7-9", "7-8", "7-7", "7-7"]
     */
    public List<String> generateKeyForLastNDaysFrom(DateTime from, int days, IKeyFunc keyFunc) {
        List<String> keys = new ArrayList<>();
        DateTime to = from.minusDays(days);
        while (to.isBefore(from)) {
            to = to.plusDays(1);
            keys.add(keyFunc.generate(to));
        }
        return keys;
    }

    public interface IKeyFunc {
        default String generate(DateTime time) {
            int month = time.getMonthOfYear();
            int day = time.getDayOfMonth();
            return month + "-" + day;
        }
    }
}
