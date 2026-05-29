package com.gyeonggisumgil.app.data.ai

object AiRouteQueryParser {
    fun extractPlaceQuery(question: String): String? {
        val withoutDistance = question
            .replace(DISTANCE_PATTERN, " ")
            .replace(DURATION_PATTERN, " ")

        val cleaned = REQUEST_WORDS.fold(withoutDistance) { text, word ->
            text.replace(word, " ")
        }
            .replace(PUNCTUATION_PATTERN, " ")
            .replace(WHITESPACE_PATTERN, " ")
            .trim()
            .stripRouteParticles()

        if (cleaned.isBlank()) return null

        val compact = cleaned.replace(" ", "")
        if (compact.length < 2) return null
        if (!EXPLICIT_PLACE_PATTERN.containsMatchIn(compact)) return null

        return cleaned
    }

    fun extractRequestedDistanceMeters(question: String): Int? {
        val kmMatch = Regex("""(\d+(?:\.\d+)?)\s*(?:km|킬로|키로|킬로미터)""", RegexOption.IGNORE_CASE)
            .find(question)
        if (kmMatch != null) {
            return kmMatch.groupValues.getOrNull(1)
                ?.toDoubleOrNull()
                ?.let { (it * 1_000).toInt() }
        }

        val meterMatch = Regex("""(\d+(?:\.\d+)?)\s*(?:m|미터)""", RegexOption.IGNORE_CASE)
            .find(question)
        return meterMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toInt()
    }

    fun extractRequestedDurationMinutes(question: String): Int? {
        val hourMatch = Regex("""(\d+(?:\.\d+)?)\s*시간""")
            .find(question)
        if (hourMatch != null) {
            return hourMatch.groupValues.getOrNull(1)
                ?.toDoubleOrNull()
                ?.let { (it * 60).toInt() }
        }

        val minuteMatch = Regex("""(\d+(?:\.\d+)?)\s*분""")
            .find(question)
        return minuteMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toInt()
    }

    private fun String.stripRouteParticles(): String {
        var result = trim()
        do {
            val before = result
            result = TRAILING_PARTICLE_PATTERN.replace(result, "").trim()
        } while (result != before)
        return result
    }

    private val DISTANCE_PATTERN = Regex(
        """\d+(?:\.\d+)?\s*(?:km|킬로미터|킬로|키로|m|미터)""",
        RegexOption.IGNORE_CASE
    )
    private val DURATION_PATTERN = Regex("""\d+\s*(?:분|시간)""")
    private val PUNCTUATION_PATTERN = Regex("""[,.;:!?~]+""")
    private val WHITESPACE_PATTERN = Regex("""\s+""")
    private val TRAILING_PARTICLE_PATTERN = Regex("""(?:에서|부터|주변|근처|일대|기준|을|를)$""")
    private val EXPLICIT_PLACE_PATTERN = Regex(
        """[가-힣A-Za-z0-9]+(?:대로|로|길|역|공원|호수|천|강|산|동|읍|면|리|시|구|군|IC|나들목)""",
        RegexOption.IGNORE_CASE
    )
    private val REQUEST_WORDS = listOf(
        "달리고 싶어",
        "달리고싶어",
        "뛰고 싶어",
        "뛰고싶어",
        "조깅하고 싶어",
        "조깅하고싶어",
        "걷고 싶어",
        "걷고싶어",
        "산책하고 싶어",
        "산책하고싶어",
        "달리고",
        "뛰고",
        "조깅하고",
        "걷고",
        "걸으면서",
        "달리면서",
        "한바퀴",
        "한 바퀴",
        "일주",
        "둘레",
        "도는",
        "돌아오는",
        "왕복",
        "산책길",
        "산책",
        "후보",
        "러닝",
        "달리기",
        "조깅",
        "코스",
        "경로",
        "추천해줘",
        "추천해라",
        "추천해",
        "추천",
        "보여줘",
        "보여라",
        "보여",
        "만들어줘",
        "만들어라",
        "만들어",
        "짜줘",
        "짜라",
        "짜",
        "찾아줘",
        "찾아라",
        "찾아",
        "싶어",
        "해줘라",
        "해줘",
        "줘라",
        "줘",
        "해봐",
        "부탁해",
        "좀",
        "한번"
    ).sortedByDescending { it.length }
}
