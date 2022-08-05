package io.gitbub.devlibx.easy.helper.calendar;

import io.gitbub.devlibx.easy.helper.calendar.KeyGenerator.IKeyFunc;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

public class CalendarMap<T> {

    private Map<String, T> data;

    /**
     * Create a map for calendar days - for last N days
     *
     * @param from starting date
     * @param days last N days
     */
    public CalendarMap(DateTime from, int days) {
        data = new HashMap<>();
        KeyGenerator keyGenerator = new KeyGenerator();
        keyGenerator.generateKeyForLastNDaysFrom(from, days).forEach(key -> data.put(key, null));
    }

    /**
     * Create a map for calendar days (starting from today) - for last N days
     *
     * @param days last N days
     */

    public CalendarMap(int days) {
        this(DateTime.now(), days);
    }

    /**
     * Call function (callback) for given key
     */
    public void executeForKey(String key, Callback<T> processFunc) {
        if (data.containsKey(key)) {
            processFunc.process(key, data.get(key), false);
        } else {
            processFunc.process(key, null, true);
        }
    }

    /**
     * Call function (callback) for given time (will use month and day of the given time for key )
     */
    public void executeForKey(IKeyFunc keyFunc, DateTime time, Callback<T> processFunc) {
        String key = keyFunc.generate(time);
        if (data.containsKey(key)) {
            T t = processFunc.process(key, data.get(key), false);
            data.put(key, t);
        } else {
            processFunc.process(key, null, true);
        }
    }

    /**
     * Call function (callback) for given time (will use month and day of the given time for key )
     */
    public void executeForKey(DateTime time, Callback<T> processFunc) {
        IKeyFunc keyFunc = new IKeyFunc() {
            @Override
            public String generate(DateTime time) {
                return IKeyFunc.super.generate(time);
            }
        };
        executeForKey(keyFunc, time, processFunc);
    }

    public synchronized void executeForAll(Callback<T> processFunc) {
        Map<String, T> newData = new HashMap<>();
        data.forEach((key, value) -> {
            T t = processFunc.process(key, value, false);
            newData.put(key, t);
        });
        data = newData;
    }

    /**
     * Get data for given key
     */
    public T getDataByKey(String key) {
        return data.getOrDefault(key, null);
    }

    /**
     * Get data for given time
     */
    public T getDataByTime(DateTime time) {
        IKeyFunc keyFunc = new IKeyFunc() {
            @Override
            public String generate(DateTime time) {
                return IKeyFunc.super.generate(time);
            }
        };
        String key = keyFunc.generate(time);
        return getDataByKey(key);
    }

    public boolean add(String key, T t) {
        if (data.containsKey(key)) {
            data.put(key, t);
            return true;
        } else {
            return false;
        }
    }

    public boolean add(DateTime time, T t) {
        IKeyFunc keyFunc = new IKeyFunc() {
            @Override
            public String generate(DateTime time) {
                return IKeyFunc.super.generate(time);
            }
        };
        String key = keyFunc.generate(time);
        return add(key, t);
    }

    public interface Callback<T> {
        T process(String key, T t, boolean outOfRange);
    }
}
