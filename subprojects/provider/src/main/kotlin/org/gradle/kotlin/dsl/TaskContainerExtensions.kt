/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.kotlin.dsl

import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

import org.gradle.kotlin.dsl.support.illegalElementType
import org.gradle.kotlin.dsl.support.uncheckedCast

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.safeCast


/**
 * Allows a [TaskContainer] to be configured via an augmented DSL that includes
 * a shorthand string notation for configuring existing tasks.
 *
 * ```kotlin
 * tasks {
 *     "build" {
 *         dependsOn("clean")
 *     }
 *     "clean"(Delete::class) {
 *         delete("./build")
 *     }
 * }
 * ```
 *
 * @param configuration The expression to configure this [TaskContainer] with.
 * @return The given [TaskContainer].
 */
inline operator fun TaskContainer.invoke(
    configuration: TaskContainerScope.() -> Unit
): TaskContainer =

    apply {
        configuration(TaskContainerScope(this))
    }


/**
 * Provides a [TaskProvider] delegate for the task named after the property.
 */
operator fun ExistingDomainObjectDelegateProvider<out TaskContainer>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate(
    delegateProvider.named(property.name)
)


/**
 * Provides a [TaskProvider] delegate for the task named after the property after configuring it with the given action.
 */
operator fun ExistingDomainObjectDelegateProviderWithAction<out TaskContainer, Task>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate(
    delegateProvider.named(property.name).apply { configure(action) }
)


/**
 * Provides a [TaskProvider] delegate for the task of the given type named after the property.
 */
operator fun <U : Task> ExistingDomainObjectDelegateProviderWithType<out TaskContainer, U>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate(
    delegateProvider.named(property.name, type)
)


/**
 * Registers a task and provides a delegate with the resulting [TaskProvider].
 */
operator fun RegisteringDomainObjectDelegateProvider<out TaskContainer>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate(
    delegateProvider.register(property.name)
)


/**
 * Registers a task that gets configured with the given action and provides a delegate with the resulting [TaskProvider].
 */
operator fun RegisteringDomainObjectDelegateProviderWithAction<out TaskContainer, Task>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate(
    delegateProvider.register(property.name, action)
)


/**
 * Registers a task of the given type and provides a delegate with the resulting [TaskProvider].
 */
operator fun <U : Task> RegisteringDomainObjectDelegateProviderWithType<out TaskContainer, U>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate(
    delegateProvider.register(property.name, type.java)
)


/**
 * Registers a task of the given type that gets configured with the given action and provides a delegate with the resulting [TaskProvider].
 */
operator fun <U : Task> RegisteringDomainObjectDelegateProviderWithTypeAndAction<out TaskContainer, U>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate(
    delegateProvider.register(property.name, type.java, action)
)


/**
 * Receiver for the `tasks` block providing an extended set of operators for the configuration of tasks.
 */
class TaskContainerScope(val container: TaskContainer) : TaskContainer by container {

    /**
     * Configures a task by name, without triggering its creation or configuration, failing if there is no such task.
     *
     * @see [TaskContainer.named]
     * @see [TaskProvider.configure]
     */
    operator fun String.invoke(configuration: Task.() -> Unit): TaskProvider<Task> =
        this().apply { configure(configuration) }

    /**
     * Locates a task by name, without triggering its creation or configuration, failing if there is no such task.
     *
     * @see [TaskContainer.named]
     */
    operator fun String.invoke(): TaskProvider<Task> =
        container.named(this)

    /**
     * Configures a task by name, without triggering its creation or configuration, failing if there is no such task.
     *
     * @see [TaskContainer.named]
     * @see [TaskProvider.configure]
     */
    operator fun <U : Task> String.invoke(type: KClass<U>, configuration: U.() -> Unit): TaskProvider<U> =
        container.named(this, type, configuration)

    /**
     * Locates a task by name and type, without triggering its creation or configuration, failing if there is no such task.
     *
     * @see [TaskContainer.named]
     */
    operator fun <U : Task> String.invoke(type: KClass<U>): TaskProvider<U> =
        container.named(this, type)
}


/**
 * Locates a task by name and type, without triggering its creation or configuration, failing if there is no such task.
 *
 * @see [TaskContainer.named]
 */
@Suppress("extension_shadowed_by_member")
inline fun <reified T : Task> TaskContainer.named(name: String): TaskProvider<T> =
    named(name, T::class)


/**
 * Locates a task by name and type, without triggering its creation or configuration, failing if there is no such task.
 *
 * @see [TaskContainer.named]
 */
fun <T : Task> TaskContainer.named(name: String, type: KClass<T>): TaskProvider<T> =
    named(name, type) {}


/**
 * Configures a task by name and type, without triggering its creation or configuration, failing if there is no such task.
 *
 * @see [TaskContainer.named]
 * @see [TaskProvider.configure]
 */
fun <T : Task> TaskContainer.named(name: String, type: KClass<T>, configuration: T.() -> Unit): TaskProvider<T> =
    uncheckedCast(named(name).also { provider ->
        provider.configure { obj ->
            configuration(
                type.safeCast(obj)
                    ?: throw illegalElementType(this@named, name, type, obj::class)
            )
        }
    })


/**
 * Creates a [Task] with the given [name] and type, passing the given arguments to the [javax.inject.Inject]-annotated constructor,
 * and adds it to this project tasks container.
 */
inline fun <reified T : Task> TaskContainer.create(name: String, vararg arguments: Any) =
    create(name, T::class.java, *arguments)
