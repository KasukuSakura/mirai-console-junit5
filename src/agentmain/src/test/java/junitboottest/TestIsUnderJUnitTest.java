package junitboottest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestIsUnderJUnitTest {
    private @interface IsUnderJUnitTesting {
    }

    @IsUnderJUnitTesting
    private static boolean isUnderJUnitTesting0() {
        return false;
    }

    @IsUnderJUnitTesting
    private boolean isUnderJUnitTesting1() {
        return false;
    }


    @IsUnderJUnitTesting
    private native boolean isUnderJUnitTesting2();

    @Test
    public void test() {
        Assertions.assertTrue(isUnderJUnitTesting0());
        Assertions.assertTrue(isUnderJUnitTesting1());
        Assertions.assertTrue(isUnderJUnitTesting2());
    }
}
