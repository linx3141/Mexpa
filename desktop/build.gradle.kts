import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(projects.sharedUi)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.miuix.ui.kmp)
    implementation(libs.miuix.icons.kmp)
    implementation(libs.miuix.nav.kmp)
    implementation(libs.miuix.blur.kmp)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation("net.java.dev.jna:jna:5.6.0")
}

val desktopVersionName = "1.0.0"
val desktopVersionCode = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 1 }

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Version" to desktopVersionName,
            "Implementation-Build" to desktopVersionCode.get(),
        )
    }
}

val isWindowsHost = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
val isMacHost = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

compose.desktop {
    application {
        mainClass = "com.linx3141.mexpa.desktop.MainKt"
        // Gradle daemon 可能固定在 JDK 21；需要时用 -Pdesktop.javaHome=... 指定 jpackage JDK
        providers.gradleProperty("desktop.javaHome").orNull?.let { javaHome = it }

        nativeDistributions {
            if (isWindowsHost) {
                targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            } else if (isMacHost) {
                targetFormats(TargetFormat.Dmg)
            } else {
                targetFormats(TargetFormat.Deb)
            }
            packageName = "mexpa"
            packageVersion = "1.0.0"
            description = when {
                isWindowsHost -> "Mexpa for Windows"
                isMacHost -> "Mexpa for macOS"
                else -> "Mexpa for Linux"
            }
            vendor = "linx3141"
            if (isWindowsHost) {
                windows {
                    menuGroup = "Network"
                    shortcut = true
                    // Windows jpackage 需要 .ico；生成脚本见 packaging/windows
                    iconFile.set(rootProject.file("packaging/windows/mexpa.ico"))
                    upgradeUuid = "6f6e2f5c-3a0e-4f6b-9b2e-2f2b6c9b7f01"
                    dirChooser = true
                    perUserInstall = true
                }
            } else if (isMacHost) {
                macOS {
                    bundleID = "com.linx3141.mexpa"
                    dockName = "Mexpa"
                }
            } else {
                linux {
                    menuGroup = "Network"
                    shortcut = true
                    // 与窗口图标/关于页共用同一份 PNG（Compose 资源里那份）
                    iconFile.set(
                        rootProject.file(
                            "shared-ui/src/commonMain/composeResources/drawable/mexpa_icon.png",
                        ),
                    )
                }
            }
        }
    }
}
