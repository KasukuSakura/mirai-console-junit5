package com.kasukusakura.miraiconsolejunit5.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Usage;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskOutputs;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MiraiConsoleJUnit5 implements Plugin<Project> {
    private static final Properties METADATA = new Properties();
    private static final String VERSION;

    static {
        try (InputStream res = MiraiConsoleJUnit5.class.getResourceAsStream("metadata.properties")) {
            METADATA.load(res);

            VERSION = METADATA.getProperty("version");
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void apply(@NotNull Project project) {
        project.getExtensions().create("miraiconsolejunit5", MiraiConsoleJUnit5Ext.class);

        project.afterEvaluate($$$$$ -> project.getPluginManager().withPlugin("java", $$$ -> {
            SourceSetContainer container = project.getExtensions().getByType(SourceSetContainer.class);

            container.all(sourceSet -> {
                if (!sourceSet.getName().equals("test")) return;

                SourceSet mainSrc = container.getByName("main");

                String confName = mainSrc.getTaskName("testerJUnitCPPath", null);

                Configuration conf = project.getConfigurations().create(confName);
                conf.setTransitive(true);
                conf.setCanBeResolved(true);
                conf.setVisible(true);
                conf.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                conf.getAttributes().attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String.class), "jvm");

                // TODO

                String artApi = "com.kasukusakura.mirai-console-junit5:mirai-console-junit5:" + VERSION;

                project.getDependencies().add(confName, artApi);
                project.getDependencies().add(confName, artApi + ":agentstub");


                Optional<ExternalModuleDependency> miraiSystemDependency = project.getConfigurations().getByName(mainSrc.getCompileClasspathConfigurationName())
                        .getAllDependencies().stream()
                        .map(it -> {
                            if (it instanceof ExternalModuleDependency) return (ExternalModuleDependency) it;
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .filter(it -> it.getVersion() != null)
                        .filter(it -> "net.mamoe".equals(it.getGroup()))
                        .filter(it -> {
                            String mod = it.getModule().getName();
                            return mod.startsWith("mirai-");
                        })
                        .findAny();

                project.getDependencies().add(sourceSet.getCompileOnlyConfigurationName(), artApi);

                if (miraiSystemDependency.isPresent()) {
                    String miraiVersion = miraiSystemDependency.get().getVersion();

                    project.getDependencies().add(confName, "net.mamoe:mirai-core-mock:" + miraiVersion);
                    project.getDependencies().add(confName, "net.mamoe:mirai-console:" + miraiVersion);
                    project.getDependencies().add(confName, "net.mamoe:mirai-console-frontend-base:" + miraiVersion);


                    project.getDependencies().add(sourceSet.getCompileOnlyConfigurationName(), "net.mamoe:mirai-core-mock:" + miraiVersion);
                }

                project.getTasks().named("test", Test.class).configure(test -> {
                    test.dependsOn(conf);

                    File rework = new File(project.getBuildDir(), "mirai-console-junit");

                    test.workingDir(rework);
                    test.doFirst($$ -> {
                        if (!miraiSystemDependency.isPresent()) {
                            throw new RuntimeException("No mirai system dependencies found; Please add mirai dependencies to compile classpath; If it is already defined, add a version specially for an arbitrary dependency.");
                        }
                        test.getWorkingDir().mkdirs();

                        ResolvedConfiguration resolvedConfiguration = conf.getResolvedConfiguration();
                        Optional<ResolvedArtifact> agentstub = resolvedConfiguration.getResolvedArtifacts().stream().filter(art -> {
                            ComponentArtifactIdentifier id = art.getId();
                            if (id instanceof ModuleComponentArtifactIdentifier) {
                                ModuleComponentArtifactIdentifier mod = (ModuleComponentArtifactIdentifier) id;
                                ModuleComponentIdentifier componentIdentifier = mod.getComponentIdentifier();
                                if (componentIdentifier.getGroup().equals("com.kasukusakura.mirai-console-junit5")) {
                                    if (componentIdentifier.getModule().equals("mirai-console-junit5")) {
                                        return "agentstub".equals(art.getClassifier());
                                    }
                                }
                            }
                            return false;
                        }).findFirst();

                        if (!agentstub.isPresent()) {
                            throw new RuntimeException("Cannot found agent main");
                        }


                        File agentstubFile = agentstub.get().getFile();

                        test.jvmArgs("-javaagent:" + agentstubFile.getAbsolutePath());
                        test.setClasspath(conf);

                        test.environment("JUNIT5_TESTING_CP",
                                sourceSet.getRuntimeClasspath().getFiles().stream()
                                        .map(File::getAbsolutePath)
                                        .collect(new JoinToStringCollector<>(File.pathSeparator))
                                        .toString()
                        );

                        MiraiConsoleJUnit5Ext ext = project.getExtensions().getByType(MiraiConsoleJUnit5Ext.class);

                        copyPlugin:
                        {
                            File pluginsFolder = new File(test.getWorkingDir(), "plugins");
                            pluginsFolder.mkdirs();

                            Path target = pluginsFolder.toPath().resolve(project.getRootProject().getName() + ".jar");

                            for (String task : ext.tasks()) {
                                if (task == null) continue;
                                Task theTask = project.getTasks().findByName(task);
                                if (theTask != null) {
                                    TaskOutputs outputs = theTask.getOutputs();

                                    if (!outputs.getHasOutput()) {
                                        break copyPlugin;
                                    }

                                    Path src = outputs.getFiles().getSingleFile().toPath();
                                    if (Files.isRegularFile(src)) {
                                        try {
                                            Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
                                        } catch (IOException e) {
                                            throw new UncheckedIOException(e);
                                        }
                                    }


                                    break copyPlugin;
                                }
                            }
                        }

                        if (ext.pluginId != null) {
                            test.environment("JUNIT5_SELECTED_PLUGIN", ext.pluginId);
                        }

                    });


                    MiraiConsoleJUnit5Ext ext = project.getExtensions().getByType(MiraiConsoleJUnit5Ext.class);

                    for (String task : ext.tasks()) {
                        if (task == null) continue;

                        Task theTask = project.getTasks().findByName(task);
                        if (theTask != null) {
                            test.dependsOn(theTask);
                            return;
                        }
                    }
                });


            });
        }));
    }
}
