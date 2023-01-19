package com.kasukusakura.mirai.console.junit5.impl;

import com.kasukusakura.mirai.console.junit5.api.CommandExecuteHelper;
import com.kasukusakura.mirai.console.junit5.frontend.JUnit5TestFrontend;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.Job;
import net.mamoe.mirai.console.MiraiConsole;
import net.mamoe.mirai.console.MiraiConsoleImplementation;
import net.mamoe.mirai.console.command.ConsoleCommandSender;
import net.mamoe.mirai.console.plugin.PluginManager;
import net.mamoe.mirai.console.plugin.jvm.AbstractJvmPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginClasspath;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription;
import net.mamoe.mirai.mock.MockBotFactory;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("RedundantTypeArguments")
class MiraiConsoleBoot {
    private static JUnitTestingClassLoader classLoader;

    static void bootstrap() throws Exception {
        MockBotFactory.initialize();


        Path pt = JUnit5TestFrontend.getWorkingDir();

        System.out.println("[JUNIT5] Working dir: " + pt);

        {
            Path loggerConf = pt.resolve("config/Console/Logger.yml");
            if (!Files.isRegularFile(loggerConf)) {
                Files.createDirectories(loggerConf.getParent());
                Files.write(loggerConf, (""
                        + "defaultPriority: ALL\n"
                        + "loggers: \n"
                        + "  Bot: ALL\n"
                        + "  org.eclipse.aether.internal: INFO\n"
                        + "  org.apache.http: INFO\n"
                ).getBytes(StandardCharsets.UTF_8));
            }
        }

        JUnit5TestFrontend frontend = new JUnit5TestFrontend(pt);

        MiraiConsoleImplementation.start(frontend);

        CommandExecuteHelper.processCommand(ConsoleCommandSender.INSTANCE, "status");

    }

    static void initJUnitTestClassLoader() throws Throwable {
        Stream<JvmPlugin> pluginStream = PluginManager.INSTANCE.getPlugins()
                .stream().<JvmPlugin>map(plugin -> {
                    if (!plugin.isEnabled()) return null;
                    if (plugin instanceof JvmPlugin) return (JvmPlugin) plugin;
                    return null;
                });

        {
            String id = System.getenv("JUNIT5_SELECTED_PLUGIN");
            if (id != null) {
                pluginStream = pluginStream.filter(jvmPlugin -> {
                    JvmPluginDescription description = jvmPlugin.getDescription();
                    return description.getId().equals(id);
                });
            }
        }

        List<JvmPlugin> usablePlugins = pluginStream.filter(Objects::nonNull).collect(Collectors.toList());
        if (usablePlugins.size() == 0) {
            Assumptions.abort("No plugins available for selecting.");
        }
        if (usablePlugins.size() != 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("More than one plugin is usable: ");
            for (JvmPlugin plugin : usablePlugins) {
                sb.append(plugin.getDescription().getId()).append(", ");
            }
            sb.setLength(sb.length() - 2);
            Assumptions.abort(sb.toString());
        }

        JvmPlugin selected = usablePlugins.get(0);


        Method getJvmPluginClasspath = AbstractJvmPlugin.class.getDeclaredMethod("getJvmPluginClasspath");
        getJvmPluginClasspath.setAccessible(true);

        JvmPluginClasspath cp = (JvmPluginClasspath) getJvmPluginClasspath.invoke(selected);

        classLoader = new JUnitTestingClassLoader(cp.getPluginClassLoader());

        String testingCp = System.getenv("JUNIT5_TESTING_CP");
        if (testingCp != null) {
            for (String subt : testingCp.split(File.pathSeparator)) {

                System.out.println("[JUNIT5] Loaded classpath " + subt);

                classLoader.addURL(new File(subt).toURI().toURL());
            }
        }
    }

    private static class JUnitTestingClassLoader extends URLClassLoader {
        private static final ClassLoader APP_CCL = MiraiConsoleBoot.class.getClassLoader();
        private static final List<String> PREFIX_PKGS = Arrays.asList(
                "com.kasukusakura.mirai.console.junit5.",
                "org.junit."
        );

        JUnitTestingClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        @Override
        protected void addURL(URL url) {
            super.addURL(url);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            for (String pkg : PREFIX_PKGS) {
                if (name.startsWith(pkg)) {
                    return APP_CCL.loadClass(name);
                }
            }
            return super.loadClass(name, resolve);
        }
    }

    static Class<?> loadTestClass(String classname) {
        try {
            return Class.forName(classname, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static void shutdown() throws Throwable {
        System.out.println("===============================");
        System.out.println("JUnit5 Console Testing Closing....");

        Job job = MiraiConsole.INSTANCE.getJob();
        MiraiConsole.shutdown();


        kotlinx.coroutines.BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s, c) -> {
            return job.join(c);
        });
    }
}
