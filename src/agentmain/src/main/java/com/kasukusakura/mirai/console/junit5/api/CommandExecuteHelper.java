package com.kasukusakura.mirai.console.junit5.api;

import net.mamoe.mirai.console.command.CommandExecuteResult;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;
import org.opentest4j.AssertionFailedError;

public class CommandExecuteHelper {
    public static void processCommand(CommandSender sender, String message) {
        String prefix = CommandManager.INSTANCE.getCommandPrefix();
        if (!message.startsWith(prefix)) {
            message = prefix + message;
        }

        processCommand(sender, new PlainText(message));
    }

    public static void processCommand(CommandSender sender, Message message) {
        try {
            processCommand0(sender, message);
        } catch (Throwable throwable) {
            throw new BadCommandExecutionException(throwable, sender, message);
        }
    }

    @SuppressWarnings({"ConstantConditions", "UnnecessaryLocalVariable"})
    private static void processCommand0(
            CommandSender sender,
            Message message
    ) {
        System.out.println("[JUNIT] " + sender + " issued command: " + message);

        CommandExecuteResult executeResult = CommandManager.INSTANCE.executeCommand(sender, message, true);
        Object executeResult1 = executeResult;

        if (executeResult instanceof CommandExecuteResult.Success) return;

        if (executeResult1 instanceof CommandExecuteResult.IllegalArgument) {
            throw SneakyThrow.thrown(executeResult.getException());
        }

        if (executeResult1 instanceof CommandExecuteResult.UnresolvedCommand) {
            throw new IllegalStateException("Command not found: " + message);
        }

        if (executeResult1 instanceof CommandExecuteResult.PermissionDenied) {
            throw new IllegalStateException("Permission denied.");
        }

        if (executeResult1 instanceof CommandExecuteResult.UnmatchedSignature) {
            CommandExecuteResult.UnmatchedSignature unmatchedSignature = ((CommandExecuteResult.UnmatchedSignature) executeResult1);
            throw new IllegalArgumentException("unmatched command signature, " + unmatchedSignature.getCommand().getUsage());
        }

        throw SneakyThrow.thrown(executeResult.getException());
    }


    public static class BadCommandExecutionException extends AssertionFailedError {
        private final CommandSender commandSender;
        private final Message message;

        public BadCommandExecutionException(Throwable cause, CommandSender commandSender, Message message) {
            super(cause == null ? null : cause.getLocalizedMessage(), cause);
            this.commandSender = commandSender;
            this.message = message;
        }

        public CommandSender getCommandSender() {
            return commandSender;
        }

        public Message getCommand() {
            return message;
        }

        private Object writeReplace() {
            return new AssertionFailedError(getLocalizedMessage());
        }
    }
}
