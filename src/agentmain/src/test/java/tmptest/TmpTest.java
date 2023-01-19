package tmptest;

import com.kasukusakura.mirai.console.junit5.frontend.JUnit5TestFrontend;
import kotlin.coroutines.EmptyCoroutineContext;
import net.mamoe.mirai.console.MiraiConsole;
import net.mamoe.mirai.console.MiraiConsoleImplementation;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.command.ConsoleCommandSender;
import net.mamoe.mirai.message.data.PlainText;

import java.nio.file.Path;

public class TmpTest {
    public static void main(String[] args) throws Throwable {
        var pt = Path.of("H:\\IDEAProjects\\mirai-console-junit5\\src\\agentmain\\build\\junittest");

        var frontend = new JUnit5TestFrontend(pt);

        MiraiConsoleImplementation.start(frontend);

        CommandManager.INSTANCE.executeCommand(ConsoleCommandSender.INSTANCE, new PlainText("/status"), false);

        kotlinx.coroutines.BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (s, c) -> {
            return MiraiConsole.INSTANCE.getJob().join(c);
        });
    }
}
