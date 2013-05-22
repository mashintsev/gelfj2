package org.graylog2.log4j2;

import org.apache.logging.log4j.core.LogEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author Anton Yakimov
 * @author Ivan Mashintsev
 * @author Jochen Schalanda
 */
public class Log4jVersionChecker {

    private static Method methodGetTimeStamp = null;

    static {
        Method[] declaredMethods = LogEvent.class.getDeclaredMethods();
        for(Method m : declaredMethods) {
            if (m.getName().equals("getMillis")) {
                methodGetTimeStamp = m;
                break;
            }
        }
    }

    public static long getTimeStamp(LogEvent event) {

        long timeStamp = 0;
        if(methodGetTimeStamp != null) {

            try {
                timeStamp = (Long) methodGetTimeStamp.invoke(event);
            } catch (IllegalAccessException e) {
                // Just return the current timestamp
            } catch (InvocationTargetException e) {
                // Just return the current timestamp
            }
        }

        return timeStamp == 0 ? System.currentTimeMillis() : timeStamp;
    }
}
