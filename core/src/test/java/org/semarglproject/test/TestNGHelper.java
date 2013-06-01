package org.semarglproject.test;

import java.util.Collection;

public final class TestNGHelper {

    private TestNGHelper() {
    }

    public static Object[][] toArray(Collection<?> tests) {
        Object[][] result = new Object[tests.size()][];
        int i = 0;
        for (Object testCase : tests) {
            result[i++] = new Object[]{testCase};
        }
        return result;
    }

}
