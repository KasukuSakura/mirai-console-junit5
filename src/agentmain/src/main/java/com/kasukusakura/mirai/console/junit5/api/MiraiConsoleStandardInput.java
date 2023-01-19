package com.kasukusakura.mirai.console.junit5.api;

import com.kasukusakura.mirai.console.junit5.frontend.JUnit5ConsoleInput;
import kotlin.coroutines.Continuation;
import net.mamoe.mirai.console.util.ConsoleInput;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 这里可以控制 {@link ConsoleInput#requestInput(String)} 的行为
 */
@SuppressWarnings("unused")
public class MiraiConsoleStandardInput {
    /**
     * 当缓冲区没有可用数据的时候的回调
     */
    public interface NoInputAvailableCallback {
        @NotNull
        default Object suspended() {
            return kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
        }

        /**
         * 当缓冲区没有可用数据时会调用此方法
         * <p>
         * requestInput 可用被同步处理, 也可以被异步处理,
         * <p>
         * 当同步处理的时候, 请返回一个 {@link java.lang.String} 对象, 并直接忽略第二个参数
         * <p>
         * 当异步处理时, 请返回 {@link #suspended()}, 并且在处理完成时调用 {@link Continuation#resumeWith(Object)}.
         * 参数类型为 {@link java.lang.String}.
         * <p>
         * 当异步处理时, 如果发生了错误, 请调用 {@link kotlin.ResultKt#createFailure(Throwable)}
         * <p>
         * Async Example:
         * <pre>{@code
         * public class MyCustomNoInputAvailableCallback implements MiraiConsoleStandardInput.NoInputAvailableCallback {
         *      Object requestInput(String hint, Continuation<? super String> continuation) {
         *          new Thread(() -> {
         *              Thread.sleep(10086);
         *              continuation.resultWith("1"); // success
         *              continuation.resultWith(ResultKt.createFailure(new Throwable())); // failed
         *          }).run();
         *          return suspended();
         *      }
         * }
         * }</pre>
         */
        @NotNull
        Object requestInput(@NotNull String hint, @NotNull Continuation<? super String> continuation);
    }

    /**
     * 将一条输入推送至输入至缓冲区末尾
     * <p>
     * 当 {@link ConsoleInput#requestInput(String)} 执行的时候会从缓冲区中返回最先进入的输入
     */
    public static void pushLine(@NotNull String line) {
        JUnit5ConsoleInput.INSTANCE.pushInput(line);
    }

    public static @NotNull NoInputAvailableCallback getInputFallback() {
        return JUnit5ConsoleInput.INSTANCE.CALLBACK;
    }

    public static void setInputFallback(@NotNull NoInputAvailableCallback callback) {
        Objects.requireNonNull(callback, "callback");
        JUnit5ConsoleInput.INSTANCE.CALLBACK = callback;
    }

    /**
     * 设置是否在缓冲区为空的情况下让 {@link ConsoleInput#requestInput(String)} 报错
     * (仅在默认 {@link NoInputAvailableCallback}) 中可用
     */
    public static void setThrowErrorIfNoMoreInput(boolean value) {
        JUnit5ConsoleInput.INSTANCE.throwErrorIfNoMoreInput = value;
    }
}
