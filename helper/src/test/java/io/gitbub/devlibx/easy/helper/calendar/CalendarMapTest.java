package io.gitbub.devlibx.easy.helper.calendar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CalendarMapTest {

    @Test
    public void tesCalendarMap() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        DateTime dt = formatter.parseDateTime("10/05/2022 12:11:11");
        DateTime from = new DateTime(new DateTime(dt));

        CalendarMap<Container> cm = new CalendarMap<Container>(from, 10);
        int ac = cm.getData().size();
        DateTime rr = from.minusDays(10);
        Assertions.assertTrue(cm.add(from, new Container(10)));
        Assertions.assertTrue(cm.add(from.minusDays(9), new Container(11)));
        Assertions.assertFalse(cm.add(from.minusDays(10), new Container(12)));

        cm.executeForKey(from.minusDays(10), (key, container, outOfRange) -> {
            if (!outOfRange) {
                Assertions.fail("I expected out of range");
            }
            return null;
        });

        cm.executeForKey(from.minusDays(9), (key, container, outOfRange) -> {
            if (outOfRange) {
                Assertions.fail("I expected not out of range");
            }
            return new Container(container.count + 1);
        });

        Container c = cm.getDataByTime(from.minusDays(9));
        Assertions.assertNotNull(c);
        Assertions.assertEquals(12, c.count);
    }

    @Test
    public void testThisMonth() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        DateTime dt = formatter.parseDateTime("10/05/2022 12:11:11");
        DateTime from = new DateTime(new DateTime(dt));

        CalendarMap<Container> cm = CalendarMap.forMonth(from, Container.class);
        Assertions.assertTrue(cm.getData().containsKey("5-1"));
        Assertions.assertTrue(cm.getData().containsKey("5-10"));
        Assertions.assertEquals(10, cm.getData().size());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Container {
        private int count;
    }
}