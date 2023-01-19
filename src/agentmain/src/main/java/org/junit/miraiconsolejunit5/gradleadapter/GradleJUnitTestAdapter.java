package org.junit.miraiconsolejunit5.gradleadapter;

import com.kasukusakura.mirai.console.junit5.impl.JHookSupport;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@SuppressWarnings("unused")
public class GradleJUnitTestAdapter {

    public static CallSite adapter(MethodHandles.Lookup lookup, String name, MethodType methodType) throws Throwable {
        return JHookSupport.adapter(lookup, name, methodType);
    }

}
