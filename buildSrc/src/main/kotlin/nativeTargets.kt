/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package net.devrieze.gradle.ext

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.project
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.HostManager
import java.util.*

enum class Host {
    Windows,
    Macos,
    Linux
}

enum class NativeState {
    ALL, HOST, SINGLE, DISABLED
}

private typealias TargetFun = KotlinMultiplatformExtension.() -> Unit

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinMultiplatformExtension.applyDefaultXmlUtilHierarchyTemplate() {
    applyHierarchyTemplate(defaultXmlUtilHierarchyTemplate)

}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
private val defaultXmlUtilHierarchyTemplate  = KotlinHierarchyTemplate {
    withSourceSetTree(KotlinSourceSetTree.main, KotlinSourceSetTree.test)

    common {
        withCompilations { c -> c.target.platformType !in arrayOf(KotlinPlatformType.jvm, KotlinPlatformType.wasm) }

        group("javaShared") {
            withCompilations { c ->
                c.target.platformType == KotlinPlatformType.jvm && "jvm" !in c.target.name
            }

            group("commonJvm") {
                withCompilations { c ->
                    c.target.platformType == KotlinPlatformType.jvm && "jvm" in c.target.name
                }
            }
        }

        group("commonDom") {

            withWasm()

            group("native") {
                group("apple") {
                    withApple()

                    group("ios") {
                        withIos()
                    }

                    group("tvos") {
                        withTvos()
                    }

                    group("watchos") {
                        withWatchos()
                    }

                    group("macos") {
                        withMacos()
                    }
                }

                group("linux") {
                    withLinux()
                }

                group("mingw") {
                    withMingw()
                }

                group("androidNative") {
                    withAndroidNative()
                }

            }
        }
    }
}

fun Project.addNativeTargets() {
    val ideaActive = System.getProperty("idea.active") == "true"
    val nativeState = when(property("native.deploy")?.toString()?.lowercase()) {
        "all", "true" -> NativeState.ALL
        "host" -> NativeState.HOST
        "disabled" -> NativeState.DISABLED
        else -> NativeState.SINGLE
    }
    val singleTargetMode = ideaActive || nativeState == NativeState.SINGLE

    val ext = extensions.getByName<ExtraPropertiesExtension>("ext")
    val manager = HostManager()//ext["hostManager"] as HostManager
    val kotlin = extensions.getByName<KotlinMultiplatformExtension>("kotlin")

    val hostTarget = manager.targetByName("host")

    val host = when {
        hostTarget.name.startsWith("mingw") -> Host.Windows
        hostTarget.name.startsWith("macos") -> Host.Macos
        else -> Host.Linux
    }

    ext["ideaPreset"] = when (host) {
        Host.Windows -> fun KotlinMultiplatformExtension.() { mingwX64() }// presets.nativePreset("mingwX64")
        Host.Macos -> fun KotlinMultiplatformExtension.() { macosX64() }//presets.nativePreset("macosX64")
        Host.Linux -> fun KotlinMultiplatformExtension.() { linuxX64() } //presets.nativePreset("linuxX64")
    }

    val nativeMainSets = mutableListOf<KotlinSourceSet>()
    val nativeTestSets = mutableListOf<KotlinSourceSet>()

    fun KotlinNativeTarget.addSourceSets() {
        nativeMainSets.add(compilations.getByName("main").kotlinSourceSets.first())
        nativeTestSets.add(compilations.getByName("test").kotlinSourceSets.first())
    }

    if (nativeState != NativeState.DISABLED) {
        with(kotlin) {
            if (singleTargetMode) {
                @Suppress("UNCHECKED_CAST") val targetFun = ext["ideaPreset"] as TargetFun
                targetFun()
            } else {
                if(true) {
                    if (nativeState != NativeState.HOST || host == Host.Linux) {
                        linuxX64 { addSourceSets() }
                        linuxArm32Hfp { addSourceSets() }
                        linuxArm64 { addSourceSets() }
                    }

                    if (nativeState != NativeState.HOST || host == Host.Macos) {
                        macosX64 { addSourceSets() }
                        macosArm64 { addSourceSets() }
                        iosArm64 { addSourceSets() }
                        iosSimulatorArm64 { addSourceSets() }
                        iosX64 { addSourceSets() }

                        watchosSimulatorArm64() { addSourceSets() }
                        watchosX64 { addSourceSets() }
                        watchosArm32 { addSourceSets() }
                        watchosArm64 { addSourceSets() }

                        tvosSimulatorArm64 { addSourceSets() }
                        tvosArm64 { addSourceSets() }
                        tvosX64 { addSourceSets() }
                    }

                    if (nativeState != NativeState.HOST || host == Host.Windows) {
                        mingwX64 { addSourceSets() }
                    }
                }
            }

            @OptIn(ExternalVariantApi::class)
            project.logger.debug("Registering :${project.name}:nativeTest")
            @OptIn(ExternalVariantApi::class)
            val nativeTest = project.tasks.register("nativeTest") {
                val testTasks = tasks.withType<KotlinNativeTest>().filter {
                    it is KotlinNativeHostTest &&
                            hostTarget.family.name in it.targetName!!.uppercase() &&
                            hostTarget.architecture.name in it.targetName!!.uppercase()
                }
                project.logger.debug("Configuring ${path} with hostTarget: ${hostTarget.visibleName} to depend on ${testTasks.joinToString { it.path}}")
                dependsOn(testTasks)
            }
        }

    }

}

private fun KotlinMultiplatformExtension.targets(configure: Action<Any>): Unit =
    (this as ExtensionAware).extensions.configure("targets", configure)

private fun KotlinMultiplatformExtension.sourceSets(configure: Action<org.gradle.api.NamedDomainObjectContainer<KotlinSourceSet>>): Unit =
    (this as ExtensionAware).extensions.configure("sourceSets", configure)

val Project.isWasmSupported: Boolean get() = true
