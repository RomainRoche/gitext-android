package io.gitext.sdk.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.gitext.sdk.data.datasources.BundleTranslationDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundleTranslationDataSourceTest {

    @Test fun loads_bundled_translations_from_assets() {
        // Instrumentation context assets include the test fixture at
        // androidTest/assets/gitext-baseline/translations.json.
        val context = InstrumentationRegistry.getInstrumentation().context
        val dto = BundleTranslationDataSource(context).load()
        assertNotNull(dto)
        assertTrue(dto!!.translations.containsKey("en"))
        assertTrue(dto.translations.containsKey("fr"))
    }

    @Test fun returns_expected_namespaces() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val dto = BundleTranslationDataSource(context).load()
        assertEquals(listOf("app"), dto?.namespaces)
    }

    @Test fun parses_plural_entries() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val dto = BundleTranslationDataSource(context).load()
        val entry = dto?.translations?.get("en")?.get("items.count")
        assertNotNull(entry)
    }

    @Test fun returns_null_when_asset_is_missing() {
        // The target context (generated stub app APK) has no gitext-baseline assets.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dto = BundleTranslationDataSource(context).load()
        assertNull(dto)
    }
}
