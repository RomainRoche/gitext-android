package io.gitrad.sdk.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.gitrad.sdk.data.datasources.LocalTranslationDataSource
import io.gitrad.sdk.data.dtos.TranslationEntryDto
import io.gitrad.sdk.data.dtos.TranslationPayloadDto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalTranslationDataSourceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val source = LocalTranslationDataSource(context, "test-local-ds")

    @Before fun setUp() = source.clear()
    @After fun tearDown() = source.clear()

    @Test fun read_returns_null_when_no_cache() {
        assertNull(source.read())
    }

    @Test fun write_then_read_round_trips() {
        val dto = sampleDto()
        source.write(dto)
        val result = source.read()
        assertNotNull(result)
        assertEquals(dto.namespaces, result!!.namespaces)
        assertEquals(1, result.translations["en"]?.size)
    }

    @Test fun clear_removes_cache_file() {
        source.write(sampleDto())
        source.clear()
        assertNull(source.read())
    }

    @Test fun modification_date_is_null_before_first_write() {
        assertNull(source.modificationDate())
    }

    @Test fun modification_date_is_set_after_write() {
        val before = System.currentTimeMillis()
        source.write(sampleDto())
        val date = source.modificationDate()
        assertNotNull(date)
        assert(date!! >= before)
    }

    @Test fun corrupt_cache_returns_null_and_deletes_file() {
        val cacheFile = File(context.cacheDir, "gitrad/test-local-ds/translations.json")
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText("{ not valid json {{")
        assertNull(source.read())
        assert(!cacheFile.exists())
    }

    @Test fun plurals_survive_write_read_round_trip() {
        val dto = TranslationPayloadDto(
            namespaces = emptyList(),
            translations = mapOf(
                "en" to mapOf(
                    "items.count" to TranslationEntryDto.Plurals(
                        mapOf("one" to "%d item", "other" to "%d items")
                    )
                )
            )
        )
        source.write(dto)
        val result = source.read()
        val entry = result?.translations?.get("en")?.get("items.count")
        assert(entry is TranslationEntryDto.Plurals)
        assertEquals("%d items", (entry as TranslationEntryDto.Plurals).map["other"])
    }

    private fun sampleDto() = TranslationPayloadDto(
        namespaces = listOf("app"),
        translations = mapOf(
            "en" to mapOf("greeting.hello" to TranslationEntryDto.Str("Hello"))
        )
    )
}
