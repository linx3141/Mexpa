package com.linx3141.mexpa.desktop

import java.security.KeyPairGenerator
import java.security.interfaces.XECPrivateKey
import java.security.interfaces.XECPublicKey
import java.security.spec.NamedParameterSpec
import java.io.File

data class AgeKeyPair(val secretKey: String, val publicKey: String)

/**
 * age 密钥生成（X25519，classic 格式）。
 *
 * 密钥字节取自 JDK 的 X25519 KeyPair（PKCS#8 / X.509 末尾 32 字节即 raw key），
 * 再按 age 的 bech32 规则编码：私钥 HRP `AGE-SECRET-KEY-`（大写），公钥 HRP `age`（小写）。
 * 不支持 mlkem768-x25519 混合密钥（需要额外后量子实现，Linux 端暂只生成 classic）。
 */
object AgeKey {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val GENERATOR = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    fun generate(): AgeKeyPair {
        val generator = KeyPairGenerator.getInstance("X25519")
        generator.initialize(NamedParameterSpec.X25519)
        val pair = generator.generateKeyPair()
        val secretBytes = (pair.private as XECPrivateKey).scalar.orElse(null)
            ?: pair.private.encoded.takeLast(32).toByteArray()
        val publicBytes = (pair.public as XECPublicKey).u.let { u ->
            // u 是正整数；按无符号大端补足 32 字节
            val bytes = u.toByteArray()
            when {
                bytes.size == 32 -> bytes
                bytes.size > 32 -> bytes.takeLast(32).toByteArray()
                else -> ByteArray(32 - bytes.size) + bytes
            }
        }
        return AgeKeyPair(
            secretKey = bech32Encode("AGE-SECRET-KEY-", secretBytes).uppercase(),
            publicKey = bech32Encode("age", publicBytes),
        )
    }

    /** 调内核生成 hybrid 密钥（mihomo 自带 `age keygen-pq`，与 Android native 同一实现）。 */
    fun hybrid(binaryPath: String?): AgeKeyPair? {
        val binary = resolveBinary(binaryPath) ?: return null
        val output = runCatching {
            val process = ProcessBuilder(binary.path, "age", "keygen-pq").redirectErrorStream(true).start()
            val text = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            text
        }.getOrNull() ?: return null
        val publicKey = output.lineSequence().firstOrNull { it.startsWith("# public key:") }
            ?.substringAfter(":")?.trim()
        val secretKey = output.lineSequence().lastOrNull { it.startsWith("AGE-SECRET-KEY-") }?.trim()
        return if (!publicKey.isNullOrBlank() && !secretKey.isNullOrBlank()) {
            AgeKeyPair(secretKey = secretKey, publicKey = publicKey)
        } else null
    }

    private fun resolveBinary(binaryPath: String?): File? {
        System.getenv("MIHOMO_BIN")?.takeIf { it.isNotBlank() }?.let { File(it).takeIf(File::isFile)?.let { f -> return f } }
        binaryPath?.takeIf { it.isNotBlank() }?.let { path ->
            val file = File(path)
            if (file.isFile && file.canExecute()) return file
            File(File(System.getProperty("user.dir")), path).takeIf { it.isFile }?.let { return it }
        }
        val cwd = File(System.getProperty("user.dir"))
        listOf(cwd, cwd.parentFile, File(cwd, "desktop"), File(cwd.parentFile ?: cwd, "desktop"))
            .filterNotNull().forEach { dir ->
                File(dir, "mihomo-linux").takeIf { it.canExecute() }?.let { return it }
            }
        return null
    }

    internal fun bech32Encode(hrp: String, data: ByteArray): String {
        val values = convertBits(data.map { it.toInt() and 0xff }, 8, 5, true)
        val checksum = createChecksum(hrp, values)
        val builder = StringBuilder(hrp).append('1')
        (values + checksum).forEach { builder.append(CHARSET[it]) }
        return builder.toString()
    }

    private fun createChecksum(hrp: String, values: List<Int>): List<Int> {
        val expanded = hrpChecksumInput(hrp) + values + List(6) { 0 }
        var polymod = 1
        expanded.forEach { value ->
            val top = polymod ushr 25
            polymod = ((polymod and 0x1ffffff) shl 5) xor value
            for (i in 0 until 5) {
                if ((top ushr i) and 1 == 1) polymod = polymod xor GENERATOR[i]
            }
        }
        val checksum = polymod xor 1
        return List(6) { i -> (checksum ushr (5 * (5 - i))) and 0x1f }
    }

    private fun hrpChecksumInput(hrp: String): List<Int> {
        val high = hrp.map { it.code ushr 5 }
        val low = hrp.map { it.code and 0x1f }
        return high + listOf(0) + low
    }

    private fun convertBits(data: List<Int>, from: Int, to: Int, pad: Boolean): List<Int> {
        var acc = 0
        var bits = 0
        val result = mutableListOf<Int>()
        val maxValue = (1 shl to) - 1
        for (value in data) {
            acc = (acc shl from) or (value and ((1 shl from) - 1))
            bits += from
            while (bits >= to) {
                bits -= to
                result += (acc ushr bits) and maxValue
            }
        }
        if (pad && bits > 0) result += (acc shl (to - bits)) and maxValue
        return result
    }
}
