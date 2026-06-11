package io.gitrad.sdk.data.datasources

import android.content.Context
import io.gitrad.sdk.data.dtos.TranslationPayloadDto
import java.io.File

internal class LocalTranslationDataSource(
    context: Context,
    cacheId: String,
) {
    private val cacheFile = File(context.cacheDir, "gitrad/$cacheId/translations.json")

    @Volatile
    private var lastSaveTime: Long? = null

    fun read(): TranslationPayloadDto? {
        if (!cacheFile.exists()) return null
        return try {
            val json = cacheFile.readText()
            TranslationPayloadDto.fromJson(json)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun write(dto: TranslationPayloadDto) {
        lastSaveTime = System.currentTimeMillis()
        try {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(TranslationPayloadDto.toJson(dto))
        } catch (_: Exception) {
            // Non-fatal: in-memory payload continues to serve.
        }
    }

    fun clear() {
        cacheFile.delete()
    }

    fun modificationDate(): Long? {
        return lastSaveTime ?: cacheFile.takeIf { it.exists() }?.lastModified()
    }
}
