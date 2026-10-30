package com.linx3141.mexpa.data.store

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.linx3141.mexpa.domain.model.Subscription
import com.linx3141.mexpa.domain.model.orderedOverrideIds
import com.linx3141.mexpa.platform.ProfileFileManager

@Serializable
private data class TransformSpec(
    val overrides: List<OverrideSpec>,
)

/** 将订阅选择的覆写清单写成内核 --transform 使用的快照。 */
class ProfileTransformWriter(
    private val fileManager: ProfileFileManager,
    private val overrides: OverrideProfileStore,
) {
    private val json = Json { encodeDefaults = true }

    fun write(
        subscription: Subscription?,
        relativePath: String,
        replacement: OverrideReplacement? = null,
    ): String? {
        val target = java.io.File(fileManager.getMihomoWorkDir(), relativePath)
        val specs = subscription?.orderedOverrideIds?.let { overrides.specs(it, replacement) }.orEmpty()
        if (specs.isEmpty()) {
            target.delete()
            return null
        }
        fileManager.writeMihomoFile(relativePath, json.encodeToString(TransformSpec.serializer(), TransformSpec(specs)))
        return target.path
    }

    companion object {
        const val RUNTIME_PATH = "profile.transform.json"
    }
}
