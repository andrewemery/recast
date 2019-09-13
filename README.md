# Recast

Recast turns Kotlin Multiplatform coroutines into consumable iOS methods.

## Introduction

&nbsp;1. Define a suspending function in your Kotlin multiplatform project and annotate:

```kotlin
@Recast
suspend fun getUser(id: Int): User { ... }
```

&nbsp;2. At compile-time, an equivalent asynchronous [1] method is generated:

```kotlin
fun getUser(id: Int, callback: (Result<User>) -> Unit): Job
```

&nbsp;3. Which can be consumed in your iOS project:

```swift
getUser("12") { (result: Result<User>) -> Void in
    let user: User? = result.getOrNull()
    ...
}
```

[1] Asynchronous: run on a background thread. See [Limitations](#asynchronous-limitations) for more information. 

## Overview

When using Kotlin multiplatform to share code between Android and iOS, suspending functions cannot be directly called from an iOS project.

Recast automatically creates synchronous or asynchronous methods that can be consumed within your iOS project.

## Integration

Recast uses annotation processing to generate Kotlin code that can be consumed within your iOS project.

The sample below shows how Recast can be integrated into a Multiplatform project:
1. The iOS source set is updated to include the generated code from the annotation processor.
2. Tasks that build iOS artifacts are updated to ensure the annotation processor is run beforehand.

```groovy
kotlin {
    commonMain {
        dependencies {
            implementation "com.andrewemery:recast:0.0.1"
        }
    }

    iosArm64Main {
        dependsOn commonMain
        kotlin.srcDirs += "build/generated/source/kaptKotlin/main"
    }
}

afterEvaluate {
    tasks.each { task ->
        if (task.name.startsWith('link') && task.name.contains('Ios')) {
            task.dependsOn 'kaptKotlinJvm'
        }
    }
}

dependencies {
    kapt "com.andrewemery:recast-compiler:0.0.1"
}
```

## Recast: asynchronous

#### Basics

When the ```@Recast``` annotation is applied:

```kotlin
@Recast
suspend fun getUser(id: Int): User { ... }
```

At compile-time, an equivalent asynchronous method is generated:

```kotlin
fun getUser(id: Int, callback: (Result<User>) -> Unit): Job
```

Which can then be consumed in your iOS project:

```swift
getUser("12") { (result: Result<User>) -> Void in
    let user: User? = result.getOrNull()
    ...
}
```

#### Coroutine scope

By default, the generated asynchronous method does not take a coroutine scope as a parameter.
In such instances, the ```GlobalScope``` is used to scope the coroutine.

If a custom scope is desired, the annotation can be adjusted to suit:

```kotlin
@Recast(scoped = true)
suspend fun getUser(id: Int): User { ... }
```

Which generates:

```kotlin
fun getUser(id: Int, scope: CoroutineScope, callback: (Result<User>) -> Unit): Job
```

Which can then be consumed in your iOS project:

```swift
let scope = CoroutinesKt.supervisorScope()
getUser("12", scope) { (result: Result<User>) -> Void in
    let user: User? = result.getOrNull()
    ...
}
```

#### Cancellation

As shown above, the generated method can pass a coroutine scope that can be used to cancel all scoped operations:

```swift
let scope = CoroutinesKt.supervisorScope()
getUser("12", scope) { ... }
scope.cancel()
```

The asynchronous method also returns a ```Job``` that can be used to cancel a single operation:

```swift
let job = getUser("12") { ... }
job.cancel()
```

#### Method suffix

If desired, a suffix can be added to the method name:

```kotlin
@Recast(suffix = "Async")
suspend fun getUser(id: Int): User { ... }
```

To produce:

```kotlin
fun getUserAsync(id: Int, callback: (Result<User>) -> Unit): Job
```

#### Asynchronous limitations

At present, multithreaded coroutines are currently [unsupported](https://github.com/Kotlin/kotlinx.coroutines/issues/462) in Kotlin/Native (the platform that iOS applications target).

To workaround this, coroutines annotated with ```@Recast``` must be called from the iOS main thread. 
The result of the asynchronous operation will also be returned to the main thread:

```swift
getUser("12") { (result: Result<User>) -> Void in  // must be called on the main thread
    let user: User? = result.getOrNull()  // result returned to the main thread
    ...
}
```

An alternative to this approach is to use the ```RecastSync``` annotation and manage threading within your iOS project natively.

## Recast: synchronous

#### Basics

When the ```RecastSync``` annotation is applied:

```kotlin
@RecastSync
suspend fun getUser(id: Int): User { ... }
```

At compile-time, an equivalent synchronous method is generated (with ```Sync``` added as a suffix to the method name):

```kotlin
fun getUserSync(id: Int): User
```

Which can then be consumed in your iOS project:

```swift
let user: User = getUserSync("12")
```

#### Method suffix

If desired, a custom method suffix can be used instead:

```kotlin
@RecastSync(suffix = "Synchronous")
suspend fun getUser(id: Int): User { ... }
```

To produce:

```kotlin
fun getUserSynchronous(id: Int): User
```

## Annotation support

#### Basics

Recast annotations can be added to the following objects:
1. Function
2. Class
3. Interface
4. Object

When the annotation is added to a class, interface or object:
 
```kotlin
@Recast
class UserRepository {
    suspend fun getUser(id: Int): User { ... }
    suspend fun getUsers(): List<User> { ... }
}
```

Equivalent extension functions are generated for all suspending functions on the target class:

```kotlin
fun UserRepository.getUser(id: Int, callback: (Result<User>) -> Unit): Job = ...
fun UserRepository.getUsers(callback: (Result<List<User>>) -> Unit): Job = ...
```

#### Overrides

Annotations set against methods override those set against a parent. For example, the following code:
 
```kotlin
@Recast(scoped = true)
class UserRepository {
    @Recast(suffix = "Async")
    suspend fun getUser(id: Int): User { ... }
}
```

Generates the following method (note how the the method annotation has overriden the parent):

```kotlin
fun UserRepository.getUserAsync(id: Int, callback: (Result<User>) -> Unit): Job = ...
```