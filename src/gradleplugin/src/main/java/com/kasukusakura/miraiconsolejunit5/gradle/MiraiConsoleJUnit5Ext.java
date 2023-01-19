package com.kasukusakura.miraiconsolejunit5.gradle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiraiConsoleJUnit5Ext {
    public String buildPluginTaskName = null;
    public List<String> buildPluginAlternativeTasks = Arrays.asList("buildPlugin", "jar");

    public String pluginId;

    List<String> tasks() {

        List<String> tasks = new ArrayList<>();
        tasks.add(buildPluginTaskName);
        tasks.addAll(buildPluginAlternativeTasks);

        return tasks;
    }
}
