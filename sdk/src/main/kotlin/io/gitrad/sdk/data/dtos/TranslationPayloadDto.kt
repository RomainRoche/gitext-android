package io.gitrad.sdk.data.dtos

import org.json.JSONObject

internal sealed class TranslationEntryDto {
    data class Str(val value: String) : TranslationEntryDto()
    data class Plurals(val map: Map<String, String>) : TranslationEntryDto()
}

internal data class TranslationPayloadDto(
    val namespaces: List<String>,
    val translations: Map<String, Map<String, TranslationEntryDto>>,
) {
    companion object {
        fun fromJson(json: String): TranslationPayloadDto {
            val root = JSONObject(json)
            val namespaces = mutableListOf<String>()
            val translations = mutableMapOf<String, Map<String, TranslationEntryDto>>()

            val nsArray = root.optJSONArray("_namespaces")
            if (nsArray != null) {
                for (i in 0 until nsArray.length()) namespaces.add(nsArray.getString(i))
            }

            val keys = root.keys()
            while (keys.hasNext()) {
                val lang = keys.next()
                if (lang == "_namespaces") continue
                val langObj = root.optJSONObject(lang) ?: continue
                val entries = mutableMapOf<String, TranslationEntryDto>()
                val entryKeys = langObj.keys()
                while (entryKeys.hasNext()) {
                    val key = entryKeys.next()
                    val value = langObj.get(key)
                    entries[key] = when (value) {
                        is String -> TranslationEntryDto.Str(value)
                        is JSONObject -> {
                            val map = mutableMapOf<String, String>()
                            val pluralKeys = value.keys()
                            while (pluralKeys.hasNext()) {
                                val pk = pluralKeys.next()
                                map[pk] = value.getString(pk)
                            }
                            TranslationEntryDto.Plurals(map)
                        }
                        else -> TranslationEntryDto.Str(value.toString())
                    }
                }
                translations[lang] = entries
            }
            return TranslationPayloadDto(namespaces, translations)
        }

        fun toJson(dto: TranslationPayloadDto): String {
            val root = JSONObject()
            if (dto.namespaces.isNotEmpty()) {
                val arr = org.json.JSONArray()
                dto.namespaces.forEach { arr.put(it) }
                root.put("_namespaces", arr)
            }
            for ((lang, entries) in dto.translations) {
                val langObj = JSONObject()
                for ((key, entry) in entries) {
                    when (entry) {
                        is TranslationEntryDto.Str -> langObj.put(key, entry.value)
                        is TranslationEntryDto.Plurals -> {
                            val pObj = JSONObject()
                            entry.map.forEach { (k, v) -> pObj.put(k, v) }
                            langObj.put(key, pObj)
                        }
                    }
                }
                root.put(lang, langObj)
            }
            return root.toString()
        }
    }
}
