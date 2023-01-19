package com.kasukusakura.mirai.console.junit5.frontend;

import com.kasukusakura.mirai.console.junit5.api.SneakyThrow;
import com.kasukusakura.mirai.console.junit5.api.MiraiConsoleStandardInput;
import kotlin.coroutines.Continuation;
import net.mamoe.mirai.console.util.ConsoleInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JUnit5ConsoleInput implements ConsoleInput {
    public static final JUnit5ConsoleInput INSTANCE = new JUnit5ConsoleInput();
    public boolean throwErrorIfNoMoreInput = true;

    private final LinkedList<String> pushedMessages = new LinkedList<>();
    private final LinkedList<Continuation<? super String>> requests = new LinkedList<>();

    private final Lock modifyLock = new ReentrantLock();

    public void pushInput(String line) {
        Objects.requireNonNull(line, "line");

        modifyLock.lock();
        try {
            Continuation<? super String> continuation = requests.pollFirst();
            if (continuation != null) {
                continuation.resumeWith(line);
                return;
            }

            pushedMessages.addLast(line);
        } finally {
            modifyLock.unlock();
        }
    }

    private JUnit5ConsoleInput() {
    }

    public MiraiConsoleStandardInput.NoInputAvailableCallback CALLBACK = (hint, continuation) -> {
        // already locked

        if (JUnit5ConsoleInput.this.throwErrorIfNoMoreInput) {
            throw SneakyThrow.thrown(new EOFException("No more input available."));
        }

        requests.addLast(continuation);

        return kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
    };

    @Nullable
    @Override
    public Object requestInput(@NotNull String s, @NotNull Continuation<? super String> continuation) {
        modifyLock.lock();
        try {
            String msg = pushedMessages.pollLast();
            if (msg != null) return msg;

            return CALLBACK.requestInput(s, continuation);
        } finally {
            modifyLock.unlock();
        }
    }
}
