package org.mili.amqp;

import java.util.HashMap;

/**
 * ThreadContext file
 */
public class ThreadContext
{
    private static ThreadLocal threadLocal = new ThreadLocal() {
        public Object initialValue() {
            return new HashMap<String, String>();
        }
    };

    public static void setValue(String key, String value) {
        ((HashMap<String, String>) threadLocal.get()).put(key, value);
    }

    public static String getValue(String key) {
        return ((HashMap<String, String>) threadLocal.get()).get(key);
    }
}
