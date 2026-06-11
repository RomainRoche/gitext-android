package io.gitrad.sdk

import io.gitrad.sdk.domain.support.PluralRules
import org.junit.Assert.assertEquals
import org.junit.Test

class PluralRulesTest {

    @Test fun english_one_other() {
        assertEquals("one",   PluralRules.category(1,   "en"))
        assertEquals("other", PluralRules.category(0,   "en"))
        assertEquals("other", PluralRules.category(2,   "en"))
        assertEquals("other", PluralRules.category(100, "en"))
    }

    @Test fun french_zero_treated_as_one() {
        assertEquals("one",   PluralRules.category(0, "fr"))
        assertEquals("one",   PluralRules.category(1, "fr"))
        assertEquals("other", PluralRules.category(2, "fr"))
    }

    @Test fun russian_slavic() {
        assertEquals("one",  PluralRules.category(1,  "ru"))
        assertEquals("few",  PluralRules.category(2,  "ru"))
        assertEquals("many", PluralRules.category(5,  "ru"))
        assertEquals("many", PluralRules.category(11, "ru"))
        assertEquals("one",  PluralRules.category(21, "ru"))
        assertEquals("few",  PluralRules.category(22, "ru"))
    }

    @Test fun arabic_six_forms() {
        assertEquals("zero",  PluralRules.category(0,   "ar"))
        assertEquals("one",   PluralRules.category(1,   "ar"))
        assertEquals("two",   PluralRules.category(2,   "ar"))
        assertEquals("few",   PluralRules.category(5,   "ar"))
        assertEquals("many",  PluralRules.category(15,  "ar"))
        assertEquals("other", PluralRules.category(100, "ar"))
    }

    @Test fun japanese_invariant() {
        assertEquals("other", PluralRules.category(1, "ja"))
        assertEquals("other", PluralRules.category(2, "ja"))
    }

    @Test fun regional_variant_strips_to_base() {
        assertEquals("one",   PluralRules.category(0, "fr-FR"))
        assertEquals("other", PluralRules.category(2, "fr-FR"))
    }

    @Test fun polish_four_forms() {
        assertEquals("one",  PluralRules.category(1,  "pl"))
        assertEquals("few",  PluralRules.category(2,  "pl"))
        assertEquals("many", PluralRules.category(5,  "pl"))
        assertEquals("many", PluralRules.category(11, "pl"))
        assertEquals("few",  PluralRules.category(22, "pl"))
    }

    @Test fun czech_one_few_other() {
        assertEquals("one",   PluralRules.category(1, "cs"))
        assertEquals("few",   PluralRules.category(3, "cs"))
        assertEquals("other", PluralRules.category(5, "cs"))
    }

    @Test fun hebrew_one_two_many_other() {
        assertEquals("one",   PluralRules.category(1,  "he"))
        assertEquals("two",   PluralRules.category(2,  "he"))
        assertEquals("many",  PluralRules.category(20, "he"))
        assertEquals("other", PluralRules.category(3,  "he"))
    }

    @Test fun form_substitutes_count() {
        val map = mapOf("one" to "%d item", "other" to "%d items")
        assertEquals("1 item",  PluralRules.form(1, map, "en"))
        assertEquals("5 items", PluralRules.form(5, map, "en"))
    }

    @Test fun form_zero_key_wins_over_cldr() {
        val map = mapOf("zero" to "No notifications", "one" to "%d notification", "other" to "%d notifications")
        assertEquals("No notifications", PluralRules.form(0, map, "en"))
        assertEquals("1 notification",   PluralRules.form(1, map, "en"))
        assertEquals("5 notifications",  PluralRules.form(5, map, "en"))
    }

    @Test fun form_falls_back_to_other_when_category_missing() {
        val map = mapOf("other" to "%d items")
        assertEquals("1 items", PluralRules.form(1, map, "en"))
    }
}
