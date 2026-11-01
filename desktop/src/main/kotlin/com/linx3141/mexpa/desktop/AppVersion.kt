package com.linx3141.mexpa.desktop

import java.io.File
import java.util.jar.JarFile

/** 版本号来自 jar manifest（Gradle 注入），不再在 UI 里写死。 */
object AppVersion {
    val name: String
    val code: Int

    init {
        var resolvedName = "1.0.0"
        var resolvedCode = 1
        runCatching {
            val location = AppVersion::class.java.protectionDomain?.codeSource?.location ?: return@runCatching
            val file = File(location.toURI())
            if (!file.isFile) return@runCatching
            JarFile(file).use { jar ->
                val manifest = jar.manifest ?: return@runCatching
                manifest.mainAttributes.getValue("Implementation-Version")?.let { resolvedName = it }
                manifest.mainAttributes.getValue("Implementation-Build")?.toIntOrNull()?.let { resolvedCode = it }
            }
        }
        name = resolvedName
        code = resolvedCode
    }
}
