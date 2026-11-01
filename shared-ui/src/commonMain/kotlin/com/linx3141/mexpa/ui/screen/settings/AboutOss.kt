package com.linx3141.mexpa.ui.screen.settings

fun aboutOssProjects(mihomoVersion: String): List<Pair<String, String>> =
    listOf(
        "Mishka" to "https://github.com/YuKongA/Mishka",
        "KernelSU" to "https://github.com/tiann/KernelSU",
        (if (mihomoVersion.isNotEmpty()) "mihomo ($mihomoVersion)" else "mihomo") to
            "https://github.com/MetaCubeX/mihomo",
        "miuix" to "https://github.com/compose-miuix-ui/miuix",
        "scripta" to "https://github.com/YuKongA/scripta",
        "AndroidHiddenApiBypass" to "https://github.com/LSPosed/AndroidHiddenApiBypass",
        "AndroidX" to "https://github.com/androidx/androidx",
        "Koin" to "https://github.com/InsertKoinIO/koin",
        "Kotlin" to "https://github.com/JetBrains/kotlin",
        "kotlinx.collections.immutable" to "https://github.com/Kotlin/kotlinx.collections.immutable",
        "kotlinx.coroutines" to "https://github.com/Kotlin/kotlinx.coroutines",
        "kotlinx-datetime" to "https://github.com/Kotlin/kotlinx-datetime",
        "kotlinx.serialization" to "https://github.com/Kotlin/kotlinx.serialization",
        "Ktor" to "https://github.com/ktorio/ktor",
        "quickie" to "https://github.com/G00fY2/quickie",
    )
