# 这是什么

应用启动初始化器，方便在应用启动阶段初始化各种逻辑

# 有什么用

- 指定初始化依赖关系
- 指定初始化执行的进程和线程
- 编译时检测依赖关系
- 快速上手，灵活和简单的配置
- 内部逻辑有充分的单元测试保证

# 怎么用

1. 应用启动的时候执行库的初始化

   ```kotlin
   class MyApplication : Application() {
   
       override fun attachBaseContext(base: Context) {
           super.attachBaseContext(base)
           Startup.init(base)
       }
   }
   ```

2. 定义自己的初始化逻辑

   ```kotlin
   @Initializer
   class LogInitializer : Initializer {
       override fun init(context: Context, processName: String) {
           // ...
       }
   }
   ```

   

3. 根据需要配置依赖项、执行的进程和线程

   ```kotlin
   // 配置依赖项、运行进程、线程
   @Initializer(
       dependencies = [BInitializer::class, CInitializer::class],
       threadMode = ComponentInfo.ThreadMode.WorkThread,
       supportProcess = ["sub", "main"]
   )
   class LogInitializer : Initializer {
       override fun init(context: Context, processName: String) {
           // ...
       }
   }
   ```

   > - 一些优先级不那么高的耗时初始化逻辑，可以放在子线程中执行来提高启动速度
   > - 配置在子线程执行的初始化器会在启动时并行执行
   > - 配置在主线程的初始化器会在启动时串行执行
   > - 有依赖关系的初始化器会保证执行的先后顺序

   # 编译时检测

   如果有循环依赖、不合理线程、进程配置关系，会在编译时给出错误信息，快速定位问题

   ```kotlin
   // 自己依赖自己，会在编译时报错 CycleDependencyException
   @Config(dependencies = [CycleInit::class])
   class CycleInit : Initializer {
       override fun init(context: Context, processName: String) {
           // no-op
       }
   }
   ```

   除了循环依赖检测，还支持以下编译时检测：

   1. `IllegalAnnotationException` 注解使用错误时抛出异常。只能注解到实现了 `Initializer` 接口的类上
   2. `IllegalProcessException` 当定义的进程信息不合法的时候抛出异常。比如 A 初始化器依赖于 B 初始化器；B 初始化器的 supportProcess 集合必须大于等于 A 初始化器
   3. `IllegalThreadException` 当定义的线程信息不合法的时候抛出异常。比如 A 在主线程初始化，B 在子线程初始化， A 不能依赖 B