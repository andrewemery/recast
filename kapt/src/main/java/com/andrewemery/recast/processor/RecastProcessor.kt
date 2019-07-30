@file:Suppress("unused")

package com.andrewemery.recast.processor

import com.andrewemery.recast.annotation.RecastAsync
import com.andrewemery.recast.annotation.RecastSync
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
 * The [RecastProcessor] is used to generate methods for the [RecastSync] and [RecastAsync] annotations.
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

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        elements<RecastSync>(roundEnv).plus(elements<RecastAsync>(roundEnv))
            .forEach { element: Element ->
                val packageName = packageName(element)

                when (element.kind) {

                    ElementKind.METHOD -> {
                        val function = element as ExecutableElement
                        if (function.isEnclosed()) return@forEach
                        buildSyncFunction(function)?.apply { write(packageName, nextId++.toString(), listOf(this)) }
                        buildAsyncFunction(function)?.apply { write(packageName, nextId++.toString(), listOf(this)) }
                    }

                    ElementKind.CLASS -> {
                        val functions = mutableListOf<FunSpec>()
                        val sync: RecastSync? = element.getAnnotation(RecastSync::class.java)
                        val async: RecastAsync? = element.getAnnotation(RecastAsync::class.java)
                        for (elem: Element in element.enclosedElements.filter { it.kind == ElementKind.METHOD }) {
                            val function = elem as ExecutableElement
                            buildSyncFunction(function, sync)?.apply { functions.add(this) }
                            buildAsyncFunction(function, async)?.apply { functions.add(this) }
                        }
                        write(packageName, element.simpleName.toString(), functions)
                    }

                    else -> e("invalid recast annotation on: ${element.simpleName}")
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
    private fun buildAsyncFunction(function: ExecutableElement, default: RecastAsync? = null): FunSpec? {
        if (!function.isRecastValid()) return null
        val async: RecastAsync = function.getAnnotation(RecastAsync::class.java) ?: default ?: return null
        val parameters = function.parametersOf()
        val receiver = function.receiver()
        val callbackType = kotlin.Function1::class.asClassName()
            .parameterizedBy(com.andrewemery.recast.job.Result::class.asClassName().parameterizedBy(function.suspendingType), Unit::class.asClassName())

        return FunSpec.builder(function.simpleName.toString() + async.suffix)
            .apply { if (receiver != null) receiver(receiver) }
            .addParameters(parameters)
            .addParameter(ParameterSpec.builder("scope", CoroutineScope::class).defaultValue("%M()", MemberName("kotlinx.coroutines", "MainScope")).build())
            .addParameter("callback", callbackType)
            .returns(com.andrewemery.recast.job.Job::class)
            .addCode(
                CodeBlock.builder()
                    .add("return %M(scope, operation = { ", MemberName("com.andrewemery.recast.coroutines", "runBackground"))
                    .addFunctionCall(function)
                    .add(" }, callback = callback)")
                    .build()
            ).build()
    }

    /**
     * Get the annotations supported by this processor.
     */
    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(RecastSync::class.java.canonicalName, RecastAsync::class.java.canonicalName)
    }
}

/**
 * Get whether or not an annotated method is enclosed within a annotated class.
 */
private fun ExecutableElement.isEnclosed(): Boolean {
    val enclosing = enclosingElement
    return enclosing != null && (enclosing.getAnnotation(RecastSync::class.java) != null || enclosing.getAnnotation(RecastAsync::class.java) != null)
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
