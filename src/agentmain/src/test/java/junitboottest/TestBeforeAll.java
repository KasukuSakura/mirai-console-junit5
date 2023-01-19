package junitboottest;

import org.junit.jupiter.api.*;

import java.util.stream.Stream;


public class TestBeforeAll {
    @BeforeAll
    public static void testBeforeAll() {
        System.out.println("@!!");
    }

    @Test
    public void a() {
    }

    @TestFactory
    public Stream<DynamicNode> test() {
        return Stream.of(DynamicTest.dynamicTest("Test", () -> {
        }));
    }
}
