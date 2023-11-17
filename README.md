[中文 Readme.md](README-CN.md)

# What is it

Application startup initializer, which makes it easy to initialize various logics during the application startup phase.

# What can it do

- Specify initialization dependencies
- Specify the process and thread for initialization execution
- Compile-time dependency detection
- Quick start, flexible and simple configuration
- The internal logic is fully guaranteed by unit tests.

# How to use

1. Execute the library's initialization when the application starts

   ```kotlin
   class MyApplication : Application() {

       override fun attachBaseContext(base: Context) {
           super.attachBaseContext(base)
           Startup.init(base)
       }
   }
   ```

2. Define your own initialization logic

   ```kotlin
   @Initializer
   class LogInitializer : Initializer {
       override fun init(context: Context, processName: String) {
           // ...
       }
   }
   ```

3. Configure dependencies, execution processes, and threads as needed

   ```kotlin
   // Configure dependencies, runtime processes, and threads
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

   > - Some time-consuming initialization logic with lower priority can be executed in sub-threads to improve startup speed
   > - Initializers configured to run in sub-threads will be executed in parallel during startup
   > - Initializers configured to run in the main thread will be executed serially during startup
   > - Initializers with dependencies will ensure the order of execution

   # Compile-time detection

   If there are circular dependencies, unreasonable thread, and process configuration relationships, error messages will be given during compilation to quickly locate the problem

   ```kotlin
   // Self-dependency, a CycleDependencyException will be reported at compile time
   @Config(dependencies = [CycleInit::class])
   class CycleInit : Initializer {
       override fun init(context: Context, processName: String) {
           // no-op
       }
   }
   ```

   In addition to circular dependency detection, the following compile-time detections are also supported:

   1. `IllegalAnnotationException` is thrown when the annotation is used incorrectly. It can only be annotated on classes that implement the `Initializer` interface
   2. `IllegalProcessException` is thrown when the defined process information is illegal. For example, initializer A depends on initializer B; the supportProcess collection of initializer B must be greater than or equal to that of initializer A
   3. `IllegalThreadException` is thrown when the defined thread information is illegal. For example, A initializes in the main thread, B initializes in the sub-thread, and A cannot depend on B