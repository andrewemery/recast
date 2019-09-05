# Recast [work in progress]

Recast Kotlin multiplatform coroutines into consumable iOS methods.

## Introduction

&nbsp;1. Define a suspending function in your Kotlin multiplatform project and annotate:

```kotlin
@RecastAsync
suspend fun getUser(id: Int): User = withContext(Dispatchers.IO) { ... }
```

&nbsp;2. At compile-time, an equivalent asynchronous method is generated:

```kotlin
fun getUser(id: Int, scope: CoroutineScope = MainScope(), callback: (Result<User>) -> Unit): Job
```

&nbsp;3. Consume the method in your iOS project:

```swift
getUser("12") { (result: Result<User>) -> Void in
    let user: User? = result.getOrNull()
    ...
}
```

## Overview

When using Kotlin multiplatform to share code between Android and iOS, the use of Kotlin coroutines is very desirable. However presently, suspending functions are cannot be consumed within an iOS project.
The simplest approach is to use coroutines internally within the multiplatform project and use synchronous calls or callbacks in the interface shared between Android and iOS.

Recast allows you to use coroutines throughout your multiplatform project and automatically creates synchronous or asynchronous methods that can be consumed within your iOS project.

## Integration

Recast uses annotation processing to generate Kotlin code that can be consumed within your iOS project.

The sample below shows how Recast can be integrated into a multiplatform project:
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

## RecastAsync

When the ```RecastAsync``` annotation is applied:

```kotlin
@RecastAsync
suspend fun getUser(id: Int): User = withContext(Dispatchers.IO) { ... }
```

At compile-time, an equivalent asynchronous method is generated:

```kotlin
fun getUser(id: Int, scope: CoroutineScope = MainScope(), callback: (Result<User>) -> Unit): Job
```

Which can then be consumed in your iOS project:

```swift
getUser("12") { (result: Result<User>) -> Void in
    let user: User? = result.getOrNull()
    ...
}
```

If desired, a suffix can be added to the method name:

```kotlin
@RecastAsync(suffix = "Async")
suspend fun getUser(id: Int): User = withContext(Dispatchers.IO) { ... }
```

To produce:

```kotlin
fun getUserAsync(id: Int, scope: CoroutineScope = MainScope(), callback: (Result<User>) -> Unit): Job
```

## RecastSync

When the ```RecastSync``` annotation is applied:

```kotlin
@RecastSync
suspend fun getUser(id: Int): User = withContext(Dispatchers.IO) { ... }
```

At compile-time, an equivalent synchronous method is generated (with ```Sync``` added as a suffix to the method name):

```kotlin
fun getUserSync(id: Int): User
```

Which can then be consumed in your iOS project:

```swift
let user: User = getUserSync("12")
```

If desired, a custom method suffix can be used instead:

```kotlin
@RecastSync(suffix = "Synchronous")
suspend fun getUser(id: Int): User = withContext(Dispatchers.IO) { ... }
```

To produce:

```kotlin
fun getUserSynchronous(id: Int): User
```

## Object support

Recast annotations can be added to the following objects:
1. Function
2. Class
3. Interface
4. Object

When the annotation is added to a class, interface or object:
 
```kotlin
@RecastAsync
class UserRepository {
    suspend fun getUser(id: Int): User = withContext(Dispatchers.IO) { ... }
    suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) { ... }
}
```

Equivalent extension functions are generated for all suspending functions on the target class:

```kotlin
fun UserRepository.getUser(id: Int, scope: CoroutineScope = MainScope(), callback: (Result<User>) -> Unit): Job = ...
fun UserRepository.getUsers(scope: CoroutineScope = MainScope(), callback: (Result<List<User>>) -> Unit): Job = ...
```

## Kotlin multiplatform coroutine support

The bad news about coroutine support for iOS applications is that multithreaded coroutines are currently [unsupported](https://github.com/Kotlin/kotlinx.coroutines/issues/462) in Kotlin/Native (the platform that iOS applications target).

The current workaround (implemented in the ```Dispatchers.IO``` value provided in this library) is to use the iOS main thread to execute coroutines on (which is more than likely unsuitable for most applications).

Until this issue is resolved it is recommended to use ```RecastSync``` to transform coroutines methods into synchronous, blocking calls and execute them within background threads managed natively (i.e. within iOS).