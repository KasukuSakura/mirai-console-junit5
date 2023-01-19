package com.kasukusakura.mirai.console.junit5.impl;

import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

class MiraiConsoleExecuteLauncher implements Launcher {
    private final ConcurrentLinkedDeque<LauncherDiscoveryListener> discoveryListeners = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<TestExecutionListener> testExecutionListeners = new ConcurrentLinkedDeque<>();

    @Override
    public void registerLauncherDiscoveryListeners(LauncherDiscoveryListener... listeners) {
        discoveryListeners.addAll(Arrays.asList(listeners));
    }

    @Override
    public void registerTestExecutionListeners(TestExecutionListener... listeners) {
        testExecutionListeners.addAll(Arrays.asList(listeners));
    }

    @Override
    public TestPlan discover(LauncherDiscoveryRequest launcherDiscoveryRequest) {
        throw new UnsupportedOperationException();
    }

    private static void planStart(ArrayList<TestExecutionListener> all, TestPlan plan, TestIdentifier testIdentifier) {
        for (TestExecutionListener executionListener : all) {
            if (plan != null) executionListener.testPlanExecutionStarted(plan);
            if (testIdentifier != null) executionListener.executionStarted(testIdentifier);
        }
    }

    private static void planEnd(ArrayList<TestExecutionListener> all, TestPlan plan, TestIdentifier testIdentifier, Throwable throwable) {
        TestExecutionResult result = throwable == null ? TestExecutionResult.successful() : TestExecutionResult.failed(throwable);
        for (TestExecutionListener executionListener : all) {
            if (testIdentifier != null) {
                executionListener.executionFinished(testIdentifier, result);
            }
            if (plan != null) {
                executionListener.testPlanExecutionFinished(plan);
            }
        }
    }

    @Override
    public void execute(LauncherDiscoveryRequest launcherDiscoveryRequest, TestExecutionListener... listeners) {
        ArrayList<TestExecutionListener> all = new ArrayList<>(testExecutionListeners);
        all.addAll(Arrays.asList(listeners));


        UniqueId rootId = UniqueId.root("mirai-console", "bootstrap");

        bootConsole:
        {
            UniqueId subid = rootId.append("boot", "bootstrap");
            UniqueId launcherError = rootId.append("boot", "launcher");

            MiraiConsoleBootDescriptor root = new MiraiConsoleBootDescriptor(rootId, "engine-root", TestDescriptor.Type.CONTAINER);
            root.addChild(new MiraiConsoleBootDescriptor(subid, "Bootstrap mirai console", TestDescriptor.Type.TEST));
            root.addChild(new MiraiConsoleBootDescriptor(launcherError, "Bootstrap test launcher", TestDescriptor.Type.TEST));

            TestPlan bootConsole = TestPlan.from(Collections.singleton(root), NotAvailableConfigurationParameters.INSTANCE);
            TestIdentifier testIdentifier = bootConsole.getTestIdentifier(subid);

            planStart(all, bootConsole, testIdentifier);

            Throwable theException = null;
            try {
                MiraiConsoleBoot.bootstrap();
            } catch (Throwable exception) {
                theException = exception;
            }


            if (theException != null) {
                planEnd(all, bootConsole, testIdentifier, theException);
                return;
            }

            try {
                MiraiConsoleBoot.initJUnitTestClassLoader();
            } catch (Throwable throwable) {
                planEnd(all, bootConsole, testIdentifier, throwable);
                break bootConsole;
            }

            planEnd(all, null, testIdentifier, null);


            // Create real launch
            Launcher launcher = LauncherFactory.create();
            if (!discoveryListeners.isEmpty()) {
                launcher.registerLauncherDiscoveryListeners(discoveryListeners.toArray(new LauncherDiscoveryListener[0]));
            }
            if (!testExecutionListeners.isEmpty()) {
                launcher.registerTestExecutionListeners(testExecutionListeners.toArray(new TestExecutionListener[0]));
            }

            LauncherDiscoveryRequest request;
            try {

                request = LauncherDiscoveryRequestBuilder.request()
                        .listeners(launcherDiscoveryRequest.getDiscoveryListener())
                        .parentConfigurationParameters(launcherDiscoveryRequest.getConfigurationParameters())
                        .filters(launcherDiscoveryRequest.getPostDiscoveryFilters().toArray(new PostDiscoveryFilter[0]))
                        .filters(launcherDiscoveryRequest.getFiltersByType(DiscoveryFilter.class).toArray(new DiscoveryFilter[0]))
                        .filters(launcherDiscoveryRequest.getEngineFilters().toArray(new EngineFilter[0]))
                        .selectors(
                                launcherDiscoveryRequest.getSelectorsByType(DiscoverySelector.class)
                                        .stream().map(selector -> {
                                            if (selector instanceof LazyLoadClassDiscovery) {
                                                Class<?> c = MiraiConsoleBoot.loadTestClass(((LazyLoadClassDiscovery) selector).classname);
                                                return DiscoverySelectors.selectClass(c);
                                            }
                                            return selector;
                                        })
                                        .collect(Collectors.toList())
                        )
                        .build();
            } catch (Throwable throwable) {
                TestIdentifier launcherIdent = bootConsole.getTestIdentifier(launcherError);
                planStart(all, null, launcherIdent);
                planEnd(all, bootConsole, launcherIdent, throwable);

                break bootConsole;
            }

            planEnd(all, bootConsole, null, null);

            launcher.execute(request, listeners);
        }

        {
            UniqueId subid = rootId.append("boot", "shutdown");

            MiraiConsoleBootDescriptor root = new MiraiConsoleBootDescriptor(rootId, "engine-root", TestDescriptor.Type.CONTAINER);
            root.addChild(new MiraiConsoleBootDescriptor(subid, "Shutdown mirai console", TestDescriptor.Type.TEST));

            TestPlan shutdown = TestPlan.from(Collections.singleton(root), NotAvailableConfigurationParameters.INSTANCE);
            TestIdentifier testIdentifier = shutdown.getTestIdentifier(subid);

            planStart(all, shutdown, testIdentifier);

            Throwable theException = null;
            try {
                MiraiConsoleBoot.shutdown();
            } catch (Throwable exception) {
                theException = exception;
            }

            planEnd(all, shutdown, testIdentifier, theException);
        }
    }

    @Override
    public void execute(TestPlan testPlan, TestExecutionListener... listeners) {
        throw new UnsupportedOperationException();
    }

    private static class NotAvailableConfigurationParameters implements ConfigurationParameters {
        private static final NotAvailableConfigurationParameters INSTANCE = new NotAvailableConfigurationParameters();


        @Override
        public Optional<String> get(String key) {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> getBoolean(String key) {
            return Optional.empty();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Set<String> keySet() {
            return Collections.emptySet();
        }
    }
}
