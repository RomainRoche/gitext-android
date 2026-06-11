package io.gitrad.sdk.data.datasources

import android.content.Context
import io.gitrad.sdk.data.dtos.TranslationPayloadDto

internal class BundleTranslationDataSource(private val context: Context) {

    fun load(): TranslationPayloadDto? {
        return try {
            val json = context.assets.open("gitrad-baseline/translations.json")
                .bufferedReader().use { it.readText() }
            TranslationPayloadDto.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }
}
