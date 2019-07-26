@file:Suppress("unused")

package com.andrewemery.recast.processor

import com.andrewemery.recast.annotation.RecastAsync
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
        roundEnv.getElementsAnnotatedWith(RecastSync::class.java).forEach { element: Element ->

            // return if the annotation is not against an interface or class
            if (element.kind !in listOf(ElementKind.METHOD) || element !is ExecutableElement || !element.suspending)
                return e("Recast must annotate a suspending function").let { false }

            val function: ExecutableElement = element
            val recast: RecastSync = element.getAnnotation(RecastSync::class.java)
            val suspendingType: TypeName = function.suspendingType
            val parameters = function.parametersOf()
            val enclosing = element.enclosingElement

            val builder = FunSpec.builder(element.simpleName.toString() + recast.suffix)
                .addParameters(parameters)
                .returns(suspendingType)
            if (enclosing != null) builder.receiver(enclosing.asType().asTypeName())
            builder.addCode(
                "return %M { %L({}) }".replace("{}", (0 until parameters.size).joinToString { "%L" }),
                MemberName("com.andrewemery.recast.coroutines", "runBlocking"),
                function.simpleName.toString(),
                *(parameters.map { it.name }).toTypedArray()
            )

            // create the file specification
            println("package name: " + packageName(element))
            val file = FileSpec.builder(packageName(element), nextId++.toString())
                .addFunction(builder.build())
                .build()

            // write the file
            write(file)
        }

        roundEnv.getElementsAnnotatedWith(RecastAsync::class.java).forEach { element: Element ->

            // return if the annotation is not against an interface or class
            if (element.kind !in listOf(ElementKind.METHOD) || element !is ExecutableElement || !element.suspending)
                return e("Recast must annotate a suspending function").let { false }

            val function: ExecutableElement = element
            val recast: RecastAsync = element.getAnnotation(RecastAsync::class.java)
            val suspendingType: TypeName = function.suspendingType
            val parameters = function.parametersOf()
            val enclosing = element.enclosingElement
            val callbackType = kotlin.Function1::class.asClassName()
                .parameterizedBy(com.andrewemery.recast.job.Result::class.asClassName()
                    .parameterizedBy(suspendingType), Unit::class.asClassName())

            val builder = FunSpec.builder(element.simpleName.toString() + recast.suffix)
                .addParameters(parameters)
                .addParameter(ParameterSpec.builder("scope", CoroutineScope::class)
                    .defaultValue("%M()", MemberName("kotlinx.coroutines", "MainScope"))
                    .build())
                .addParameter("callback", callbackType)
                .returns(com.andrewemery.recast.job.Job::class)
            if (enclosing != null) builder.receiver(enclosing.asType().asTypeName())
            builder.addCode(
                "return %M(scope, operation = { %L({}) }, callback = callback)".replace("{}", (0 until parameters.size).joinToString { "%L" }),
                MemberName("com.andrewemery.recast.coroutines", "runBackground"),
                function.simpleName.toString(),
                *(parameters.map { it.name }).toTypedArray()
            )

            // create the file specification
            println("package name: " + packageName(element))
            val file = FileSpec.builder(packageName(element), nextId++.toString())
                .addFunction(builder.build())
                .build()

            // write the file
            write(file)
        }

        return true
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * supported annotation types
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(RecastSync::class.java.canonicalName, RecastAsync::class.java.canonicalName)
    }

}


