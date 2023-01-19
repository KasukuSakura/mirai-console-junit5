package com.kasukusakura.mirai.console.junit5.api;

@SuppressWarnings("unchecked")
public class SneakyThrow {
    public static <T extends Throwable> RuntimeException thrown(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
