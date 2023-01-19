package com.kasukusakura.mirai.console.junit5.impl;

import kotlin.coroutines.Continuation;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.network.CustomLoginFailedException;
import net.mamoe.mirai.utils.DeviceVerificationRequests;
import net.mamoe.mirai.utils.DeviceVerificationResult;
import net.mamoe.mirai.utils.LoginSolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class NotAvailableLoginSolver extends LoginSolver {
    private static class NotAvailableException extends CustomLoginFailedException {
        public NotAvailableException() {
            super(true, "Login solver is not available under junit testing...");
        }
    }

    private void notAvailable() {
        throw new NotAvailableException();
    }

    @Nullable
    @Override
    public Object onSolvePicCaptcha(@NotNull Bot bot, @NotNull byte[] bytes, @NotNull Continuation<? super String> continuation) {
        notAvailable();
        return null;
    }

    @Nullable
    @Override
    public Object onSolveSliderCaptcha(@NotNull Bot bot, @NotNull String s, @NotNull Continuation<? super String> continuation) {
        notAvailable();
        return null;
    }

    @Nullable
    @Override
    public Object onSolveDeviceVerification(@NotNull Bot bot, @NotNull DeviceVerificationRequests requests, @NotNull Continuation<? super DeviceVerificationResult> $completion) {
        notAvailable();
        return super.onSolveDeviceVerification(bot, requests, $completion);
    }

    @Nullable
    @Override
    public Object onSolveUnsafeDeviceLoginVerify(@NotNull Bot bot, @NotNull String url, @NotNull Continuation<? super String> $completion) {
        notAvailable();
        return super.onSolveUnsafeDeviceLoginVerify(bot, url, $completion);
    }
}
