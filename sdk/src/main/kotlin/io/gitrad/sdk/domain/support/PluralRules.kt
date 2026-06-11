package io.gitrad.sdk.domain.support

internal object PluralRules {

    fun category(count: Int, language: String): String {
        val base = language.substringBefore("-").lowercase()
        return when (base) {
            "ja", "zh", "ko", "th", "vi", "id", "ms", "lo", "my", "km",
            "bo", "dz", "ii", "jbo", "kde", "kea", "sah", "ses", "sg", "wo", "yo" -> "other"

            "fr", "ff", "kab", "mg", "mfe", "hy" -> if (count <= 1) "one" else "other"

            "ru", "uk", "be" -> slavicRuCategory(count)

            "cs", "sk" -> when {
                count == 1 -> "one"
                count in 2..4 -> "few"
                else -> "other"
            }

            "pl" -> polishCategory(count)

            "ar" -> arabicCategory(count)

            "lv" -> when {
                count == 0 -> "zero"
                count % 10 == 1 && count % 100 != 11 -> "one"
                else -> "other"
            }

            "ga" -> when (count) {
                1 -> "one"
                2 -> "two"
                in 3..6 -> "few"
                in 7..10 -> "many"
                else -> "other"
            }

            "ro" -> {
                val mod100 = count % 100
                when {
                    count == 1 -> "one"
                    count == 0 || mod100 in 1..19 -> "few"
                    else -> "other"
                }
            }

            "lt" -> lithuanianCategory(count)

            "sl" -> {
                val mod100 = count % 100
                when (mod100) {
                    1 -> "one"
                    2 -> "two"
                    3, 4 -> "few"
                    else -> "other"
                }
            }

            "he", "iw" -> when {
                count == 1 -> "one"
                count == 2 -> "two"
                count >= 11 && count % 10 == 0 -> "many"
                else -> "other"
            }

            "mk" -> if (count % 10 == 1 && count != 11) "one" else "other"

            else -> if (count == 1) "one" else "other"
        }
    }

    fun form(count: Int, map: Map<String, String>, language: String): String {
        if (count == 0) {
            val zero = map["zero"]
            if (zero != null) return zero.replace("%d", count.toString())
        }
        val cat = category(count, language)
        val raw = map[cat] ?: map["other"] ?: return count.toString()
        return raw.replace("%d", count.toString())
    }

    private fun slavicRuCategory(count: Int): String {
        val mod10 = Math.abs(count) % 10
        val mod100 = Math.abs(count) % 100
        return when {
            mod10 == 1 && mod100 != 11 -> "one"
            mod10 in 2..4 && mod100 !in 12..14 -> "few"
            else -> "many"
        }
    }

    private fun polishCategory(count: Int): String {
        if (count == 1) return "one"
        val mod10 = count % 10
        val mod100 = count % 100
        return when {
            mod10 in 2..4 && mod100 !in 12..14 -> "few"
            mod10 == 0 || mod10 == 1 || mod10 >= 5 || mod100 in 12..14 -> "many"
            else -> "other"
        }
    }

    private fun arabicCategory(count: Int): String {
        val mod100 = count % 100
        return when {
            count == 0 -> "zero"
            count == 1 -> "one"
            count == 2 -> "two"
            mod100 in 3..10 -> "few"
            mod100 in 11..99 -> "many"
            else -> "other"
        }
    }

    private fun lithuanianCategory(count: Int): String {
        val mod10 = count % 10
        val mod100 = count % 100
        val teenRange = 11..19
        return when {
            mod10 == 1 && mod100 !in teenRange -> "one"
            mod10 in 2..9 && mod100 !in teenRange -> "few"
            else -> "other"
        }
    }
}
