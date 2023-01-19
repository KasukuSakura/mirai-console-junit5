# Configuration

由 mirai-console-junit5 启动的 mirai-console 示例运行在 `/build/mirai-console-junit` 中

如果 `plugins` 存在其他插件, 需要使用

```groovy
miraiconsolejunit5 {
    pluginId = ""
}
```

指定项目的插件 id

## 检测是否在 JUnit 中运行

因为 mirai-console 没有方法检测是否正在 JUnit 下运行, 所以需要在插件源代码中添加以下片段

```kotlin
// kotlin

//@Retention(AnnotationRetention.BINARY)
internal annotation class IsUnderJUnitTesting

@IsUnderJUnitTesting
internal fun isUnderJUnitTesting(): Boolean = false
```

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class JUnitTestHelper {
    @Retention(RetentionPolicy.CLASS)
    private @interface IsUnderJUnitTesting {
    }

    @IsUnderJUnitTesting
    public static boolean isUnderJUnitTesting() {
        return false;
    }
}
```

由 `IsUnderJUnitTesting` 注解标记的返回类型为 `boolean` 的方法会被 `mirai-console-junit5` 替换为 `return true`

因此可以使用以上方法判断是否在 junit 测试环境中运行, 并做一些环境初始化准备

```kotlin
object MyTestPlugin : KotlinPlugin(
    JvmPluginDescription("com.kasukusakura.mytest.testing", "1.0.0") {}
) {
    override fun onEnable() {
        logger.info { "isUnderJUnitTesting: " + isUnderJUnitTesting() }

        if (isUnderJUnitTesting()) {
            logger.info("JUnit Testing")
        } else {
            logger.info("Not JUnit Testing")
        }
    }
}
```

-------------

# API

在 test 源里可以使用 mirai-console-junit5 的 api (`com.kasukusakura.mirai.console.junit5.api`)

## 控制输入

```kotlin
MiraiConsoleStandardInput.pushLine("message...")
```

> 注: JUnit Console 不附带命令输入, 如果需要让 console 执行一条命令,
>
> 请使用 `CommandExecuteHelper.executeCommand(ConsoleCommandSender.INSTANCE, "status")`

