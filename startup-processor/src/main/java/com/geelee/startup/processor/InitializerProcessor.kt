package com.geelee.startup.processor

import com.geelee.startup.annotation.Config
import com.geelee.startup.annotation.IInitializerRegistry
import com.geelee.startup.annotation.model.ComponentInfo
import com.geelee.startup.annotation.model.DependencyChain
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.geelee.startup.processor.exception.IllegalAnnotationException
import com.geelee.startup.processor.ktx.toListLiteral
import com.geelee.startup.processor.ktx.toLiteral
import com.geelee.startup.processor.ktx.toMapLiteral
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class InitializerProcessor : AbstractProcessor() {

    companion object {
        private const val TAG = "InitializerProcessor"
        private const val GENERATE_CLASS_NAME = IInitializerRegistry.GENERATED_CLASS_NAME
        private const val GENERATE_CLASS_PACKAGE_NAME =
            IInitializerRegistry.GENERATED_CLASS_PACKAGE_NAME

        private const val ALL_LIST = "allList"
        private const val MAIN_THREAD_LIST = "mainThreadList"
        private const val WORK_THREAD_LIST = "workThreadList"
    }

    private lateinit var messager: Messager
    private lateinit var filer: Filer
    private lateinit var elementUtils: Elements

    private var writeRoundDone: Boolean = false
    private var round: Int = 0

    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        messager = processingEnvironment.messager
        filer = processingEnvironment.filer
        elementUtils = processingEnvironment.elementUtils
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            Config::class.java.canonicalName
        )
    }

    override fun process(
        set: MutableSet<out TypeElement>,
        roundEnvironment: RoundEnvironment
    ): Boolean {
        round++
        val processingOver = roundEnvironment.processingOver()
        log(
            Diagnostic.Kind.NOTE,
            "processing() round:$round processingOver:$processingOver annotations:$set"
        )
        if (processingOver) {
            if (set.isNotEmpty()) {
                log(
                    Diagnostic.Kind.ERROR,
                    "Unexpected processing state: annotations still available after processing over"
                )
                return false
            }
        }
        if (set.isEmpty()) {
            return false
        }
        if (writeRoundDone) {
            log(
                Diagnostic.Kind.ERROR,
                "Unexpected processing state: annotations still available after processing over"
            )
        }
        val result = writeToFile(roundEnvironment)
        writeRoundDone = true
        return result
    }

    private fun buildComponentInfoList(roundEnvironment: RoundEnvironment): List<ComponentInfo> {
        return roundEnvironment.getElementsAnnotatedWith(Config::class.java)
            .filter {
                if (it !is TypeElement) {
                    throw IllegalAnnotationException("${Config::class.java.simpleName} 只能注解到实现了 Initializer 接口的类上 --> ${it.simpleName}")
                }
                true
            }
            .map {
                val annotation = it.getAnnotation(Config::class.java)
                val componentName = (it as TypeElement).qualifiedName.toString()
                val supportProcess = annotation.supportProcess
                val threadMode = annotation.threadMode
                val dependencies = annotation.getClazzArrayNameList()
                ComponentInfo(
                    name = componentName,
                    supportProcess = supportProcess.toList(),
                    threadMode = threadMode,
                    dependencies = dependencies.toList()
                ) { }
            }
    }

    private fun Config.getClazzArrayNameList(): List<String> {
        try {
            return this.dependencies.map { it.java.canonicalName }
        } catch (mte: MirroredTypesException) {
            if (mte.typeMirrors.isEmpty()) return emptyList()
            return mte.typeMirrors.map {
                ((it as DeclaredType).asElement() as TypeElement).qualifiedName.toString()
            }
        }
    }

    private fun writeToFile(roundEnvironment: RoundEnvironment): Boolean {
        val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"] ?: return false

        val componentList = buildComponentInfoList(roundEnvironment).sorted()
        val dependencyChainBuilder = DependencyChainBuilder(logger, componentList.toMap())
        val mainThreadComponentChainList =
            dependencyChainBuilder.buildComponentChainList(componentList.getMainThreadComponentList())
        val workThreadComponentChainList =
            dependencyChainBuilder.buildComponentChainList(componentList.getWorkThreadComponentList())

        FileSpec.builder(
            GENERATE_CLASS_PACKAGE_NAME,
            GENERATE_CLASS_NAME
        )
            .addComment("Auto Generated by the KAPT.  DO NOT EDIT!")
            .addType(
                TypeSpec.classBuilder(GENERATE_CLASS_NAME)
                    .addSuperinterface(IInitializerRegistry::class.asTypeName())
                    .addProperties(componentList.generateComponentInfoProperties())
                    .addProperty(
                        PropertySpec.builder(
                            ALL_LIST,
                            Map::class.asClassName().parameterizedBy(
                                String::class.asTypeName(),
                                ComponentInfo::class.asTypeName()
                            )
                        )
                            .addModifiers(KModifier.PRIVATE)
                            .initializer(CodeBlock.of(componentList.toMapLiteral()))
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder(
                            MAIN_THREAD_LIST,
                            List::class.asClassName().parameterizedBy(
                                DependencyChain::class.asTypeName()
                            )
                        )
                            .addModifiers(KModifier.PRIVATE)
                            .initializer(CodeBlock.of(mainThreadComponentChainList.toListLiteral()))
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder(
                            WORK_THREAD_LIST,
                            List::class.asClassName().parameterizedBy(
                                DependencyChain::class.asTypeName()
                            )
                        )
                            .addModifiers(KModifier.PRIVATE)
                            .initializer(CodeBlock.of(workThreadComponentChainList.toListLiteral()))
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("getAllInitializer")
                            .returns(
                                Map::class.asClassName().parameterizedBy(
                                    String::class.asTypeName(),
                                    ComponentInfo::class.asTypeName()
                                )
                            )
                            .addModifiers(KModifier.OVERRIDE)
                            .addStatement("return $ALL_LIST")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("getMainThreadComponentChainList")
                            .returns(
                                List::class.asClassName().parameterizedBy(
                                    DependencyChain::class.asTypeName()
                                )
                            )
                            .addModifiers(KModifier.OVERRIDE)
                            .addStatement("return $MAIN_THREAD_LIST")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("getWorkThreadComponentChainList")
                            .returns(
                                List::class.asClassName().parameterizedBy(
                                    DependencyChain::class.asTypeName()
                                )
                            )
                            .addModifiers(KModifier.OVERRIDE)
                            .addStatement("return $WORK_THREAD_LIST")
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(File(kaptKotlinGeneratedDir))
        // 输出报告
        InitializerReporter(
            mainThreadComponentChainList,
            workThreadComponentChainList
        ).outputReport(processingEnv)
        return true
    }

    /**
     * 获取所有在主线程执行的初始化器
     */
    private fun List<ComponentInfo>.getMainThreadComponentList(): List<ComponentInfo> {
        return this.filter { it.threadMode == ComponentInfo.ThreadMode.MainThread }
    }

    /**
     * 获取所有在子线程执行的初始化器
     */
    private fun List<ComponentInfo>.getWorkThreadComponentList(): List<ComponentInfo> {
        return this.filter { it.threadMode == ComponentInfo.ThreadMode.WorkThread }
    }

    /**
     * 根据列表构造一个 初始化器类名 to ComponentInfo 的 Map
     */
    private fun List<ComponentInfo>.toMap(): Map<String, ComponentInfo> {
        val map = mutableMapOf<String, ComponentInfo>()
        this.forEach {
            map[it.name] = it
        }
        return map
    }

    /**
     * 根据列表一个个构造 componentInfo 成员变量
     */
    private fun List<ComponentInfo>.generateComponentInfoProperties(): List<PropertySpec> {
        return this.map {
            PropertySpec.builder(
                name = it.simpleName,
                type = ComponentInfo::class.asTypeName()
            )
                .addModifiers(KModifier.PRIVATE)
                .initializer(CodeBlock.of(it.toLiteral()))
                .build()
        }
    }

    private val logger = object : ILogger {
        override fun i(msg: String) {
            log(Diagnostic.Kind.NOTE, msg)
        }

        override fun e(msg: String, throwable: Throwable) {
            log(Diagnostic.Kind.ERROR, msg)
        }
    }

    private fun log(level: Diagnostic.Kind, msg: String) {
        val message = "$TAG --> $msg"
        println(message)
    }
}
