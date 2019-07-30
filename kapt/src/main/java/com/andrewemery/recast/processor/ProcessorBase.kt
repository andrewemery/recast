package com.andrewemery.recast.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic
import kotlin.coroutines.Continuation
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

/**
 * The [ProcessorBase] is a root class to simplify annotation processing.
 */
internal abstract class ProcessorBase : AbstractProcessor() {
    internal abstract val environment: ProcessingEnvironment
}

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * type name: java to kotlin type
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

/**
 * Convert the type into an equivalent kotlin type where applicable.
 */
internal fun TypeName.javaToKotlinType(): TypeName =
    if (this is ParameterizedTypeName) (rawType.javaToKotlinType() as ClassName).parameterizedBy(*typeArguments.map { it.javaToKotlinType() }.toTypedArray())
    else JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()?.let {
        ClassName.bestGuess(it)
    } ?: this

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * executable element: suspending
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

/**
 * Get whether or not the executable element representing a function is suspending function.
 */
internal val ExecutableElement.suspending: Boolean
    get() {
        val pc: VariableElement? = parameters.getOrNull(parameters.size - 1)
        val ppc: ParameterizedTypeName? = pc?.asType()?.asTypeName() as? ParameterizedTypeName
        return ppc?.rawType?.canonicalName == Continuation::class.java.canonicalName ?: false
    }

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * executable element: suspending type
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

/**
 * Get the return type of the executable element representing a suspending function.
 */
internal val ExecutableElement.suspendingType: TypeName
    get() {
        if (!suspending) throw IllegalStateException("element must be a suspending function: $this")
        val pc: VariableElement? = parameters.getOrNull(parameters.size - 1)
        val ppc: ParameterizedTypeName? = pc?.asType()?.asTypeName() as? ParameterizedTypeName
        return (ppc!!.typeArguments[0] as WildcardTypeName).inTypes[0].javaToKotlinType()
    }

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * executable element: parameters of
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

/**
 * Get the parameter specifications of the executable element representing a function.
 */
internal fun ExecutableElement.parametersOf(): List<ParameterSpec> {
    val params = parameters.map { it.parameterOf() }
    return if (suspending) params.dropLast(1) else params
}

/**
 * Get the parameter specification of the variable element representing a function parameter.
 */
internal fun VariableElement.parameterOf(): ParameterSpec {
    val annotationSpecs = annotationMirrors.map { AnnotationSpec.get(it) }
    val notNullAnnotationSpec = AnnotationSpec.builder(NotNull::class).build()
    val nullableAnnotationSpec = AnnotationSpec.builder(Nullable::class).build()
    val nullable = annotationSpecs.contains(nullableAnnotationSpec)

    return ParameterSpec.get(this)
        .run { toBuilder(type = type.javaToKotlinType().copy(nullable = nullable)) }
        .addAnnotations(annotationSpecs - notNullAnnotationSpec - nullableAnnotationSpec)
        .build()
}

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * annotation mirror: spec of
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

/**
 * Get the annotation specification from the given annotation mirror.
 *
 * Note: Implementation provided to work around issue generating variable argument value with the default value:
 * @Headers(["Content-Type:application/json"])
 * Type inference failed: Required: String Found: Array<String>
 */
internal fun AnnotationMirror.specOf(): AnnotationSpec {
    val spec = AnnotationSpec.get(this)
    if (spec.members.isEmpty() || elementValues.keys.toList()[0].simpleName.toString() != "value") return spec
    return spec.toBuilder().apply {
        members[0] = CodeBlock.of("value = %L", members[0].toString())
    }.build()
}

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * code block
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

internal fun CodeBlock.Builder.addFunctionCall(function: ExecutableElement): CodeBlock.Builder {
    return addFunctionCall(function, function.parametersOf())
}

internal fun CodeBlock.Builder.addFunctionCall(function: ExecutableElement, parameters: List<ParameterSpec>):
        CodeBlock.Builder {
    return this.add("%L({})".replace("{}", (0 until parameters.size).joinToString { "%L" }),
        function.simpleName.toString(), *(parameters.map { it.name }).toTypedArray())
}

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * processor base
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

/**
 * Retrieve the name of the package of the given element.
 *
 * @param env The annotation environment.
 */
internal inline fun <reified T : Annotation> ProcessorBase.elements(env: RoundEnvironment): Set<Element> =
    env.getElementsAnnotatedWith(T::class.java)

/**
 * Retrieve the name of the package of the given element.
 *
 * @param element The element (class, interface, etc.) to retrieve the package of.
 */
internal fun ProcessorBase.packageName(element: Element): String =
    environment.elementUtils.getPackageOf(element).toString()

/**
 * Log an error.
 *
 * @param string The error message to log.
 */
internal fun ProcessorBase.e(string: String) = environment.messager.printMessage(Diagnostic.Kind.ERROR, string)

/**
 * Log a warning.
 *
 * @param string The warning message to log.
 */
internal fun ProcessorBase.w(string: String) = environment.messager.printMessage(Diagnostic.Kind.WARNING, string)

/**
 * Write the file in the appropriate package.
 *
 * @param file The file specification write.
 */
internal fun ProcessorBase.write(file: FileSpec) {
    val f = File(environment.options["kapt.kotlin.generated"].orEmpty()).apply { mkdirs() }
    file.writeTo(f)
}
