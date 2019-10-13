@file:Suppress("unused")

package com.andrewemery.recast.processor

import com.andrewemery.recast.annotation.Recast
import com.andrewemery.recast.annotation.RecastSync
import com.andrewemery.recast.job.Job
import com.andrewemery.recast.job.Result
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.CoroutineScope
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

/**
 * The [RecastProcessor] is used to generate methods for the [RecastSync] and [Recast] annotations.
 */
@AutoService(Processor::class)
@SupportedOptions("kapt.kotlin.generated")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
internal class RecastProcessor : ProcessorBase() {
    override val environment: ProcessingEnvironment by lazy { processingEnv }
    private var nextId: Int = 0

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * process
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    data class Group(val kind: ElementKind, val packageName: String, val fileName: String)
    
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        elements<RecastSync>(roundEnv).plus(elements<Recast>(roundEnv))
            .groupBy { element ->
                val fileName = when {
                    element.kind == ElementKind.CLASS -> element.simpleName.toString()
                    element.enclosingElement.simpleName.endsWith("Kt") -> element.enclosingElement.simpleName.toString().removeSuffix("Kt") + "Rc"
                    else -> element.enclosingElement.simpleName.toString()
                }
                Group(element.kind, packageName(element), fileName)
            }.forEach { entry: Map.Entry<Group, List<Element>> ->
                val group = entry.key
                when (group.kind) {

                    ElementKind.METHOD -> {
                        val functions = mutableListOf<FunSpec>()
                        for (elem: Element in entry.value) {
                            val function = elem as ExecutableElement
                            if (function.isEnclosed()) continue
                            buildSyncFunction(function)?.apply { functions.add(this) }
                            buildAsyncFunction(function)?.apply { functions.add(this) }
                        }
                        if (functions.size != 0) write(group.packageName, group.fileName, functions)
                    }

                    in setOf(ElementKind.CLASS, ElementKind.INTERFACE) -> {
                        val element = entry.value[0]
                        val functions = mutableListOf<FunSpec>()
                        val sync: RecastSync? = element.getAnnotation(RecastSync::class.java)
                        val async: Recast? = element.getAnnotation(Recast::class.java)
                        for (elem: Element in element.enclosedElements.filter { it.kind == ElementKind.METHOD }) {
                            val function = elem as ExecutableElement
                            buildSyncFunction(function, sync)?.apply { functions.add(this) }
                            buildAsyncFunction(function, async)?.apply { functions.add(this) }
                        }
                        if (functions.size != 0) write(group.packageName, group.fileName, functions)
                    }

                    else -> e("invalid recast annotation on: ${group.packageName}.${group.fileName}")
                }
            }

        return true
    }

    /**
     * Write a file at the given package containing the given functions.
     *
     * @param packageName The name of the package to add the file to.
     * @param fileName The name of the file to write.
     * @param functions The functions to include.
     */
    private fun write(packageName: String, fileName: String, functions: List<FunSpec>) {
        val file = FileSpec.builder(packageName, fileName)
        functions.forEach { file.addFunction(it) }
        write(file.build())
    }

    /**
     * Build a synchronous function that wraps the given function using the given annotation.
     *
     * @param function The function to wrap.
     * @param default The default annotation to use if no annotation is present on the function itself.
     * @return The function specification, or null if no function should be generated.
     */
    private fun buildSyncFunction(function: ExecutableElement, default: RecastSync? = null): FunSpec? {
        if (!function.isRecastValid()) return null
        val sync: RecastSync = function.getAnnotation(RecastSync::class.java) ?: default ?: return null
        val suspendingType: TypeName = function.suspendingType
        val parameters = function.parametersOf()
        val receiver = function.receiver()

        return FunSpec.builder(function.simpleName.toString() + sync.suffix)
            .apply { if (receiver != null) receiver(receiver) }
            .addParameters(parameters)
            .returns(suspendingType)
            .addCode(
                CodeBlock.builder()
                    .add("return %M { ", MemberName("com.andrewemery.recast.coroutines", "runBlocking"))
                    .addFunctionCall(function)
                    .add(" }")
                    .build()
            ).build()
    }

    /**
     * Build an asynchronous function that wraps the given function.
     *
     * @param function The function to wrap.
     * @param default The default annotation to use if no annotation is present on the function itself.
     * @return The function specification, or null if no function should be generated.
     */
    private fun buildAsyncFunction(function: ExecutableElement, default: Recast? = null): FunSpec? {
        if (!function.isRecastValid()) return null
        val async: Recast = function.getAnnotation(Recast::class.java) ?: default ?: return null
        val parameters = function.parametersOf()
        val receiver = function.receiver()
        val callbackType = Function1::class.asClassName()
            .parameterizedBy(
                Result::class.asClassName()
                    .parameterizedBy(function.suspendingType), Unit::class.asClassName()
            )

        val globalScope = MemberName("kotlinx.coroutines", "GlobalScope")
        val coroutineScopePlus = MemberName("kotlinx.coroutines", "plus")
        val dispatchers = MemberName("com.andrewemery.recast.coroutines", "Dispatchers")
        val scope = CodeBlock.builder().add("%M.%M(%M.IO)", globalScope, coroutineScopePlus, dispatchers).build()

        return FunSpec.builder(function.simpleName.toString() + async.suffix)
            .apply { if (receiver != null) receiver(receiver) }
            .addParameters(parameters)
            .apply {
                if (async.scoped) addParameter(
                    ParameterSpec.builder("scope", CoroutineScope::class).defaultValue(
                        scope
                    ).build()
                )
            }
            .addParameter("callback", callbackType)
            .returns(Job::class)
            .addCode(
                CodeBlock.builder()
                    .add("return %M(", MemberName("com.andrewemery.recast.coroutines", "runBackground"))
                    .apply { if (!async.scoped) add(scope) else add("scope") }
                    .add(", operation = { ")
                    .addFunctionCall(function)
                    .add(" }, callback = callback)")
                    .build()
            ).build()
    }

    /**
     * Get the annotations supported by this processor.
     */
    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(RecastSync::class.java.canonicalName, Recast::class.java.canonicalName)
    }
}

/**
 * Get whether or not an annotated method is enclosed within a annotated class.
 */
private fun ExecutableElement.isEnclosed(): Boolean {
    val enclosing = enclosingElement
    return enclosing != null && (enclosing.getAnnotation(RecastSync::class.java) != null || enclosing.getAnnotation(
        Recast::class.java
    ) != null)
}

/**
 * Get whether or not it is valid to generate a recast function for the given, annotated function.
 */
private fun ExecutableElement.isRecastValid(): Boolean {
    return suspending && enclosingElement != null && !enclosingElement.simpleName.endsWith("DefaultImpls")
}

/**
 * Get the receiver to set against the generated function (if any).
 */
private fun ExecutableElement.receiver(): TypeName? {
    val enclosing = enclosingElement
    if (enclosing == null || enclosing.simpleName.endsWith("Kt")) return null
    return enclosing.asType().asTypeName()
}
