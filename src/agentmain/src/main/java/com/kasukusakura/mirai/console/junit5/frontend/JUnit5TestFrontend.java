package com.kasukusakura.mirai.console.junit5.frontend;

import com.kasukusakura.mirai.console.junit5.impl.NotAvailableLoginSolver;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import net.mamoe.mirai.console.MiraiConsoleFrontEndDescription;
import net.mamoe.mirai.console.frontendbase.AbstractMiraiConsoleFrontendImplementation;
import net.mamoe.mirai.console.frontendbase.FrontendBase;
import net.mamoe.mirai.console.frontendbase.logging.AllDroppedLogRecorder;
import net.mamoe.mirai.console.frontendbase.logging.LogRecorder;
import net.mamoe.mirai.console.util.ConsoleInput;
import net.mamoe.mirai.console.util.SemVersion;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.LoginSolver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JUnit5TestFrontend extends AbstractMiraiConsoleFrontendImplementation {
    private final FrontendBase frontendBase;
    private final ConsoleCommandSenderImpl impl = new ConsoleCommandSenderImpl() {
        /*
        @JvmSynthetic
        public suspend fun sendMessage(message: Message)

        @JvmSynthetic
        public suspend fun sendMessage(message: String)
        */

        public Object sendMessage(Message message, Continuation<? super Unit> continuation) {
            return sendMessage(message.contentToString(), continuation);
        }

        public Object sendMessage(String message, Continuation<? super Unit> continuation) {
            System.out.println(message);
            return Unit.INSTANCE;
        }
    };

    @Override
    public boolean isAnsiSupported() {
        return true;
    }

    public static Path getWorkingDir() {
        Path fst = new File("a").getAbsoluteFile().toPath().getParent();
        if (fst != null) {
            return fst;
        } else {
            return Paths.get(".").toAbsolutePath();
        }
    }

    public JUnit5TestFrontend(Path workingDir) {
        super("JUnit5 Test Frontend");

        Path wd = workingDir == null ? getWorkingDir() : workingDir;

        frontendBase = new FrontendBase() {
            @NotNull
            @Override
            public CoroutineScope getScope() {
                return JUnit5TestFrontend.this;
            }

            @NotNull
            @Override
            public Path getWorkingDirectory() {
                return wd;
            }

            @Override
            public void printToScreenDirectly(@NotNull String s) {
                System.out.println(s);
            }

            @Override
            protected void initScreen_forwardStdToMiraiLogger() {
                // unnecessary for junit test
            }

            @Override
            protected void initScreen_forwardStdToScreen() {
                // unnecessary for junit test
            }

            @NotNull
            @Override
            protected LogRecorder initLogRecorder() {
                // log files is unnecessary under junit test
                return AllDroppedLogRecorder.INSTANCE;
            }

            @Override
            public void recordToLogging(@NotNull String msg) {
                // log files is unnecessary under junit test
            }
        };
    }

    @NotNull
    @Override
    protected FrontendBase getFrontendBase() {
        return frontendBase;
    }

    @NotNull
    @Override
    public ConsoleCommandSenderImpl getConsoleCommandSender() {
        return impl;
    }

    @NotNull
    @Override
    public ConsoleInput getConsoleInput() {
        return JUnit5ConsoleInput.INSTANCE;
    }

    @NotNull
    @Override
    public MiraiConsoleFrontEndDescription getFrontEndDescription() {
        return new MiraiConsoleFrontEndDescription() {
            @NotNull
            @Override
            public String getName() {
                return "JUnit 5 Testing Frontend";
            }

            @NotNull
            @Override
            public String getVendor() {
                return "KasukuSakura Technologies";
            }

            @NotNull
            @Override
            public SemVersion getVersion() {
                return SemVersion.parse("1.0.0");
            }
        };
    }

    @NotNull
    @Override
    public Path getRootPath() {
        return getFrontendBase().getWorkingDirectory();
    }

    @NotNull
    @Override
    public LoginSolver createLoginSolver(long l, @NotNull BotConfiguration botConfiguration) {
        return new NotAvailableLoginSolver();
    }
}
