package io.gitbub.devlibx.easy.helper.calendar;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class KeyGeneratorTest {

    @Test
    public void testGenerateKeyForLastNDaysFrom() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        DateTime dt = formatter.parseDateTime("10/05/2022 12:11:11");
        DateTime from = new DateTime(new DateTime(dt));

        KeyGenerator keyGenerator = new KeyGenerator();
        List<String> keys = keyGenerator.generateKeyForLastNDaysFrom(from, 5);
        Assertions.assertEquals(5, keys.size());
        for (String s : keys) {
          //  System.out.println(s);
        }

        Assertions.assertEquals("5-6", keys.get(0));
        Assertions.assertEquals("5-7", keys.get(1));
        Assertions.assertEquals("5-8", keys.get(2));
        Assertions.assertEquals("5-9", keys.get(3));
        Assertions.assertEquals("5-10", keys.get(4));
    }

    @Test
    public void testGenerateKeyForLastNDaysFrom_With28Days() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        DateTime dt = formatter.parseDateTime("3/03/2022 12:11:11");
        DateTime from = new DateTime(new DateTime(dt));

        KeyGenerator keyGenerator = new KeyGenerator();
        List<String> keys = keyGenerator.generateKeyForLastNDaysFrom(from, 5);
        Assertions.assertEquals(5, keys.size());
        for (String s : keys) {
            // System.out.println(s);
        }

        Assertions.assertEquals("2-27", keys.get(0));
        Assertions.assertEquals("2-28", keys.get(1));
        Assertions.assertEquals("3-1", keys.get(2));
        Assertions.assertEquals("3-2", keys.get(3));
        Assertions.assertEquals("3-3", keys.get(4));
    }

    @Test
    public void testGenerateKeyForLastNDaysFrom_With29Days() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        DateTime dt = formatter.parseDateTime("3/03/2024 12:11:11");
        DateTime from = new DateTime(new DateTime(dt));

        KeyGenerator keyGenerator = new KeyGenerator();
        List<String> keys = keyGenerator.generateKeyForLastNDaysFrom(from, 5);
        Assertions.assertEquals(5, keys.size());
        for (String s : keys) {
            System.out.println(s);
        }

        Assertions.assertEquals("2-28", keys.get(0));
        Assertions.assertEquals("2-29", keys.get(1));
        Assertions.assertEquals("3-1", keys.get(2));
        Assertions.assertEquals("3-2", keys.get(3));
        Assertions.assertEquals("3-3", keys.get(4));
    }

    @Test
    public void testGenerateKeyForLastNDaysFrom_With29Days_31_days() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        DateTime dt = formatter.parseDateTime("3/03/2024 12:11:11");
        DateTime from = new DateTime(new DateTime(dt));

        KeyGenerator keyGenerator = new KeyGenerator();
        List<String> keys = keyGenerator.generateKeyForLastNDaysFrom(from, 31);
        Assertions.assertEquals(31, keys.size());
        for (String s : keys) {
            // System.out.println(s);
        }

        Assertions.assertEquals("2-2", keys.get(0));
        Assertions.assertEquals("3-3", keys.get(30));
    }

    @Test
    public void testGenerateKeyForLastNDaysFrom_With29Days_35_days() {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        DateTime dt = formatter.parseDateTime("3/03/2024 12:11:11");
        DateTime from = new DateTime(new DateTime(dt));

        KeyGenerator keyGenerator = new KeyGenerator();
        List<String> keys = keyGenerator.generateKeyForLastNDaysFrom(from, 35);
        Assertions.assertEquals(35, keys.size());
        for (String s : keys) {
            // System.out.println(s);
        }

        Assertions.assertEquals("1-29", keys.get(0));
        Assertions.assertEquals("3-3", keys.get(34));
    }
}