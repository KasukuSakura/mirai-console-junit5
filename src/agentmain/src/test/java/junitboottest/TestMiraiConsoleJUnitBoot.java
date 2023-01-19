package junitboottest;

import org.junit.jupiter.api.Test;

import java.io.File;

public class TestMiraiConsoleJUnitBoot {
    @Test
    void run() {
        System.out.println(ProcessHandle.current());
        System.out.println("TCL: " + getClass().getClassLoader());
        System.out.println("PCL: " + getClass().getClassLoader().getParent());
        System.out.println(new File(".").getAbsolutePath());
    }

    @Test
    void run2() {
        System.out.println(ProcessHandle.current());
    }
}
